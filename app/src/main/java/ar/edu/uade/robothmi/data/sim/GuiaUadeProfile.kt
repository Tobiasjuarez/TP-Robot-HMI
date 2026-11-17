package ar.edu.uade.robothmi.data.sim

import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.MetricLevel
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.RobotPlatform
import ar.edu.uade.robothmi.domain.model.ServiceMetric
import ar.edu.uade.robothmi.domain.model.Waypoint
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Caso de uso: Go2 que guía a personas ciegas o con baja visión dentro de una sede UADE.
 * La persona va tomada de un asa; el robot le avisa por voz giros, ascensores y llegadas.
 * Un operador supervisa los viajes desde el Punto de encuentro.
 *
 * El plano es ILUSTRATIVO (coordenadas en metros, pisos en la zona).
 * Reemplazar por el plano real de la sede cuando lo tengamos.
 */
class GuiaUadeProfile : ServiceProfile {
    override val serviceName = "Guía para personas con baja visión · UADE"
    override val platform = RobotPlatform.GO2
    override val robotDescription = "Guía con asa y voz (simulado)"
    override val base = Waypoint(
        name = "Punto de encuentro",
        zone = "Planta baja · Hall de entrada",
        x = 2f, y = 2f,
    )
    override val customScenarioLabel = "La persona soltó el asa"

    /** Nunca deja sola a la persona: con batería crítica se detiene y pide ayuda. */
    override val autoReturnOnCriticalBattery = false

    // ---------- Puntos del plano (ilustrativo) ----------
    private val pasilloPb = Waypoint(
        "Pasillo central", "Planta baja · Pasillo central", 10f, 2f,
        announcement = "Seguimos derecho por el pasillo central.",
    )
    private val ascensorPb = Waypoint(
        "Ascensor A (planta baja)", "Planta baja · Ascensores", 16f, 6f,
        announcement = "Llegamos al ascensor. Esperamos a que abra la puerta.",
    )
    private fun salidaAscensor(piso: Int) = Waypoint(
        "Salida del ascensor, piso $piso", "Piso $piso · Ascensores", 16f, 8f,
        announcement = "Estamos en el piso $piso. Salimos del ascensor hacia la derecha.",
    )

    override val missions = listOf(
        MissionTemplate(
            id = "aula-305",
            name = "Guiar a Aula 305",
            description = "Hall → ascensor → piso 3",
            waypoints = listOf(
                pasilloPb,
                ascensorPb,
                salidaAscensor(3),
                Waypoint("Pasillo norte", "Piso 3 · Pasillo norte", 24f, 10f, "Giramos a la izquierda. Puerta de vidrio a la derecha."),
                Waypoint("Aula 305", "Piso 3 · Aula 305", 28f, 14f, "Llegamos al Aula 305. La puerta está frente a vos."),
            ),
        ),
        MissionTemplate(
            id = "biblioteca",
            name = "Guiar a Biblioteca",
            description = "Hall → ascensor → piso 1",
            waypoints = listOf(
                pasilloPb,
                ascensorPb,
                salidaAscensor(1),
                Waypoint("Biblioteca", "Piso 1 · Biblioteca", 22f, 12f, "Llegamos a la Biblioteca. El mostrador está a tu izquierda."),
            ),
        ),
        MissionTemplate(
            id = "bedelia",
            name = "Guiar a Bedelía",
            description = "En planta baja, sin ascensor",
            waypoints = listOf(
                pasilloPb,
                Waypoint("Bedelía", "Planta baja · Bedelía", 14f, -4f, "Llegamos a Bedelía. Hay una fila a tu derecha."),
            ),
        ),
    )

    // ---------- Estado del servicio ----------
    private var handleHeld = false
    /** Hay una persona con el robot (desde que arranca el viaje hasta que llega o se cancela). */
    private var accompanying = false
    private var crowd = 0            // 0 baja · 1 media · 2 alta
    private var crowdWarned = false
    private var lastVoice = "—"

