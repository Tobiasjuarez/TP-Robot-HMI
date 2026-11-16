package ar.edu.uade.robothmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.uade.robothmi.ui.RobotViewModel
import ar.edu.uade.robothmi.ui.screens.MainScaffold
import ar.edu.uade.robothmi.ui.screens.RobotSelectionScreen
import ar.edu.uade.robothmi.ui.theme.RobotHmiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RobotHmiTheme {
                RobotHmiApp()
            }
        }
    }
}

@Composable
fun RobotHmiApp(vm: RobotViewModel = viewModel(factory = RobotViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val robot = state.selectedRobot
    if (robot == null) {
        RobotSelectionScreen(
            robots = state.robots,
            serviceName = state.serviceName,
            onSelect = vm::selectRobot,
        )
    } else {
        MainScaffold(vm = vm, state = state, robot = robot)
    }
}
