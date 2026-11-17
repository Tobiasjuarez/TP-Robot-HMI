package ar.edu.uade.robothmi.data.sim

import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.RobotPlatform
import ar.edu.uade.robothmi.domain.model.ServiceMetric
import ar.edu.uade.robothmi.domain.model.Waypoint
import kotlin.random.Random

/** Evento propio del servicio (hallazgo, aviso, situación con la persona). */
data class ServiceEvent(
    val severity: AlertSeverity,
    val message: String,
    /** Si es true y el robot está en misión, se frena y pide una decisión del operador. */
    val requiresOperator: Boolean = false,
)

/** Lo que el simulador le cuenta al perfil en cada momento. */
data class SimContext(
    val state: OperationalState,
    val missionActive: Boolean,
    val moving: Boolean,
    val remainingMeters: Float?,
)

/**
 * TODO lo específico del caso de uso vive acá: recorrido, misiones, las variables
 * del servicio, el escenario propio y algunas reglas (por ejemplo, qué hacer con
 * batería crítica). El simulador genérico está en SimulatedRobotRepository.
 */
interface ServiceProfile {
    val serviceName: String
    val platform: RobotPlatform
    val robotDescription: String

    /** Punto seguro: adonde vuelve el robot con "Volver a punto seguro". */
    val base: Waypoint
    val missions: List<MissionTemplate>
    val customScenarioLabel: String

    /**
     * true: con batería crítica el robot vuelve solo al punto seguro.
     * false: si está en misión se detiene donde está y pide ayuda (servicios que acompañan personas).
     */
    val autoReturnOnCriticalBattery: Boolean

    fun resetForMission(mission: MissionTemplate)
    fun metrics(ctx: SimContext): List<ServiceMetric>
    fun onTick(ctx: SimContext, random: Random): ServiceEvent? = null
    fun onWaypointReached(waypoint: Waypoint, random: Random): ServiceEvent?
    fun onCommandAccepted(type: CommandType) {}

    /** Multiplica la velocidad del robot (1 = normal). */
    fun speedFactor(): Float = 1f
    fun customScenario(): ServiceEvent
}
