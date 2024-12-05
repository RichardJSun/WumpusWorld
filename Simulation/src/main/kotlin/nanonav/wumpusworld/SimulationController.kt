package nanonav.wumpusworld

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object SimulationController {
    var simulation: Simulation? = null
    var simulationJob: Job? = null

    fun startSimulation() {
        if (simulation == null) return
        simulation!!.setup()
        simulationJob = CoroutineScope(Dispatchers.MC).launch {
            var shouldRun = true
            while (shouldRun) {
                delay(1000)
                runCatching {
                    if (simulation!!.step()) shouldRun = false
                }.onFailure {
                    it.printStackTrace()
                    shouldRun = false
                }
            }
        }.also {
            it.invokeOnCompletion {
                stopSimulation()
                MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("Simulation ended"))
            }
        }
    }

    fun stopSimulation() {
        if (simulation == null) return
        simulation?.cleanup()
        simulationJob?.cancel()
        simulation = null
    }

    fun drawSimulationDebug(ctx: WorldRenderContext) {
        simulation?.drawDebug(ctx)
    }
}


val Dispatchers.MC: CoroutineDispatcher
    get() = MinecraftClient.getInstance().asCoroutineDispatcher()