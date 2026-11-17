package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Panel para la demo: fuerza las situaciones de los 5 escenarios.
 * Con un robot real esta pantalla no existiría.
 */
@Composable
fun SimulatorScreen(
    customScenarioLabel: String,
    onDisconnect: () -> Unit,
    onReject: () -> Unit,
    onBattery: (Int) -> Unit,
    onObstacle: () -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Controles del simulador (solo para la demo). Ver docs/escenarios.md.",
            style = MaterialTheme.typography.bodySmall,
        )

        ScenarioCard("1. Misión normal", "Iniciá una misión desde el Panel y dejala terminar.")

        ScenarioCard(
            "2. Batería baja",
            "18 %: advertencia y regreso recomendado. 8 % en misión: el robot se detiene sin dejar sola a la persona y pide ayuda.",
        ) {
            OutlinedButton(onClick = { onBattery(18) }, modifier = Modifier.weight(1f)) { Text("Batería 18 %") }
            OutlinedButton(onClick = { onBattery(8) }, modifier = Modifier.weight(1f)) { Text("Batería 8 %") }
        }

        ScenarioCard("3. Desconexión", "Corta la telemetría 15 s: datos viejos, órdenes sin confirmar y recuperación.") {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("Cortar conexión 15 s") }
        }

        ScenarioCard("4. Incidente", "Paso bloqueado (por ejemplo, pasillo lleno en el cambio de turno): el robot se frena y pide una decisión. Requiere misión en curso.") {
            OutlinedButton(onClick = onObstacle, modifier = Modifier.fillMaxWidth()) { Text("Generar obstáculo") }
        }

        ScenarioCard("5. Escenario propio", customScenarioLabel) {
            OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("Disparar escenario propio") }
        }

        ScenarioCard("Extra: rechazo", "La próxima orden que mandes vuelve RECHAZADA.") {
            OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) { Text("Rechazar próxima orden") }
        }
    }
}

@Composable
private fun ScenarioCard(
    title: String,
    description: String,
    actions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (actions != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
        }
    }
}
