package ar.edu.uade.robothmi.data.sim

import ar.edu.uade.robothmi.data.RobotRepository
import ar.edu.uade.robothmi.data.SimulationControls
import ar.edu.uade.robothmi.data.local.EventLogDao
import ar.edu.uade.robothmi.data.local.EventLogEntity
import ar.edu.uade.robothmi.data.local.toDomain
import ar.edu.uade.robothmi.domain.CommandRules
import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.AlertType
import ar.edu.uade.robothmi.domain.model.CommandRecord
import ar.edu.uade.robothmi.domain.model.CommandStatus
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.Connectivity
import ar.edu.uade.robothmi.domain.model.EventCategory
import ar.edu.uade.robothmi.domain.model.HistoryEntry
import ar.edu.uade.robothmi.domain.model.MissionProgress
import ar.edu.uade.robothmi.domain.model.MissionStatus
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.Position
import ar.edu.uade.robothmi.domain.model.RobotAlert
import ar.edu.uade.robothmi.domain.model.RobotInfo
import ar.edu.uade.robothmi.domain.model.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Robot simulado. Corre un "tick" por segundo que actualiza batería, posición y misión,
 * y publica telemetría solo si hay conexión. Los comandos tienen latencia, pueden ser
 * rechazados y vencen si no hay respuesta.
 *
 * Todo el estado interno se modifica con [mutex] tomado.
 */
