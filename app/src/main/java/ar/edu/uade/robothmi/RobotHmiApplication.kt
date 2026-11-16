package ar.edu.uade.robothmi

import android.app.Application
import android.content.Context
import ar.edu.uade.robothmi.data.RobotRepository
import ar.edu.uade.robothmi.data.SimulationControls
import ar.edu.uade.robothmi.data.local.AppDatabase
import ar.edu.uade.robothmi.data.sim.InspeccionPlantaProfile
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

    // Para cambiar de caso de uso, reemplazar InspeccionPlantaProfile por otro ServiceProfile.
    private val simulated = SimulatedRobotRepository(
        dao = database.eventLogDao(),
        profile = InspeccionPlantaProfile(),
    )

    val robotRepository: RobotRepository = simulated
    val simulationControls: SimulationControls = simulated
}
