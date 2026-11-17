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
 * Caso de uso: Go2 que guía a personas ciegas o con baja visión dentro del Campus UADE Monserrat.
 * La persona va tomada de un asa; el robot le avisa por voz giros, ascensores y llegadas.
 * Un operador supervisa los viajes desde el Punto de encuentro.
 *
 * Plano: la manzana Lima / Chile / Salta / Av. Independencia, con los edificios del
 * "Mapa UADE" (Lima 1-3, Chile 1-3, Salta, Independencia 1-2, UADE Labs) y el patio central.
 * Coordenadas en metros: x de oeste (Av. Independencia) a este (Chile), y de sur (Lima) a norte (Salta).
 * La ubicación de edificios y entradas sale del mapa oficial; la de ascensores y pasillos
 * interiores es APROXIMADA (verificar en la sede).
 */
class GuiaUadeProfile : ServiceProfile {
    override val serviceName = "Guía para personas con baja visión · Campus UADE Monserrat"
    override val platform = RobotPlatform.GO2
    override val robotDescription = "Guía con asa y voz (simulado)"
    override val base = Waypoint(
        name = "Punto de encuentro",
        zone = "Entrada peatonal Lima",
        x = 49f, y = 12f,
    )
    override val customScenarioLabel = "La persona soltó el asa"

    /** Nunca deja sola a la persona: con batería crítica se detiene y pide ayuda. */
    override val autoReturnOnCriticalBattery = false

    // ---------- Puntos del campus ----------
    private val hallLima1 = Waypoint(
        "Hall de Lima 1", "Lima 1 · Planta baja", 66f, 22f,
        announcement = "Entramos a Lima 1. Seguimos derecho.",
    )
    private val patio = Waypoint(
        "Patio central", "Patio central", 62f, 58f,
        announcement = "Estamos en el patio central, al aire libre. El piso es parejo.",
    )

    override val missions = listOf(
        MissionTemplate(
            id = "aula-225",
            name = "Guiar al Aula 225",
            description = "Lima 2 · 2.º piso (entrada por Lima, pasando por Lima 1)",
            waypoints = listOf(
                hallLima1,
                Waypoint("Paso a Lima 2", "Lima 1 → Lima 2 · Planta baja", 58f, 27f, "Giramos a la izquierda para pasar a Lima 2."),
                Waypoint("Ascensores de Lima 2", "Lima 2 · Planta baja", 49f, 30f, "Llegamos a los ascensores. Esperamos a que abra la puerta."),
                Waypoint("Salida del ascensor, 2.º piso", "Lima 2 · 2.º piso", 49f, 31f, "Estamos en el segundo piso. Salimos del ascensor."),
                Waypoint("Aula 225", "Lima 2 · 2.º piso · Aula 225", 46f, 37f, "Llegamos al Aula 225. La puerta está frente a vos."),
            ),
        ),
        MissionTemplate(
            id = "uade-labs",
            name = "Guiar a UADE Labs",
            description = "Por el patio central hasta el edificio sobre Av. Independencia",
            waypoints = listOf(
                hallLima1,
                patio,
                Waypoint("Patio, lado oeste", "Patio central · frente a UADE Labs", 40f, 78f, "Seguimos derecho. UADE Labs está adelante."),
                Waypoint("UADE Labs", "UADE Labs · Planta baja", 28f, 82f, "Llegamos a UADE Labs. La entrada está frente a vos."),
            ),
        ),
        MissionTemplate(
            id = "parada-independencia",
            name = "Guiar a la parada de autos",
            description = "Salida por Independencia 2 a Av. Independencia (taxi, remís o familiar)",
            waypoints = listOf(
                hallLima1,
                patio,
                Waypoint("Independencia 2", "Independencia 2 · Planta baja", 40f, 60f, "Entramos a Independencia 2. Seguimos derecho hacia la avenida."),
                Waypoint("Parada de autos", "Salida Av. Independencia · parada de autos", 6f, 58f, "Llegamos a la parada de autos de avenida Independencia. Esperá acá: el cordón está adelante."),
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
        lastVoice = "Hola. Tomá el asa. Destino: ${mission.waypoints.last().name}."
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
