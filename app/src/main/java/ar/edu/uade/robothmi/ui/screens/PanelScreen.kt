package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.edu.uade.robothmi.domain.CommandRules
import ar.edu.uade.robothmi.domain.model.CommandRecord
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.Connectivity
import ar.edu.uade.robothmi.domain.model.MissionStatus
import ar.edu.uade.robothmi.domain.model.MissionTemplate
import ar.edu.uade.robothmi.domain.model.OperationalState
import ar.edu.uade.robothmi.domain.model.Telemetry
import ar.edu.uade.robothmi.ui.RobotUiState
import ar.edu.uade.robothmi.ui.components.AvailabilityHint
import ar.edu.uade.robothmi.ui.components.MetricTile
import ar.edu.uade.robothmi.ui.components.SectionTitle
import ar.edu.uade.robothmi.ui.components.StatusPill
import ar.edu.uade.robothmi.ui.components.Tone
import ar.edu.uade.robothmi.ui.components.formatTime
import ar.edu.uade.robothmi.ui.components.toTone

/** Órdenes que piden confirmación (alto impacto). La detención NO pide confirmación: tiene que ser rápida. */
private val NEEDS_CONFIRMATION = setOf(CommandType.CANCELAR, CommandType.VOLVER_A_BASE)

@Composable
fun PanelScreen(
    state: RobotUiState,
    onSend: (CommandType) -> Unit,
    onStartMission: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var toConfirm by remember { mutableStateOf<CommandType?>(null) }
    var missionToStart by remember { mutableStateOf<MissionTemplate?>(null) }
    val telemetry = state.telemetry

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (telemetry == null) {
            Text("Esperando la primera telemetría…")
        } else {
            StatusHeader(telemetry, state)
            TelemetryGrid(telemetry, state)
            MissionSection(state, telemetry, onRequestStart = { missionToStart = it })
            CommandsSection(state) { type ->
                if (type in NEEDS_CONFIRMATION) toConfirm = type else onSend(type)
            }
            RecentCommands(state.recentCommands)
        }
    }

    toConfirm?.let { type ->
        AlertDialog(
            onDismissRequest = { toConfirm = null },
            title = { Text("¿${type.label}?") },
            text = { Text(confirmationText(type)) },
            confirmButton = {
                TextButton(onClick = {
                    toConfirm = null
                    onSend(type)
                }) { Text("Sí, enviar") }
            },
            dismissButton = { TextButton(onClick = { toConfirm = null }) { Text("Volver") } },
        )
    }

    missionToStart?.let { mission ->
        AlertDialog(
            onDismissRequest = { missionToStart = null },
            title = { Text("¿Iniciar \"${mission.name}\"?") },
            text = {
                Text(
                    "Recorrido: ${mission.waypoints.joinToString(" → ") { it.name }}\n\n" +
                        "Batería actual: ${telemetry?.batteryPct ?: "?"} %",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    missionToStart = null
                    onStartMission(mission.id)
                }) { Text("Iniciar") }
            },
            dismissButton = { TextButton(onClick = { missionToStart = null }) { Text("Volver") } },
        )
    }
}

private fun confirmationText(type: CommandType) = when (type) {
    CommandType.CANCELAR -> "La misión termina y el robot queda quieto donde está. No se puede deshacer."
    CommandType.VOLVER_A_BASE -> "Si hay una misión activa, se interrumpe y el robot vuelve a la base de carga."
    else -> "Confirmá la orden."
}

// ---------------------------------------------------------------------------

@Composable
private fun StatusHeader(t: Telemetry, state: RobotUiState) {
    val (hint, tone) = nextActionHint(t, state)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado actual", style = MaterialTheme.typography.labelMedium)
            Text(t.state.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            StatusPill(hint, tone)
        }
    }
}

/** Jerarquía: primero estado y si hay que actuar. */
private fun nextActionHint(t: Telemetry, state: RobotUiState): Pair<String, Tone> = when {
    state.isStale -> "Datos desactualizados: no decidas con estos valores" to Tone.ERROR
    t.state == OperationalState.REQUIERE_AYUDA -> "El robot espera tu decisión. Revisá Alertas" to Tone.ERROR
    t.batteryPct < CommandRules.LOW_BATTERY &&
        t.state != OperationalState.CARGANDO &&
        t.state != OperationalState.VOLVIENDO_A_BASE -> "Batería baja: considerá volver a punto seguro" to Tone.WARNING
    t.state == OperationalState.DETENIDO -> "Detenido. Podés reanudar, cancelar o volver a base" to Tone.WARNING
    t.state == OperationalState.PAUSADO -> "En pausa. Podés reanudar o cancelar" to Tone.WARNING
    t.state == OperationalState.EN_MISION -> "Misión en curso. No necesitás intervenir" to Tone.OK
    t.state == OperationalState.VOLVIENDO_A_BASE -> "Volviendo a punto seguro" to Tone.INFO
    t.state == OperationalState.CARGANDO -> "En base, cargando" to Tone.INFO
    else -> "Disponible para una nueva misión" to Tone.OK
}

