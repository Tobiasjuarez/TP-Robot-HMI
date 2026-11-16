package ar.edu.uade.robothmi.domain.model

// Modelos de dominio. No dependen de Android ni del simulador.

enum class RobotPlatform(val label: String) {
    GO2("Unitree Go2"),
    G1("Unitree G1"),
}

data class RobotInfo(
    val id: String,
    val name: String,
    val platform: RobotPlatform,
    val description: String,
    val simulated: Boolean,
)

enum class Connectivity(val label: String) {
    CONECTADO("Conectado"),
    INESTABLE("Señal débil"),
    DESCONECTADO("Sin conexión"),
}

enum class OperationalState(val label: String) {
    DISPONIBLE("Disponible"),
    EN_MISION("En misión"),
    PAUSADO("En pausa"),
    REQUIERE_AYUDA("Requiere intervención"),
    VOLVIENDO_A_BASE("Volviendo a punto seguro"),
    DETENIDO("Detenido por el operador"),
    CARGANDO("Cargando en base"),
}

enum class MissionStatus(val label: String) {
    EN_CURSO("En curso"),
    PAUSADA("Pausada"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada"),
}

data class Waypoint(val name: String, val zone: String, val x: Float, val y: Float)

data class MissionTemplate(
    val id: String,
    val name: String,
    val description: String,
    val waypoints: List<Waypoint>,
)

data class MissionProgress(
    val missionId: String,
    val name: String,
    val status: MissionStatus,
    val currentWaypoint: Int,
    val totalWaypoints: Int,
    val stage: String,
) {
    val progressPct: Int
        get() = if (totalWaypoints == 0) 0 else currentWaypoint * 100 / totalWaypoints
}

data class Position(val x: Float, val y: Float, val zone: String)

enum class MetricLevel { NORMAL, ADVERTENCIA, CRITICO }

/** Variable propia del servicio (la consigna pide tres). */
data class ServiceMetric(
    val id: String,
    val label: String,
    val value: String,
    val unit: String,
    val level: MetricLevel = MetricLevel.NORMAL,
)

data class Telemetry(
    val robotId: String,
    val connectivity: Connectivity,
    val signalPct: Int,
    val batteryPct: Int,
    val charging: Boolean,
    val state: OperationalState,
    val mission: MissionProgress?,
    val position: Position,
    val speedMps: Float,
    val serviceMetrics: List<ServiceMetric>,
    val timestampMs: Long,
)

enum class CommandType(val label: String) {
    INICIAR_MISION("Iniciar misión"),
    PAUSAR("Pausar"),
    REANUDAR("Reanudar"),
    CANCELAR("Cancelar misión"),
    VOLVER_A_BASE("Volver a punto seguro"),
    DETENCION_OPERATIVA("Solicitar detención"),
    RECONOCER_ALERTA("Reconocer alerta"),
}

enum class CommandStatus(val label: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    RECHAZADA("Rechazada"),
    SIN_RESPUESTA("Sin respuesta"),
}

data class CommandRecord(
    val id: String,
    val type: CommandType,
    val target: String?,
    val sentAtMs: Long,
    val status: CommandStatus,
    val detail: String? = null,
    val respondedAtMs: Long? = null,
)

enum class AlertSeverity(val label: String) {
    INFO("Info"),
    ADVERTENCIA("Advertencia"),
    CRITICA("Crítica"),
}

enum class AlertType { BATERIA_BAJA, DESCONEXION, OBSTACULO, FALLA, SERVICIO }

data class RobotAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val createdAtMs: Long,
    val acknowledged: Boolean = false,
)

enum class EventCategory { MISION, COMANDO, ALERTA, CONEXION, SISTEMA }

data class HistoryEntry(
    val id: Long,
    val timestampMs: Long,
    val category: EventCategory,
    val title: String,
    val detail: String?,
)
