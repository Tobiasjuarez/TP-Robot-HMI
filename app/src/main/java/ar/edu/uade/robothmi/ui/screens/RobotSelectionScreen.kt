package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.edu.uade.robothmi.domain.model.RobotInfo
import ar.edu.uade.robothmi.ui.components.StatusPill
import ar.edu.uade.robothmi.ui.components.Tone

@Composable
fun RobotSelectionScreen(
    robots: List<RobotInfo>,
    serviceName: String,
    onSelect: (RobotInfo) -> Unit,
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Seleccioná un robot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(serviceName, style = MaterialTheme.typography.bodyMedium)
            robots.forEach { robot ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                robot.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (robot.simulated) StatusPill("Simulado", Tone.INFO)
                        }
                        Text("${robot.platform.label} · ${robot.description}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onSelect(robot) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Conectar")
                        }
                    }
                }
            }
        }
    }
}