private data class TileData(val label: String, val value: String, val tone: Tone, val supporting: String? = null)

@Composable
private fun TelemetryGrid(t: Telemetry, state: RobotUiState) {
    val stale = state.isStale
    val connectionTone = when {
        stale -> Tone.ERROR
        t.connectivity == Connectivity.CONECTADO -> Tone.OK
        t.connectivity == Connectivity.INESTABLE -> Tone.WARNING
        else -> Tone.ERROR
    }
    val batteryTone = when {
        t.batteryPct < CommandRules.CRITICAL_BATTERY -> Tone.ERROR
        t.batteryPct < CommandRules.LOW_BATTERY -> Tone.WARNING
        else -> Tone.OK
    }
    val mission = t.mission
    val tiles = listOf(
        TileData("Conexión", if (stale) "Sin datos" else t.connectivity.label, connectionTone, "Señal ${t.signalPct} %"),
        TileData("Batería", "${t.batteryPct} %", batteryTone, if (t.charging) "Cargando" else null),
        TileData(
            "Misión",
            if (mission == null) "Sin misión" else "${mission.progressPct} %",
            Tone.NEUTRAL,
            mission?.let { "${it.name} · ${it.status.label}" },
        ),
        TileData("Ubicación", t.position.zone, Tone.NEUTRAL, "x %.1f · y %.1f m".format(t.position.x, t.position.y)),
        TileData("Movimiento", if (t.speedMps < 0.05f) "Quieto" else "%.1f m/s".format(t.speedMps), Tone.NEUTRAL),
        TileData(
            "Alertas",
            if (state.unacknowledgedAlerts == 0) "Ninguna" else "${state.unacknowledgedAlerts} sin reconocer",
            if (state.unacknowledgedAlerts == 0) Tone.OK else Tone.WARNING,
        ),
    ) + t.serviceMetrics.map { m ->
        TileData(m.label, listOf(m.value, m.unit).filter { it.isNotBlank() }.joinToString(" "), m.level.toTone())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Telemetría")
        MetricTile(
            label = "Última actualización",
            value = formatTime(t.timestampMs),
            tone = if (stale) Tone.ERROR else Tone.OK,
            supporting = "hace ${state.secondsSinceUpdate ?: 0} s",
            modifier = Modifier.fillMaxWidth(),
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile ->
                    MetricTile(
                        label = tile.label,
                        value = tile.value,
                        tone = tile.tone,
                        supporting = tile.supporting,
                        dimmed = stale,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MissionSection(state: RobotUiState, t: Telemetry, onRequestStart: (MissionTemplate) -> Unit) {
    val mission = t.mission
    val active = mission != null && (mission.status == MissionStatus.EN_CURSO || mission.status == MissionStatus.PAUSADA)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Misión")
            if (active && mission != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mission.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    StatusPill(mission.status.label, if (mission.status == MissionStatus.EN_CURSO) Tone.OK else Tone.WARNING)
                }
                Text(mission.stage, style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = { mission.progressPct / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Punto ${mission.currentWaypoint} de ${mission.totalWaypoints}", style = MaterialTheme.typography.bodySmall)
            } else {
                if (mission != null) {
                    Text("Última: ${mission.name} · ${mission.status.label}", style = MaterialTheme.typography.bodySmall)
                }
                val startAvailability = state.availabilityOf(CommandType.INICIAR_MISION)
                state.missions.forEach { template ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleSmall)
                            Text(template.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onRequestStart(template) }, enabled = startAvailability.enabled) {
                            Text("Iniciar")
                        }
                    }
                }
                AvailabilityHint(startAvailability)
            }
        }
    }
}

@Composable
private fun CommandsSection(state: RobotUiState, onCommand: (CommandType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Control")
        listOf(CommandType.PAUSAR, CommandType.REANUDAR, CommandType.CANCELAR, CommandType.VOLVER_A_BASE)
            .forEach { type ->
                val availability = state.availabilityOf(type)
                OutlinedButton(
                    onClick = { onCommand(type) },
                    enabled = availability.enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(type.label) }
                AvailabilityHint(availability)
            }

        val stop = state.availabilityOf(CommandType.DETENCION_OPERATIVA)
        Button(
            onClick = { onCommand(CommandType.DETENCION_OPERATIVA) },
            enabled = stop.enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text("■ Solicitar detención (software)") }
        AvailabilityHint(stop)
        Text(
            "Es una orden de software simulada: no reemplaza la parada de emergencia física del robot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentCommands(commands: List<CommandRecord>) {
    if (commands.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Últimas órdenes")
        commands.forEach { cmd ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${formatTime(cmd.sentAtMs)}  ${cmd.type.label}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        StatusPill(cmd.status.label, cmd.status.toTone())
                    }
                    cmd.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
