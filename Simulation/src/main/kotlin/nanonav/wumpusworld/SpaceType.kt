package nanonav.wumpusworld

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks

enum class SpaceType(val blockState: BlockState, val signal: Signal?, val danger: Boolean = false) {
    EMPTY(Blocks.BEDROCK.defaultState, null),
    HOME(Blocks.DIAMOND_BLOCK.defaultState, null),
    PIT(Blocks.AIR.defaultState, Signal.BREEZE, danger = true),
    WUMPUS(Blocks.REDSTONE_BLOCK.defaultState, Signal.STENCH, danger = true),
    GOLD(Blocks.GOLD_BLOCK.defaultState, Signal.GLITTER)
}