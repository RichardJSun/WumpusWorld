package nanonav.wumpusworld

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.text.Text
import net.minecraft.util.BlockRotation
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import java.util.EnumSet

class Simulation(private val world: ClientWorld) {
    val topLeft = BlockPos(0, 0, 0)
    val size = 4
    val startLoc = topLeft.add(0, 0, size - 1)
    val startFacing = Direction.NORTH
    private val agent = Agent(this)
    private var arrowCount = 1
    private var goldCollected = false
    private var entity: LivingEntity? = null

    fun placeBoard(board: Array<Array<SpaceType>>) {
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val pos = topLeft.add(x, 0, y)
                val spaceType = board[y][x]
                val blockState = spaceType.blockState
                world.setBlockState(pos, blockState)
            }
        }

        world.setBlockState(startLoc, Blocks.DIAMOND_BLOCK.defaultState)
    }

    fun setup() {
        if (entity == null) {
            entity = EntityType.CAT.create(world, SpawnReason.COMMAND) ?: error("Failed to create entity")
            world.addEntity(entity)
        }
        entity!!.refreshPositionAndAngles(startLoc.up(), 180f, 0f)
        entity!!.snapToYaw(180f)
        world.getEntitiesByType(
            TypeFilter.instanceOf(ArrowEntity::class.java),
            Box.enclosing(topLeft, topLeft.add(size, size, size))
        ) { true }.forEach {
            it.remove(Entity.RemovalReason.DISCARDED)
        }
    }

    fun cleanup() {
        entity?.remove(Entity.RemovalReason.DISCARDED)
        entity = null
    }

    fun step(): Boolean {
        val signals = getSignals(entity!!.blockPos.down()) as MutableSet
        val locationSpace = SpaceType.entries.first { it.blockState == world.getBlockState(entity!!.blockPos.down()) }
        if (locationSpace == SpaceType.WUMPUS) {
            error("Wumpus")
        } else if (locationSpace == SpaceType.PIT) {
            error("Pit")
        } else if (locationSpace == SpaceType.GOLD) {
            goldCollected = true
            world.setBlockState(entity!!.blockPos.down(), SpaceType.EMPTY.blockState)
            signals.add(Signal.GOLD)
        } else if (goldCollected && entity!!.blockPos.down() == startLoc) {
            // solved
            return true
        }
        val actions = agent.getActions(signals)
        MinecraftClient.getInstance().inGameHud.chatHud.addMessage(Text.of("Signals: $signals, Actions: $actions"))
        for (action in actions) {
            when (action.type) {
                Action.Type.TURN_LEFT -> entity!!.snapToYaw(entity!!.applyRotation(BlockRotation.COUNTERCLOCKWISE_90))
                Action.Type.TURN_RIGHT -> entity!!.snapToYaw(entity!!.applyRotation(BlockRotation.CLOCKWISE_90))
                Action.Type.FORWARD -> {
                    val newPos = entity!!.blockPos.offset(entity!!.horizontalFacing)
                    entity!!.setPosition(newPos.toBottomCenterPos())
                }
                Action.Type.SHOOT -> {
                    if (arrowCount-- == 0) {
                        error("No arrows")
                    }
                    val shootPos = entity!!.blockPos.down().offset(entity!!.horizontalFacing)

                    val arrow = EntityType.ARROW.create(world, SpawnReason.COMMAND) ?: error("Failed to create arrow")
                    arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED
                    arrow.setPosition(shootPos.up(10).toCenterPos())
                    arrow.setVelocityClient(0.0, -1.0, 0.0)
                    world.addEntity(arrow)

                    if (world.getBlockState(shootPos).block == SpaceType.WUMPUS.blockState.block) {
                        world.setBlockState(shootPos, SpaceType.EMPTY.blockState)
                    }
                }
            }
            val locationSpace = SpaceType.entries.first { it.blockState == world.getBlockState(entity!!.blockPos.down()) }
            if (locationSpace == SpaceType.WUMPUS) {
                error("Wumpus")
            } else if (locationSpace == SpaceType.PIT) {
                error("Pit")
            } else if (locationSpace == SpaceType.GOLD) {
                // goldCollected = true
                // world.setBlockState(entity!!.blockPos.down(), SpaceType.EMPTY.blockState)
                // signals.add(Signal.GOLD) // unused
            } else if (goldCollected && entity!!.blockPos.down() == startLoc) {
                // solved
                return true
            }
        }
        return false
    }

    private fun getSignals(location: BlockPos): Set<Signal> {
        val spaces = getValidAdjacent(location).map { world.getBlockState(it) }.map { SpaceType.entries.first { s -> s.blockState == it } }
        val signals = EnumSet.noneOf(Signal::class.java)
        spaces.forEach {
            signals.add(it.signal ?: Signal.NOTHING)
        }
        return signals
    }

    fun getValidAdjacent(pos: BlockPos): Set<BlockPos> {
        return Direction.Type.HORIZONTAL.map { pos.offset(it) }.filterTo(mutableSetOf()) { it.x >= topLeft.x && it.x < topLeft.x + size && it.z >= topLeft.z && it.z < topLeft.z + size }
    }

    fun drawDebug(ctx: WorldRenderContext) {
        agent.drawDebug(ctx)
    }

    private fun LivingEntity.snapToYaw(yaw: Float) {
        this.yaw = yaw
        this.headYaw = yaw
        this.bodyYaw = yaw
        this.prevYaw = yaw
        this.prevHeadYaw = yaw
        this.prevBodyYaw = yaw
    }
}