    override fun resetForMission(mission: MissionTemplate) {
        handleHeld = true
        accompanying = true
        crowd = 0
        crowdWarned = false
        lastVoice = "Hola. Tomá el asa: vamos a ${mission.waypoints.last().name}."
    }

    private fun personPresent(ctx: SimContext) = accompanying &&
        ctx.state != OperationalState.DISPONIBLE && ctx.state != OperationalState.CARGANDO

    override fun metrics(ctx: SimContext): List<ServiceMetric> = listOf(
        ServiceMetric(
            id = "asa",
            label = "Asa",
            value = when {
                !personPresent(ctx) -> "Sin persona"
                handleHeld -> "Sujetada"
                else -> "Suelta"
            },
            unit = "",
            level = if (personPresent(ctx) && !handleHeld) MetricLevel.CRITICO else MetricLevel.NORMAL,
        ),
        ServiceMetric(
            id = "distancia",
            label = "Distancia al destino",
            value = ctx.remainingMeters?.roundToInt()?.toString() ?: "—",
            unit = if (ctx.remainingMeters == null) "" else "m",
        ),
        ServiceMetric(
            id = "gente",
            label = "Gente en el pasillo",
            value = listOf("Poca", "Media", "Mucha")[crowd],
            unit = "",
            level = if (crowd == 2) MetricLevel.ADVERTENCIA else MetricLevel.NORMAL,
        ),
        // Extra (HRI): lo último que el robot le dijo a la persona, para que el operador lo sepa.
        ServiceMetric("voz", "Último aviso por voz", lastVoice, ""),
    )

    override fun onTick(ctx: SimContext, random: Random): ServiceEvent? {
        // Llegó a destino o al punto de encuentro: la persona ya no está con el robot.
        if (ctx.state == OperationalState.DISPONIBLE || ctx.state == OperationalState.CARGANDO) accompanying = false
        if (!ctx.moving) return null
        // La cantidad de gente cambia de a poco (más en los cambios de turno).
        val r = random.nextFloat()
        if (r < 0.15f) crowd = (crowd + 1).coerceAtMost(2)
        else if (r > 0.85f) crowd = (crowd - 1).coerceAtLeast(0)

        if (crowd == 2 && !crowdWarned) {
            crowdWarned = true
            lastVoice = "Hay mucha gente: vamos más despacio. (Al resto: \"Permiso, paso guiando a una persona\")"
            return ServiceEvent(
                AlertSeverity.INFO,
                "Pasillo con mucha gente: el robot baja la velocidad y pide paso por voz.",
            )
        }
        return null
    }

    override fun speedFactor(): Float = if (crowd == 2) 0.4f else 0.7f

    override fun onWaypointReached(waypoint: Waypoint, random: Random): ServiceEvent? {
        waypoint.announcement?.let { lastVoice = it }
        return null
    }

    override fun onCommandAccepted(type: CommandType) {
        lastVoice = when (type) {
            CommandType.PAUSAR -> "Nos detenemos un momento. Seguí tomando el asa."
            CommandType.REANUDAR -> {
                handleHeld = true
                "Seguimos. Tomá el asa."
            }
            CommandType.CANCELAR -> "Cancelamos el viaje. Quedate acá: alguien del personal viene a acompañarte."
            CommandType.VOLVER_A_BASE -> "Volvemos juntos al Punto de encuentro."
            CommandType.DETENCION_OPERATIVA -> "Me detuve. Quedate quieto: ya viene alguien."
            else -> lastVoice
        }
    }

    override fun customScenario(): ServiceEvent {
        handleHeld = false
        lastVoice = "Soltaste el asa. Me quedo quieto acá. Si necesitás ayuda, decí \"ayuda\"."
        return ServiceEvent(
            severity = AlertSeverity.CRITICA,
            message = "La persona soltó el asa. El robot se detuvo y le habló. " +
                "Decidí: reanudar cuando vuelva a tomarla, enviar a alguien del personal o cancelar el viaje.",
            requiresOperator = true,
        )
    }
}
