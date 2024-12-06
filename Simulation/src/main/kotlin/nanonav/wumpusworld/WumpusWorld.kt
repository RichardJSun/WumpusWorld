package nanonav.wumpusworld

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resource.ResourceType
import org.slf4j.LoggerFactory

object WumpusWorld : ModInitializer {
    private val logger = LoggerFactory.getLogger("wumpusworld")

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("Hello Fabric world!")
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				literal("wumpusworld")
					.then(
						literal("build")
							.then(
								argument("name", StringArgumentType.greedyString()).suggests(
									WumpusWorldCommand.BoardNameSuggestionProvider
								).executes(WumpusWorldCommand::build)
							)
					)
					.then(
						literal("simulate").executes(WumpusWorldCommand::simulate).then(
							literal("stop").executes(WumpusWorldCommand::stopSimulation)
						)
					)
			)
		}
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
			.registerReloadListener(WumpusWorldCommand.ResourceListener)

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register { context ->
			SimulationController.drawSimulationDebug(context)
		}

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { mc, world ->
			SimulationController.stopSimulation()
		}
	}
}