class SimulatedRobotRepository(
    private val dao: EventLogDao,
    private val profile: ServiceProfile,
) : RobotRepository, SimulationControls {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val random: Random = Random.Default

    // ---------- Flujos expuestos ----------
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    override val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private val _alerts = MutableStateFlow<List<RobotAlert>>(emptyList())
    override val alerts: StateFlow<List<RobotAlert>> = _alerts.asStateFlow()

    private val _commands = MutableStateFlow<List<CommandRecord>>(emptyList())
    override val commands: StateFlow<List<CommandRecord>> = _commands.asStateFlow()

    override val history: Flow<List<HistoryEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override val robots: List<RobotInfo> = listOf(
        RobotInfo(
            id = "robot-01",
            name = "${profile.platform.label} · Unidad 01",
            platform = profile.platform,
            description = profile.robotDescription,
            simulated = true,
        ),
    )
    override val serviceName: String get() = profile.serviceName
    override val safePointName: String get() = profile.base.name
    override val missions: List<MissionTemplate> get() = profile.missions
    override val customScenarioLabel: String get() = profile.customScenarioLabel

    // ---------- Estado interno del robot ----------
    private class MissionRun(val template: MissionTemplate, val startX: Float, val startY: Float) {
        var index = 0
        var legTicks = 0
        var status = MissionStatus.EN_CURSO
        var stage = "Saliendo"
    }

    private var robotId: String? = null
    private var tickJob: Job? = null

    private var battery = 86f
    private var opState = OperationalState.DISPONIBLE
    private var run: MissionRun? = null
    private var x = profile.base.x
    private var y = profile.base.y
    private var zone = profile.base.zone
    private var speed = 0f
    private var signal = 90

    private var returnFromX = 0f
    private var returnFromY = 0f
    private var returnTicks = 0
    private var returnTicksTotal = 1

    /** El robot se frenó por un obstáculo: al reanudar, replanifica la ruta. */
    private var replanOnResume = false

    private var lowBatteryWarned = false
    private var criticalBatteryWarned = false

    // ---------- Fallas inyectadas ----------
    @Volatile private var disconnectedUntil = 0L
    @Volatile private var rejectNext = false
    private var wasDisconnected = false
    private var disconnectedSince = 0L
    private val queuedAlerts = mutableListOf<RobotAlert>()

    // =====================================================================
    // Conexión
    // =====================================================================

    override fun connect(robotId: String) {
        if (tickJob?.isActive == true) return
        this.robotId = robotId
        log(EventCategory.CONEXION, "Sesión iniciada con $robotId (simulado)")
        tickJob = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    override fun disconnect() {
        tickJob?.cancel()
        tickJob = null
        _telemetry.value = null
        robotId?.let { log(EventCategory.CONEXION, "Sesión cerrada con $it") }
        robotId = null
    }

    private fun isLinkDown() = System.currentTimeMillis() < disconnectedUntil

    // =====================================================================
    // Ciclo de simulación
    // =====================================================================

    private suspend fun tick() {
        mutex.withLock { tickLocked() }
    }

    private fun tickLocked() {
        val now = System.currentTimeMillis()

        updateBattery()
        when (opState) {
            OperationalState.EN_MISION -> advanceMission()
            OperationalState.VOLVIENDO_A_BASE -> advanceReturn()
            else -> speed = 0f
        }
        profile.onTick(simContext(), random)?.let { raiseServiceEvent(it) }

        val linkDown = isLinkDown()
        if (linkDown && !wasDisconnected) {
            wasDisconnected = true
            disconnectedSince = now
            log(EventCategory.CONEXION, "Se dejó de recibir telemetría")
        }
        if (!linkDown && wasDisconnected) {
            wasDisconnected = false
            val secs = (now - disconnectedSince) / 1000
            signal = 40
            log(EventCategory.CONEXION, "Conexión recuperada", "Sin datos durante $secs s")
            _alerts.update { (queuedAlerts.reversed() + it).take(MAX_ALERTS) }
            queuedAlerts.clear()
            addAlert(
                AlertType.DESCONEXION, AlertSeverity.ADVERTENCIA,
                "Se perdió la conexión durante $secs s. Verificá que el estado mostrado coincida con el robot.",
            )
        }

        if (!linkDown) {
            signal = (signal + random.nextInt(-3, 6)).coerceIn(30, 98)
            _telemetry.value = snapshot(now)
        }
    }

    private fun snapshot(now: Long) = Telemetry(
        robotId = robotId.orEmpty(),
        connectivity = if (signal < 45) Connectivity.INESTABLE else Connectivity.CONECTADO,
        signalPct = signal,
        batteryPct = battery.roundToInt(),
        charging = opState == OperationalState.CARGANDO,
        state = opState,
        mission = run?.let {
            MissionProgress(
                missionId = it.template.id,
                name = it.template.name,
                status = it.status,
                currentWaypoint = it.index.coerceAtMost(it.template.waypoints.size),
                totalWaypoints = it.template.waypoints.size,
                stage = it.stage,
            )
        },
        position = Position(x, y, zone),
        speedMps = speed,
        serviceMetrics = profile.metrics(simContext()),
        timestampMs = now,
    )

    private fun missionActive(): Boolean {
        val r = run ?: return false
        return r.status == MissionStatus.EN_CURSO || r.status == MissionStatus.PAUSADA
    }

    private fun simContext() = SimContext(
        state = opState,
        missionActive = missionActive(),
        moving = speed > 0.05f,
        remainingMeters = remainingMeters(),
    )

    /** Metros que faltan recorrer hasta el último punto de la misión activa. */
    private fun remainingMeters(): Float? {
        val r = run ?: return null
        if (!missionActive()) return null
        val wps = r.template.waypoints
        if (r.index >= wps.size) return 0f
        var total = hypot(wps[r.index].x - x, wps[r.index].y - y)
        for (i in r.index until wps.size - 1) {
            total += hypot(wps[i + 1].x - wps[i].x, wps[i + 1].y - wps[i].y)
        }
        return total
    }

    private fun updateBattery() {
        when (opState) {
            OperationalState.CARGANDO -> {
                battery = (battery + 1.5f).coerceAtMost(100f)
                if (battery >= 100f) {
                    opState = OperationalState.DISPONIBLE
                    log(EventCategory.SISTEMA, "Carga completa")
                }
            }
            OperationalState.EN_MISION, OperationalState.VOLVIENDO_A_BASE -> battery -= 0.3f
            else -> battery -= 0.04f
        }
        battery = battery.coerceIn(0f, 100f)

        val pct = battery.roundToInt()
        if (pct >= 30) {
            lowBatteryWarned = false
            criticalBatteryWarned = false
        }
        if (opState == OperationalState.CARGANDO) return

        if (pct < CommandRules.LOW_BATTERY && !lowBatteryWarned) {
            lowBatteryWarned = true
            addAlert(
                AlertType.BATERIA_BAJA, AlertSeverity.ADVERTENCIA,
                "Batería baja ($pct %). Conviene ordenar el regreso a punto seguro.",
            )
        }
        if (pct < CommandRules.CRITICAL_BATTERY && !criticalBatteryWarned) {
            criticalBatteryWarned = true
            // Si el operador lo detuvo o espera una decisión, el robot NO se mueve solo.
            val canMoveAlone = opState == OperationalState.EN_MISION ||
                opState == OperationalState.PAUSADO ||
                opState == OperationalState.DISPONIBLE
            if (!profile.autoReturnOnCriticalBattery && opState == OperationalState.EN_MISION) {
                // Servicio que acompaña a una persona: el robot no se va solo y la deja.
                opState = OperationalState.REQUIERE_AYUDA
                speed = 0f
                run?.let {
                    it.status = MissionStatus.PAUSADA
                    it.stage = "Detenido por batería crítica, junto a la persona"
                }
                addAlert(
                    AlertType.BATERIA_BAJA, AlertSeverity.CRITICA,
                    "Batería crítica ($pct %). El robot se detuvo en un lugar seguro sin dejar sola a la persona. Enviá a alguien a acompañarla.",
                )
            } else if (canMoveAlone && profile.autoReturnOnCriticalBattery) {
                addAlert(
                    AlertType.BATERIA_BAJA, AlertSeverity.CRITICA,
                    "Batería crítica ($pct %). El robot inicia el regreso a punto seguro por su cuenta.",
                )
                startReturn("Regreso automático por batería crítica")
            } else {
                addAlert(
                    AlertType.BATERIA_BAJA, AlertSeverity.CRITICA,
                    "Batería crítica ($pct %). El robot está quieto: ordená el regreso a punto seguro.",
                )
            }
        }
    }

    private fun advanceMission() {
        val r = run ?: return
        val wps = r.template.waypoints
        if (r.index >= wps.size) {
            r.status = MissionStatus.COMPLETADA
            r.stage = "Recorrido finalizado"
            opState = OperationalState.DISPONIBLE
            speed = 0f
            log(EventCategory.MISION, "Misión completada", r.template.name)
            return
        }
        val target = wps[r.index]
        val fromX = if (r.index == 0) r.startX else wps[r.index - 1].x
        val fromY = if (r.index == 0) r.startY else wps[r.index - 1].y

        r.legTicks++
        val t = (r.legTicks.toFloat() / LEG_TICKS).coerceAtMost(1f)
        x = lerp(fromX, target.x, t)
        y = lerp(fromY, target.y, t)
        zone = target.zone
        speed = (0.9f + random.nextFloat() * 0.3f) * profile.speedFactor()
        r.stage = "Yendo a ${target.name}"

        if (r.legTicks >= LEG_TICKS) {
            r.legTicks = 0
            r.index++
            r.stage = if (r.index >= wps.size) "Llegando a ${target.name}" else "En ${target.name}"
            log(EventCategory.MISION, "Punto alcanzado: ${target.name}", r.template.name)
            profile.onWaypointReached(target, random)?.let { raiseServiceEvent(it) }
        }
    }

    private fun startReturn(reason: String) {
        run?.let {
            if (it.status == MissionStatus.EN_CURSO || it.status == MissionStatus.PAUSADA) {
                it.status = MissionStatus.CANCELADA
                it.stage = "Interrumpida: regreso a punto seguro"
                log(EventCategory.MISION, "Misión interrumpida", it.template.name)
            }
        }
        returnFromX = x
        returnFromY = y
        val dist = hypot(x - profile.base.x, y - profile.base.y)
        returnTicksTotal = (dist / 5f).roundToInt().coerceIn(3, 10)
        returnTicks = 0
        opState = OperationalState.VOLVIENDO_A_BASE
        log(EventCategory.MISION, "Regreso a punto seguro", reason)
    }

    private fun advanceReturn() {
        returnTicks++
        val t = (returnTicks.toFloat() / returnTicksTotal).coerceAtMost(1f)
        x = lerp(returnFromX, profile.base.x, t)
        y = lerp(returnFromY, profile.base.y, t)
        if (t >= 1f) {
            zone = profile.base.zone
            speed = 0f
            opState = OperationalState.CARGANDO
            log(EventCategory.MISION, "Llegó a punto seguro", "${profile.base.name}: inicia carga")
        } else {
            zone = "En tránsito a ${profile.base.name}"
            speed = 0.8f + random.nextFloat() * 0.2f
        }
    }

    private fun raiseServiceEvent(event: ServiceEvent) {
        addAlert(AlertType.SERVICIO, event.severity, event.message)
        if (event.requiresOperator && opState == OperationalState.EN_MISION) {
            opState = OperationalState.REQUIERE_AYUDA
            speed = 0f
            run?.let {
                it.status = MissionStatus.PAUSADA
                it.stage = "Esperando decisión del operador"
            }
        }
    }

    private fun addAlert(type: AlertType, severity: AlertSeverity, message: String) {
        val alert = RobotAlert(
            id = UUID.randomUUID().toString(),
            type = type,
            severity = severity,
            message = message,
            createdAtMs = System.currentTimeMillis(),
        )
        log(EventCategory.ALERTA, "${severity.label}: $message")
        // Sin enlace, la alerta existe en el robot pero la app todavía no la recibió.
        if (isLinkDown()) {
            queuedAlerts += alert
        } else {
            _alerts.update { (listOf(alert) + it).take(MAX_ALERTS) }
        }
    }

    // =====================================================================
    // Comandos
    // =====================================================================

    private sealed interface CommandOutcome {
        data class Accepted(val message: String) : CommandOutcome
        data class Rejected(val reason: String) : CommandOutcome
    }

    override suspend fun sendCommand(type: CommandType, target: String?): CommandRecord {
        val sent = CommandRecord(
            id = UUID.randomUUID().toString().take(8),
            type = type,
            target = target,
            sentAtMs = System.currentTimeMillis(),
            status = CommandStatus.PENDIENTE,
        )
        upsertCommand(sent)
        log(EventCategory.COMANDO, "Orden enviada: ${type.label}", target)

        val outcome = withTimeoutOrNull(COMMAND_TIMEOUT_MS) {
            delay(random.nextLong(400, 1200))          // latencia de ida
            while (isLinkDown()) delay(200)              // sin enlace la orden no llega
            mutex.withLock {
                execute(type, target).also {
                    if (it is CommandOutcome.Accepted) profile.onCommandAccepted(type)
                }
            }
        }

        val now = System.currentTimeMillis()
        val done = when (outcome) {
            null -> sent.copy(
                status = CommandStatus.SIN_RESPUESTA,
                detail = "Sin respuesta en ${COMMAND_TIMEOUT_MS / 1000} s. No asumas que se ejecutó: verificá el estado antes de reintentar.",
                respondedAtMs = now,
            )
            is CommandOutcome.Accepted -> sent.copy(status = CommandStatus.CONFIRMADA, detail = outcome.message, respondedAtMs = now)
            is CommandOutcome.Rejected -> sent.copy(status = CommandStatus.RECHAZADA, detail = outcome.reason, respondedAtMs = now)
        }
        upsertCommand(done)
        log(EventCategory.COMANDO, "${type.label}: ${done.status.label}", done.detail)
        return done
    }

    private fun upsertCommand(record: CommandRecord) {
        _commands.update { list ->
            val without = list.filterNot { it.id == record.id }
            (listOf(record) + without).sortedByDescending { it.sentAtMs }.take(MAX_COMMANDS)
        }
    }

    /** Validación y ejecución del lado del robot. Se llama con el mutex tomado. */
    private fun execute(type: CommandType, target: String?): CommandOutcome {
        if (rejectNext) {
            rejectNext = false
            return CommandOutcome.Rejected("El robot rechazó la orden (rechazo forzado desde Simulación).")
        }
        val r = run
        return when (type) {
            CommandType.INICIAR_MISION -> {
                val template = profile.missions.firstOrNull { it.id == target }
                    ?: return CommandOutcome.Rejected("Misión desconocida.")
                when {
                    opState != OperationalState.DISPONIBLE && opState != OperationalState.CARGANDO ->
                        CommandOutcome.Rejected("El robot no está disponible (${opState.label}).")
                    battery < CommandRules.MIN_BATTERY_TO_START ->
                        CommandOutcome.Rejected("Batería insuficiente para la misión (${battery.roundToInt()} %).")
                    else -> {
                        run = MissionRun(template, x, y)
                        profile.resetForMission(template)
                        opState = OperationalState.EN_MISION
                        log(EventCategory.MISION, "Misión iniciada", template.name)
                        CommandOutcome.Accepted("Misión \"${template.name}\" iniciada.")
                    }
                }
            }

            CommandType.PAUSAR ->
                if (opState == OperationalState.EN_MISION && r != null) {
                    opState = OperationalState.PAUSADO
                    speed = 0f
                    r.status = MissionStatus.PAUSADA
                    r.stage = "Pausada por el operador"
                    CommandOutcome.Accepted("Robot en pausa. Puede reanudarse.")
                } else {
                    CommandOutcome.Rejected("No hay misión en curso para pausar.")
                }

            CommandType.REANUDAR -> when {
                r == null || r.status != MissionStatus.PAUSADA ->
                    CommandOutcome.Rejected("No hay misión pausada.")
                battery < CommandRules.CRITICAL_BATTERY ->
                    CommandOutcome.Rejected("Batería crítica: no se puede seguir la misión.")
                opState == OperationalState.REQUIERE_AYUDA && hasUnackedCritical() ->
                    CommandOutcome.Rejected("Hay alertas críticas sin reconocer.")
                opState != OperationalState.PAUSADO &&
                    opState != OperationalState.DETENIDO &&
                    opState != OperationalState.REQUIERE_AYUDA ->
                    CommandOutcome.Rejected("No se puede reanudar desde ${opState.label}.")
                else -> {
                    val note = if (replanOnResume) "Ruta replanificada. " else ""
                    replanOnResume = false
                    opState = OperationalState.EN_MISION
                    r.status = MissionStatus.EN_CURSO
                    log(EventCategory.MISION, "Misión reanudada", r.template.name)
                    CommandOutcome.Accepted("${note}Misión reanudada.")
                }
            }

            CommandType.CANCELAR ->
                if (r != null && (r.status == MissionStatus.EN_CURSO || r.status == MissionStatus.PAUSADA)) {
                    r.status = MissionStatus.CANCELADA
                    r.stage = "Cancelada por el operador"
                    opState = OperationalState.DISPONIBLE
                    speed = 0f
                    log(EventCategory.MISION, "Misión cancelada", r.template.name)
                    CommandOutcome.Accepted("Misión cancelada. El robot queda quieto donde está.")
                } else {
                    CommandOutcome.Rejected("No hay misión activa para cancelar.")
                }

            CommandType.VOLVER_A_BASE ->
                if (opState == OperationalState.VOLVIENDO_A_BASE || opState == OperationalState.CARGANDO) {
                    CommandOutcome.Rejected("El robot ya está en punto seguro o yendo hacia él.")
                } else {
                    startReturn("Ordenado por el operador")
                    CommandOutcome.Accepted("Regresando a ${profile.base.name}.")
                }

            CommandType.DETENCION_OPERATIVA -> when (opState) {
                OperationalState.EN_MISION,
                OperationalState.VOLVIENDO_A_BASE,
                OperationalState.PAUSADO,
                OperationalState.REQUIERE_AYUDA -> {
                    if (r != null && r.status == MissionStatus.EN_CURSO) {
                        r.status = MissionStatus.PAUSADA
                        r.stage = "Detenida por el operador"
                    }
                    opState = OperationalState.DETENIDO
                    speed = 0f
                    CommandOutcome.Accepted("Movimiento detenido por software. No reemplaza la parada de emergencia física.")
                }
                else -> CommandOutcome.Rejected("El robot ya está quieto (${opState.label}).")
            }

            CommandType.RECONOCER_ALERTA ->
                if (_alerts.value.none { it.id == target }) {
                    CommandOutcome.Rejected("La alerta no existe.")
                } else {
                    _alerts.update { list -> list.map { if (it.id == target) it.copy(acknowledged = true) else it } }
                    CommandOutcome.Accepted("Alerta reconocida.")
                }
        }
    }

    private fun hasUnackedCritical() =
        _alerts.value.any { !it.acknowledged && it.severity == AlertSeverity.CRITICA }

    override suspend fun clearHistory() = dao.clear()

    // =====================================================================
    // Controles de simulación (escenarios de la demo)
    // =====================================================================

    override suspend fun forceDisconnection(seconds: Int): String {
        disconnectedUntil = System.currentTimeMillis() + seconds * 1000L
        return "Conexión cortada durante $seconds s"
    }

    override suspend fun rejectNextCommand(): String {
        rejectNext = true
        return "La próxima orden será rechazada"
    }

    override suspend fun setBattery(percent: Int): String = mutex.withLock {
        battery = percent.toFloat()
        "Batería forzada a $percent %"
    }

    override suspend fun triggerObstacle(): String = mutex.withLock {
        val r = run
        if (opState != OperationalState.EN_MISION || r == null) {
            return@withLock "El obstáculo solo aplica con una misión en curso"
        }
        val next = r.template.waypoints.getOrNull(r.index)?.name ?: "el próximo punto"
        opState = OperationalState.REQUIERE_AYUDA
        speed = 0f
        replanOnResume = true
        r.status = MissionStatus.PAUSADA
        r.stage = "Paso bloqueado camino a $next"
        addAlert(
            AlertType.OBSTACULO, AlertSeverity.CRITICA,
            "Paso bloqueado camino a $next. El robot está quieto y espera tu decisión: reanudar (replanifica), volver a punto seguro o cancelar.",
        )
        "Obstáculo generado"
    }

    override suspend fun triggerCustomScenario(): String = mutex.withLock {
        raiseServiceEvent(profile.customScenario())
        "Escenario propio: ${profile.customScenarioLabel}"
    }

    // =====================================================================

    private fun log(category: EventCategory, title: String, detail: String? = null) {
        val entry = EventLogEntity(
            timestampMs = System.currentTimeMillis(),
            category = category.name,
            title = title,
            detail = detail,
        )
        scope.launch(Dispatchers.IO) { dao.insert(entry) }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    companion object {
        const val TICK_MS = 1_000L
        const val LEG_TICKS = 4
        const val COMMAND_TIMEOUT_MS = 6_000L
        private const val MAX_ALERTS = 50
        private const val MAX_COMMANDS = 30
    }
}
