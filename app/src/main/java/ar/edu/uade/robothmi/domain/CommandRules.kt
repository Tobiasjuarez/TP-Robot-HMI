package ar.edu.uade.robothmi.domain

import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.Connectivity
import ar.edu.uade.robothmi.domain.model.MissionStatus
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.Telemetry

/**
 * Resultado de evaluar si una orden puede enviarse.
 * [warning] = se puede enviar, pero hay que avisarle algo al operador.
 */
data class CommandAvailability(
    val enabled: Boolean,
    val reason: String? = null,
    val warning: Boolean = false,
)

/**
 * Condiciones de habilitación de cada comando (lado app).
 * El robot vuelve a validar al recibir la orden: la app puede estar mostrando datos viejos.
 * Es una función pura para poder testearla sin Android (ver CommandRulesTest).
 */
object CommandRules {
    const val MIN_BATTERY_TO_START = 25
    const val LOW_BATTERY = 20
    const val CRITICAL_BATTERY = 10

    /** Órdenes de seguridad: se permiten aun sin conexión (pueden quedar sin respuesta). */
    val SAFETY_COMMANDS = setOf(CommandType.DETENCION_OPERATIVA, CommandType.VOLVER_A_BASE)

    fun availability(
        type: CommandType,
        telemetry: Telemetry?,
        isStale: Boolean,
        pending: Set<CommandType>,
        hasUnackedCritical: Boolean,
        hasUnackedAlerts: Boolean,
    ): CommandAvailability {
        if (telemetry == null) return no("Todavía no hay datos del robot")
        if (type in pending) return no("Esperando respuesta de una orden igual")

        if (isStale || telemetry.connectivity == Connectivity.DESCONECTADO) {
            return if (type in SAFETY_COMMANDS) {
                CommandAvailability(true, "Sin conexión: se envía igual, pero puede quedar sin respuesta", warning = true)
            } else {
                no("Sin conexión: la orden no podría confirmarse")
            }
        }

        val state = telemetry.state
        val mission = telemetry.mission
        val missionActive = mission != null &&
            (mission.status == MissionStatus.EN_CURSO || mission.status == MissionStatus.PAUSADA)

        return when (type) {
            CommandType.INICIAR_MISION -> when {
                state != OperationalState.DISPONIBLE && state != OperationalState.CARGANDO ->
                    no("El robot ya tiene una tarea en curso (${state.label})")
                telemetry.batteryPct < MIN_BATTERY_TO_START ->
                    no("Batería insuficiente: mínimo $MIN_BATTERY_TO_START %")
                else -> ok()
            }

            CommandType.PAUSAR ->
                if (state == OperationalState.EN_MISION) ok() else no("Solo con una misión en curso")

            CommandType.REANUDAR -> when {
                mission?.status != MissionStatus.PAUSADA -> no("No hay una misión pausada")
                telemetry.batteryPct < CRITICAL_BATTERY -> no("Batería crítica: no se puede seguir la misión")
                state == OperationalState.REQUIERE_AYUDA && hasUnackedCritical ->
                    no("Reconocé la alerta crítica antes de reanudar")
                state != OperationalState.PAUSADO &&
                    state != OperationalState.DETENIDO &&
                    state != OperationalState.REQUIERE_AYUDA -> no("No se puede reanudar desde ${state.label}")
                else -> ok()
            }

            CommandType.CANCELAR ->
                if (missionActive) ok() else no("No hay misión activa")

            CommandType.VOLVER_A_BASE -> when {
                state == OperationalState.VOLVIENDO_A_BASE || state == OperationalState.CARGANDO ->
                    no("Ya está en punto seguro o yendo hacia él")
                telemetry.batteryPct < LOW_BATTERY ->
                    CommandAvailability(true, "Recomendado: batería baja", warning = true)
                else -> ok()
            }

            CommandType.DETENCION_OPERATIVA -> when (state) {
                OperationalState.EN_MISION,
                OperationalState.VOLVIENDO_A_BASE,
                OperationalState.PAUSADO,
                OperationalState.REQUIERE_AYUDA -> ok()
                else -> no("El robot ya está quieto")
            }

            CommandType.RECONOCER_ALERTA ->
                if (hasUnackedAlerts) ok() else no("No hay alertas pendientes")
        }
    }

    private fun ok() = CommandAvailability(true)
    private fun no(reason: String) = CommandAvailability(false, reason)
}
