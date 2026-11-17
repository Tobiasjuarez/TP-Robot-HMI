package ar.edu.uade.robothmi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ar.edu.uade.robothmi.RobotHmiApplication
import ar.edu.uade.robothmi.data.RobotRepository
import ar.edu.uade.robothmi.data.SimulationControls
import ar.edu.uade.robothmi.domain.CommandAvailability
import ar.edu.uade.robothmi.domain.CommandRules
import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.CommandRecord
import ar.edu.uade.robothmi.domain.model.CommandStatus
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.HistoryEntry
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.RobotAlert
import ar.edu.uade.robothmi.domain.model.RobotInfo
import ar.edu.uade.robothmi.domain.model.Telemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Todo lo que la UI necesita para dibujarse. La UI no calcula reglas: solo lee esto. */
data class RobotUiState(
    val robots: List<RobotInfo> = emptyList(),
    val serviceName: String = "",
    val safePointName: String = "",
    val selectedRobot: RobotInfo? = null,
    val telemetry: Telemetry? = null,
    val isStale: Boolean = false,
    val secondsSinceUpdate: Long? = null,
    val missions: List<MissionTemplate> = emptyList(),
    val availability: Map<CommandType, CommandAvailability> = emptyMap(),
    val recentCommands: List<CommandRecord> = emptyList(),
    val alerts: List<RobotAlert> = emptyList(),
) {
    val unacknowledgedAlerts: Int get() = alerts.count { !it.acknowledged }

    fun availabilityOf(type: CommandType): CommandAvailability =
        availability[type] ?: CommandAvailability(false, "Sin datos")
}

class RobotViewModel(
    private val repository: RobotRepository,
    private val simulation: SimulationControls,
) : ViewModel() {

    private val selectedRobot = MutableStateFlow<RobotInfo?>(null)

    /** Reloj para recalcular "hace cuánto llegó el último dato" aunque no llegue telemetría. */
    private val clock: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(500)
        }
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val customScenarioLabel: String = simulation.customScenarioLabel

    val history: StateFlow<List<HistoryEntry>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<RobotUiState> = combine(
        selectedRobot, repository.telemetry, repository.commands, repository.alerts, clock,
    ) { robot, telemetry, commands, alerts, now ->
        val age = telemetry?.let { now - it.timestampMs }
        val stale = age != null && age > STALE_AFTER_MS
        val pending = commands.filter { it.status == CommandStatus.PENDIENTE }.map { it.type }.toSet()
        val unackedCritical = alerts.any { !it.acknowledged && it.severity == AlertSeverity.CRITICA }
        val unacked = alerts.any { !it.acknowledged }

        RobotUiState(
            robots = repository.robots,
            serviceName = repository.serviceName,
            safePointName = repository.safePointName,
            selectedRobot = robot,
            telemetry = telemetry,
            isStale = stale,
            secondsSinceUpdate = age?.div(1000),
            missions = repository.missions,
            availability = CommandType.entries.associateWith {
                CommandRules.availability(it, telemetry, stale, pending, unackedCritical, unacked)
            },
            recentCommands = commands.take(6),
            alerts = alerts,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RobotUiState(robots = repository.robots, serviceName = repository.serviceName),
    )

    // ---------- Eventos de la UI ----------

    fun selectRobot(robot: RobotInfo) {
        selectedRobot.value = robot
        repository.connect(robot.id)
    }

    fun closeSession() {
        repository.disconnect()
        selectedRobot.value = null
    }

    fun startMission(missionId: String) = send(CommandType.INICIAR_MISION, missionId)

    fun acknowledgeAlert(alertId: String) = send(CommandType.RECONOCER_ALERTA, alertId)

    fun send(type: CommandType, target: String? = null) {
        val availability = uiState.value.availabilityOf(type)
        if (!availability.enabled) {
            _messages.tryEmit(availability.reason ?: "Orden no disponible")
            return
        }
        viewModelScope.launch {
            val result = repository.sendCommand(type, target)
            _messages.emit("${type.label}: ${result.status.label}")
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    // ---------- Simulación (solo demo) ----------

    fun simulateDisconnection(seconds: Int = 15) = runSim { simulation.forceDisconnection(seconds) }
    fun simulateRejection() = runSim { simulation.rejectNextCommand() }
    fun simulateBattery(percent: Int) = runSim { simulation.setBattery(percent) }
    fun simulateObstacle() = runSim { simulation.triggerObstacle() }
    fun simulateCustomScenario() = runSim { simulation.triggerCustomScenario() }

    private fun runSim(block: suspend () -> String) {
        viewModelScope.launch { _messages.emit(block()) }
    }

    companion object {
        const val STALE_AFTER_MS = 3_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RobotHmiApplication
                RobotViewModel(app.container.robotRepository, app.container.simulationControls)
            }
        }
    }
}
