package ar.edu.uade.robothmi.data.sim

import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.MetricLevel
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.RobotPlatform
import ar.edu.uade.robothmi.domain.model.ServiceMetric
import ar.edu.uade.robothmi.domain.model.Waypoint
import kotlin.math.max
import kotlin.random.Random

/** Evento propio del servicio (lectura anómala, hallazgo, etc.). */
data class ServiceEvent(
    val severity: AlertSeverity,
    val message: String,
    /** Si es true y el robot está en misión, se frena y pide una decisión del operador. */
    val requiresOperator: Boolean = false,
)

/**
 * TODO lo específico del caso de uso vive acá: recorrido, misiones, las 3 variables
 * del servicio y el "escenario propio". Para cambiar de caso, crear otra implementación
 * y pasarla en AppContainer.
 */
interface ServiceProfile {
    val serviceName: String
    val platform: RobotPlatform
    val robotDescription: String
    val base: Waypoint
    val missions: List<MissionTemplate>
    val customScenarioLabel: String

    fun resetForMission(mission: MissionTemplate)
    fun metrics(): List<ServiceMetric>
    fun onWaypointReached(waypoint: Waypoint, random: Random): ServiceEvent?
    fun customScenario(): ServiceEvent
}

/**
 * EJEMPLO: ronda de inspección en planta con Go2.
 * Coordenadas en metros sobre un plano de ~40 x 30 m.
 */
class InspeccionPlantaProfile : ServiceProfile {
    override val serviceName = "Inspección de planta (ejemplo)"
    override val platform = RobotPlatform.GO2
    override val robotDescription = "Cuadrúpedo con cámara de inspección (simulado)"
    override val base = Waypoint("Base de carga", "Sala de control", 2f, 2f)

    private val ronda = listOf(
        Waypoint("Bomba P-101", "Sala de bombas", 10f, 4f),
        Waypoint("Manómetro línea 2", "Sala de bombas", 16f, 9f),
        Waypoint("Tablero eléctrico A", "Sala de tableros", 24f, 14f),
        Waypoint("Tablero eléctrico B", "Sala de tableros", 30f, 14f),
        Waypoint("Válvula V-12", "Entrepiso (por escalera)", 34f, 22f),
        Waypoint("Compresor C-3", "Nave principal", 20f, 26f),
    )

    override val missions = listOf(
        MissionTemplate("ronda-completa", "Ronda completa", "6 puntos, incluye el entrepiso por escalera", ronda),
        MissionTemplate("ronda-tableros", "Solo tableros eléctricos", "2 puntos en la sala de tableros", ronda.subList(2, 4)),
    )

    override val customScenarioLabel = "Temperatura crítica en un tablero"

    private var total = 0
    private var inspected = 0
    private var maxTemp = 0f
    private var outOfRange = 0

    override fun resetForMission(mission: MissionTemplate) {
        total = mission.waypoints.size
        inspected = 0
        maxTemp = 0f
        outOfRange = 0
    }

    override fun metrics() = listOf(
        ServiceMetric("puntos", "Puntos inspeccionados", "$inspected/$total", ""),
        ServiceMetric(
            id = "temp_max",
            label = "Temp. máx. detectada",
            value = if (maxTemp == 0f) "—" else "%.0f".format(maxTemp),
            unit = if (maxTemp == 0f) "" else "°C",
            level = when {
                maxTemp >= 70f -> MetricLevel.CRITICO
                maxTemp >= 55f -> MetricLevel.ADVERTENCIA
                else -> MetricLevel.NORMAL
            },
        ),
        ServiceMetric(
            id = "fuera_rango",
            label = "Lecturas fuera de rango",
            value = "$outOfRange",
            unit = "",
            level = if (outOfRange > 0) MetricLevel.ADVERTENCIA else MetricLevel.NORMAL,
        ),
    )

    override fun onWaypointReached(waypoint: Waypoint, random: Random): ServiceEvent? {
        inspected++
        maxTemp = max(maxTemp, 32f + random.nextFloat() * 22f)
        return if (random.nextFloat() < 0.2f) {
            outOfRange++
            ServiceEvent(AlertSeverity.ADVERTENCIA, "Lectura fuera de rango en ${waypoint.name}. Quedó registrada para revisión.")
        } else {
            null
        }
    }

    override fun customScenario(): ServiceEvent {
        maxTemp = 78f
        outOfRange++
        return ServiceEvent(
            severity = AlertSeverity.CRITICA,
            message = "Temperatura de 78 °C en Tablero eléctrico B. Decidí si enviar un técnico y si el robot sigue la ronda.",
            requiresOperator = true,
        )
    }
}
