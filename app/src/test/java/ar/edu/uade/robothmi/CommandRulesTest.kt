package ar.edu.uade.robothmi

import ar.edu.uade.robothmi.domain.CommandRules
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.Connectivity
import ar.edu.uade.robothmi.domain.model.MissionProgress
import ar.edu.uade.robothmi.domain.model.MissionStatus
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.Position
import ar.edu.uade.robothmi.domain.model.Telemetry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRulesTest {

    private fun telemetry(
        state: OperationalState = OperationalState.DISPONIBLE,
        battery: Int = 80,
        missionStatus: MissionStatus? = null,
    ) = Telemetry(
        robotId = "r1",
        connectivity = Connectivity.CONECTADO,
        signalPct = 90,
        batteryPct = battery,
        charging = false,
        state = state,
        mission = missionStatus?.let { MissionProgress("m1", "Ronda", it, 1, 4, "") },
        position = Position(0f, 0f, "Base"),
        speedMps = 0f,
        serviceMetrics = emptyList(),
        timestampMs = 0L,
    )

    private fun check(
        type: CommandType,
        t: Telemetry,
        stale: Boolean = false,
        pending: Set<CommandType> = emptySet(),
        unackedCritical: Boolean = false,
    ) = CommandRules.availability(type, t, stale, pending, unackedCritical, unackedCritical)

    @Test
    fun iniciarMision_conBateriaSuficiente_habilitado() {
        assertTrue(check(CommandType.INICIAR_MISION, telemetry()).enabled)
    }

    @Test
    fun iniciarMision_conBateriaBaja_deshabilitado() {
        assertFalse(check(CommandType.INICIAR_MISION, telemetry(battery = 15)).enabled)
    }

    @Test
    fun pausar_soloConMisionEnCurso() {
        assertFalse(check(CommandType.PAUSAR, telemetry()).enabled)
        assertTrue(
            check(CommandType.PAUSAR, telemetry(OperationalState.EN_MISION, missionStatus = MissionStatus.EN_CURSO)).enabled,
        )
    }

    @Test
    fun reanudar_conAlertaCriticaSinReconocer_deshabilitado() {
        val t = telemetry(OperationalState.REQUIERE_AYUDA, missionStatus = MissionStatus.PAUSADA)
        assertFalse(check(CommandType.REANUDAR, t, unackedCritical = true).enabled)
        assertTrue(check(CommandType.REANUDAR, t, unackedCritical = false).enabled)
    }

    @Test
    fun sinConexion_soloOrdenesDeSeguridad() {
        val t = telemetry(OperationalState.EN_MISION, missionStatus = MissionStatus.EN_CURSO)
        assertFalse(check(CommandType.PAUSAR, t, stale = true).enabled)
        assertTrue(check(CommandType.DETENCION_OPERATIVA, t, stale = true).enabled)
        assertTrue(check(CommandType.DETENCION_OPERATIVA, t, stale = true).warning)
    }

    @Test
    fun ordenIgualPendiente_bloqueaDuplicado() {
        val t = telemetry(OperationalState.EN_MISION, missionStatus = MissionStatus.EN_CURSO)
        assertFalse(check(CommandType.PAUSAR, t, pending = setOf(CommandType.PAUSAR)).enabled)
    }
}
