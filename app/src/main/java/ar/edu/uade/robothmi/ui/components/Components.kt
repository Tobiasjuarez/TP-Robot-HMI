package ar.edu.uade.robothmi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.edu.uade.robothmi.domain.CommandAvailability
import ar.edu.uade.robothmi.domain.model.AlertSeverity
import ar.edu.uade.robothmi.domain.model.CommandStatus
import ar.edu.uade.robothmi.domain.model.MetricLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tono visual. Siempre se combina con texto + símbolo: el estado nunca depende solo del color. */
enum class Tone { OK, WARNING, ERROR, INFO, NEUTRAL }

@Composable
fun toneColor(tone: Tone): Color = when (tone) {
    Tone.OK -> Color(0xFF2E7D32)
    Tone.WARNING -> Color(0xFFB26A00)
    Tone.ERROR -> MaterialTheme.colorScheme.error
    Tone.INFO -> MaterialTheme.colorScheme.primary
    Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun toneGlyph(tone: Tone): String = when (tone) {
    Tone.OK -> "✓"
    Tone.WARNING -> "⚠"
    Tone.ERROR -> "✕"
    Tone.INFO -> "ℹ"
    Tone.NEUTRAL -> "•"
}

fun MetricLevel.toTone() = when (this) {
    MetricLevel.NORMAL -> Tone.OK
    MetricLevel.ADVERTENCIA -> Tone.WARNING
    MetricLevel.CRITICO -> Tone.ERROR
}

fun AlertSeverity.toTone() = when (this) {
    AlertSeverity.INFO -> Tone.INFO
    AlertSeverity.ADVERTENCIA -> Tone.WARNING
    AlertSeverity.CRITICA -> Tone.ERROR
}

fun CommandStatus.toTone() = when (this) {
    CommandStatus.PENDIENTE -> Tone.INFO
    CommandStatus.CONFIRMADA -> Tone.OK
    CommandStatus.RECHAZADA -> Tone.ERROR
    CommandStatus.SIN_RESPUESTA -> Tone.WARNING
}

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val color = toneColor(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = "${toneGlyph(tone)} $text",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    supporting: String? = null,
    dimmed: Boolean = false,
) {
    OutlinedCard(modifier = modifier) {
        Column(
            Modifier
                .padding(12.dp)
                .alpha(if (dimmed) 0.45f else 1f),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tone != Tone.NEUTRAL) {
                    Text("${toneGlyph(tone)} ", color = toneColor(tone), fontWeight = FontWeight.Bold)
                }
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StaleBanner(seconds: Long?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("⚠ Sin datos nuevos hace ${seconds ?: "?"} s", fontWeight = FontWeight.Bold)
            Text(
                "Los valores pueden no ser actuales. Las órdenes pueden quedar sin respuesta.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

/** Explica por qué un control está deshabilitado (o qué riesgo tiene). */
@Composable
fun AvailabilityHint(availability: CommandAvailability, modifier: Modifier = Modifier) {
    val reason = availability.reason ?: return
    val tone = if (availability.warning) Tone.WARNING else Tone.NEUTRAL
    Text(
        text = "${toneGlyph(tone)} $reason",
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = toneColor(tone),
    )
}

fun formatTime(ms: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ms))

fun formatDateTime(ms: Long): String = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(ms))
