package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.robothmi.domain.model.EventCategory
import ar.edu.uade.robothmi.domain.model.HistoryEntry
import ar.edu.uade.robothmi.ui.components.StatusPill
import ar.edu.uade.robothmi.ui.components.Tone
import ar.edu.uade.robothmi.ui.components.formatDateTime

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entries.size} eventos guardados en el dispositivo",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onClear, enabled = entries.isNotEmpty()) { Text("Borrar") }
            }
        }
        items(entries, key = { it.id }) { entry ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(entry.category.label(), entry.category.tone())
                    Text(
                        "  ${formatDateTime(entry.timestampMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                entry.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun EventCategory.label() = when (this) {
    EventCategory.MISION -> "Misión"
    EventCategory.COMANDO -> "Orden"
    EventCategory.ALERTA -> "Alerta"
    EventCategory.CONEXION -> "Conexión"
    EventCategory.SISTEMA -> "Sistema"
}

private fun EventCategory.tone() = when (this) {
    EventCategory.ALERTA -> Tone.WARNING
    EventCategory.CONEXION -> Tone.INFO
    else -> Tone.NEUTRAL
}
