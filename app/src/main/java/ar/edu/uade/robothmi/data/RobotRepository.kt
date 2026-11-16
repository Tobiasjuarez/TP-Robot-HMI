package ar.edu.uade.robothmi.data

import ar.edu.uade.robothmi.domain.model.CommandRecord
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.HistoryEntry
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.RobotAlert
import ar.edu.uade.robothmi.domain.model.RobotInfo
import ar.edu.uade.robothmi.domain.model.Telemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato entre la app y "el robot". El ViewModel solo conoce esta interfaz.
 * Hoy la implementa SimulatedRobotRepository; un robot real tendría otra implementación
 * (WebSocket, ROS bridge, etc.) sin tocar la UI.
 */
interface RobotRepository {
    val robots: List<RobotInfo>
    val serviceName: String
    val missions: List<MissionTemplate>

    /** Última telemetría recibida. Puede quedar vieja si se corta la conexión. */
    val telemetry: StateFlow<Telemetry?>
    val alerts: StateFlow<List<RobotAlert>>
    val commands: StateFlow<List<CommandRecord>>

    /** Historial persistido localmente (Room). */
    val history: Flow<List<HistoryEntry>>

    fun connect(robotId: String)
    fun disconnect()

    /**
     * Envía una orden y espera su resolución: CONFIRMADA, RECHAZADA o SIN_RESPUESTA (timeout).
     * Mientras tanto, la orden aparece como PENDIENTE en [commands].
     */
    suspend fun sendCommand(type: CommandType, target: String?): CommandRecord

    suspend fun clearHistory()
}

/**
 * Controles para forzar situaciones en la demo (los 5 escenarios).
 * No existirían con un robot real.
 */
interface SimulationControls {
    val customScenarioLabel: String
    suspend fun forceDisconnection(seconds: Int): String
    suspend fun rejectNextCommand(): String
    suspend fun setBattery(percent: Int): String
    suspend fun triggerObstacle(): String
    suspend fun triggerCustomScenario(): String
}
