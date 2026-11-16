package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.robothmi.domain.CommandAvailability
import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.RobotAlert
import ar.edu.uade.robothmi.ui.components.AvailabilityHint
import ar.edu.uade.robothmi.ui.components.StatusPill
import ar.edu.uade.robothmi.ui.components.Tone
import ar.edu.uade.robothmi.ui.components.formatTime
import ar.edu.uade.robothmi.ui.components.toTone

@Composable
fun AlertsScreen(
    alerts: List<RobotAlert>,
    ackAvailability: CommandAvailability,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (alerts.isEmpty()) {
            item { Text("Sin alertas.") }
        }
        items(alerts, key = { it.id }) { alert ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(alert.severity.label, alert.severity.toTone())
                        Spacer(Modifier.weight(1f))
                        Text(formatTime(alert.createdAtMs), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                    if (alert.acknowledged) {
                        StatusPill("Reconocida", Tone.OK)
                    } else {
                        Button(onClick = { onAcknowledge(alert.id) }, enabled = ackAvailability.enabled) {
                            Text("Reconocer")
                        }
                        if (!ackAvailability.enabled) AvailabilityHint(ackAvailability)
                        if (alert.severity == AlertSeverity.CRITICA) {
                            Text(
                                "Después de reconocerla, decidí en el Panel qué hace el robot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
