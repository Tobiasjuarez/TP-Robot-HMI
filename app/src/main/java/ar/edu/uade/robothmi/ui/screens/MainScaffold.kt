package ar.edu.uade.robothmi.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.robothmi.domain.model.CommandType
import ar.edu.uade.robothmi.domain.model.Connectivity
import ar.edu.uade.robothmi.domain.model.RobotInfo
import ar.edu.uade.robothmi.ui.RobotUiState
import ar.edu.uade.robothmi.ui.RobotViewModel
import ar.edu.uade.robothmi.ui.components.StaleBanner
import ar.edu.uade.robothmi.ui.components.StatusPill
import ar.edu.uade.robothmi.ui.components.Tone

private enum class Tab(val label: String, val glyph: String) {
    PANEL("Panel", "◉"),
    ALERTAS("Alertas", "⚠"),
    HISTORIAL("Historial", "☰"),
    SIMULACION("Simulación", "⚙"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(vm: RobotViewModel, state: RobotUiState, robot: RobotInfo) {
    var tab by rememberSaveable { mutableStateOf(Tab.PANEL) }
    val snackbar = remember { SnackbarHostState() }
    val history by vm.history.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(robot.name)
                        Text(state.serviceName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    ConnectionPill(state)
                    TextButton(onClick = vm::closeSession) { Text("Salir") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val label = if (t == Tab.ALERTAS && state.unacknowledgedAlerts > 0) {
                        "Alertas (${state.unacknowledgedAlerts})"
                    } else {
                        t.label
                    }
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.glyph) },
                        label = { Text(label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isStale) StaleBanner(state.secondsSinceUpdate)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (tab) {
                    Tab.PANEL -> PanelScreen(
                        state = state,
                        onSend = { vm.send(it) },
                        onStartMission = vm::startMission,
                    )
                    Tab.ALERTAS -> AlertsScreen(
                        alerts = state.alerts,
                        ackAvailability = state.availabilityOf(CommandType.RECONOCER_ALERTA),
                        onAcknowledge = vm::acknowledgeAlert,
                    )
                    Tab.HISTORIAL -> HistoryScreen(
                        entries = history,
                        onClear = vm::clearHistory,
                    )
                    Tab.SIMULACION -> SimulatorScreen(
                        customScenarioLabel = vm.customScenarioLabel,
                        onDisconnect = { vm.simulateDisconnection(15) },
                        onReject = vm::simulateRejection,
                        onBattery = vm::simulateBattery,
                        onObstacle = vm::simulateObstacle,
                        onCustom = vm::simulateCustomScenario,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionPill(state: RobotUiState) {
    val telemetry = state.telemetry
    when {
        telemetry == null -> StatusPill("Conectando…", Tone.NEUTRAL)
        state.isStale -> StatusPill("Sin datos", Tone.ERROR)
        telemetry.connectivity == Connectivity.CONECTADO -> StatusPill("Conectado", Tone.OK)
        telemetry.connectivity == Connectivity.INESTABLE -> StatusPill("Señal débil", Tone.WARNING)
        else -> StatusPill("Sin conexión", Tone.ERROR)
    }
}
