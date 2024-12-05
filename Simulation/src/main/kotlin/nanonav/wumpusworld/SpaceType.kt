package nanonav.wumpusworld

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks

enum class SpaceType(val blockState: BlockState, val danger: Boolean = false) {
    EMPTY(Blocks.BEDROCK.defaultState),
    HOME(Blocks.DIAMOND_BLOCK.defaultState),
    PIT(Blocks.AIR.defaultState, danger = true),
    WUMPUS(Blocks.REDSTONE_BLOCK.defaultState, danger = true),
    GOLD(Blocks.GOLD_BLOCK.defaultState)
}