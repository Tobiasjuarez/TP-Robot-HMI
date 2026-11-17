package ar.edu.uade.robothmi

import android.app.Application
import android.content.Context
import ar.edu.uade.robothmi.data.RobotRepository
import ar.edu.uade.robothmi.data.SimulationControls
import ar.edu.uade.robothmi.data.local.AppDatabase
import ar.edu.uade.robothmi.data.sim.GuiaUadeProfile
import ar.edu.uade.robothmi.data.sim.SimulatedRobotRepository

class RobotHmiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Inyección de dependencias manual: un solo lugar donde se arma todo. */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)

    // El caso de uso (guía en UADE) está en GuiaUadeProfile.
    private val simulated = SimulatedRobotRepository(
        dao = database.eventLogDao(),
        profile = GuiaUadeProfile(),
    )

    val robotRepository: RobotRepository = simulated
    val simulationControls: SimulationControls = simulated
}
