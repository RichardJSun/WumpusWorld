package nanonav.wumpusworld

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture
import kotlin.collections.iterator

object WumpusWorldCommand {
    private val boards = mutableMapOf<String, Array<Array<SpaceType>>>()

    fun build(context: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(context, "name")
        val board = boards[name] ?: return 0
        val world = context.source.world

        SimulationController.stopSimulation()

        SimulationController.simulation = Simulation(world, context.source.player)

        SimulationController.simulation!!.placeBoard(board)

        context.source.sendFeedback(Text.of("Wumpus World '$name' built successfully"))
        return 1
    }

    fun simulate(context: CommandContext<FabricClientCommandSource>): Int {
        if (SimulationController.simulation == null) {
            // Assume we made a custom board
            SimulationController.simulation = Simulation(context.source.world, context.source.player)
        }
        SimulationController.simulation!!.setup()
        SimulationController.startSimulation()
        context.source.sendFeedback(Text.of("Simulation started"))
        return 1
    }

    fun stopSimulation(context: CommandContext<FabricClientCommandSource>): Int {
        SimulationController.stopSimulation()
        context.source.sendFeedback(Text.of("Simulation stopped"))
        return 1
    }


    object ResourceListener : SimpleSynchronousResourceReloadListener {
        override fun getFabricId(): Identifier = Identifier.of("wumpusworld")

        override fun reload(manager: ResourceManager) {
            boards.clear()
            for (resource in manager.findResources("boards") { it.path.endsWith(".txt") }) {
                val board = resource.value.reader.use {
                    it.readLines().map {
                        it.toCharArray().map {
                            SpaceType.entries.first { type -> type.name.first() == it }
                        }.toTypedArray()
                    }.toTypedArray()
                }
                val name = resource.key.path.substringAfterLast("/").substringBeforeLast(".")
                boards[name] = board
            }
        }
    }

    object BoardNameSuggestionProvider : SuggestionProvider<FabricClientCommandSource> {
        override fun getSuggestions(
            ctx: CommandContext<FabricClientCommandSource>,
            suggestionsBuilder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            for (name in boards.keys) {
                suggestionsBuilder.suggest(name)
            }
            return suggestionsBuilder.buildFuture()
        }

    }
}