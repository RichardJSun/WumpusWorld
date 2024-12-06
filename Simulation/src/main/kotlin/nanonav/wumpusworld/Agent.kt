package nanonav.wumpusworld

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.debug.DebugRenderer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class Agent(val sim: Simulation) {
    val possible: Array<Array<MutableSet<SpaceType>>> = Array(4) { Array(4) { SpaceType.entries.filter { it != SpaceType.HOME }.toMutableSet() } }

    val path = ArrayDeque<BlockPos>()
    val visited = mutableSetOf<BlockPos>()
    val targetSteps = ArrayDeque<BlockPos>()

    var location = sim.startLoc
    var direction = sim.startFacing

    var foundGold = false
    var hasArrow = true

    init {
        possible[location.z][location.x].clear()
        possible[location.z][location.x].add(SpaceType.HOME)
    }

    fun getActions(signals: Set<Signal>): Collection<Action> {
        val adjacent = sim.getValidAdjacent(location)

        if (signals.contains(Signal.GOLD)) {
            // FIXME: this signal doesn't trigger if it happens in the middle of an action
            foundGold = true
            targetSteps.clear()
            targetSteps.addAll(pathfind(sim.startLoc))

            val next = targetSteps.removeFirstOrNull()

            if (next != null) {
                // take the first step
                path.add(location)
                return getMoveAction(next).also(::updateState)
            } else error("No path to return to")
        } else possible[location.z][location.x].remove(SpaceType.GOLD)

        // we have to be safe if we're standing on it
        possible[location.z][location.x].removeIf(SpaceType::danger)

        visited.add(location)

        if (targetSteps.isNotEmpty()) {
            val next = targetSteps.removeFirst()
            path.add(location)
            return getMoveAction(next).also(::updateState)
        }

        val hasBreeze = signals.contains(Signal.BREEZE)
        val hasStench = signals.contains(Signal.STENCH)
        val hasDangerSignal = hasBreeze || hasStench

        if (!hasDangerSignal) {
            adjacent.forEach { possible[it.z][it.x].removeIf(SpaceType::danger) }
        } else if (!hasBreeze) {
            adjacent.forEach { possible[it.z][it.x].remove(SpaceType.PIT) }
        }  else if (!hasStench) {
            adjacent.forEach { possible[it.z][it.x].remove(SpaceType.WUMPUS) }
        }

        if (hasStench) {
            // everything not adjacent cannot be the wumpus
            possible.withIndex()
                .flatMap { (y, row) -> row.mapIndexed { x, set -> BlockPos(x, 0, y) to set } }
                .filter { (point, _) -> point !in adjacent }
                .forEach { (_, set) -> set.remove(SpaceType.WUMPUS) }
        }

        if (hasArrow) {
            val possibleWumpusLocs = possible.withIndex()
                .flatMap { (y, row) -> row.mapIndexed { x, set -> BlockPos(x, 0, y) to set } }
                .filter { (_, set) -> SpaceType.WUMPUS in set }
                .map { (point, _) -> point }
            if (possibleWumpusLocs.size == 1 && targetSteps.isEmpty()) {
                // if only 1 is possible, let's shoot it
                val adjWumpus = adjacent.find { it == possibleWumpusLocs.first() }
                if (adjWumpus != null) {
                    // we took a shot which should be right
                    // if it's not, the game already ended
                    possible[adjWumpus.z][adjWumpus.x].clear()
                    possible[adjWumpus.z][adjWumpus.x].add(SpaceType.EMPTY)

                    val moveNeeded = getMoveAction(adjWumpus).toMutableList()
                    // remove the last element, which would take us into the wumpus
                    moveNeeded.removeLastOrNull()
                    moveNeeded.add(Action(Action.Type.SHOOT))

                    return moveNeeded.also(::updateState)
                } else {
                    // if it's not adjacent, we need to pathfind to it
                    targetSteps.clear()
                    targetSteps.addAll(pathfind(possibleWumpusLocs.first(), hugTarget = true))

                    val next = targetSteps.removeFirstOrNull()

                    if (next != null) {
                        // take the first step
                        path.add(location)
                        return getMoveAction(next).also(::updateState)
                    }
                }
            }
        }


        if (!signals.contains(Signal.GLITTER)) {
            adjacent.forEach { possible[it.z][it.x].remove(SpaceType.GOLD) }
        } else {
            // everything not adjacent can't be gold
            possible.withIndex()
                .flatMap { (y, row) -> row.mapIndexed { x, set -> BlockPos(x, 0, y) to set } }
                .filter { (point, _) -> point !in adjacent }
                .forEach { (_, set) -> set.remove(SpaceType.GOLD) }

            val adjGlitters = adjacent.filter { SpaceType.GOLD in possible[it.z][it.x] }
            // if there is only one gold, move to it
            if (adjGlitters.size == 1) {
                path.add(location)
                return getMoveAction(adjGlitters.first()).also(::updateState)
            } else if (adjGlitters.isNotEmpty()) {
                // if there are multiple golds, we need to check safety first
                val safeGlitters = adjGlitters.filter { possible[it.z][it.x].none(SpaceType::danger) }

                // visit the first one, since all will be visited eventually
                if (safeGlitters.isNotEmpty()) {
                    path.add(location)
                    return getMoveAction(safeGlitters.first()).also(::updateState)
                }
            }
        }

        val glitters = possible.withIndex()
            .flatMap { (y, row) -> row.mapIndexed { x, set -> BlockPos(x, 0, y) to set } }
            .filter { (_, set) -> SpaceType.GOLD in set }
            .map { (point, _) -> point }
        if (glitters.isNotEmpty()) {
            // there are gold possibilites
            if (glitters.size == 1) {
                // if there is only one, it must be gold
                possible[glitters.first().z][glitters.first().x].clear()
                possible[glitters.first().z][glitters.first().x].add(SpaceType.GOLD)

                // lets try to generate a path there
                val toGold = pathfind(glitters.first())

                if (toGold.isNotEmpty()) {
                    targetSteps.clear()
                    targetSteps.addAll(toGold)

                    val next = targetSteps.removeFirstOrNull()

                    if (next != null) {
                        // take the first step
                        path.add(location)
                        return getMoveAction(next).also(::updateState)
                    }
                }
            }
        }

        val safeMoves = adjacent.filter { possible[it.z][it.x].none(SpaceType::danger) }
        val moveLoc = safeMoves.find { it !in visited }

        if (moveLoc == null) {
            // no safe move found
            val last = path.removeLast()
            return getMoveAction(last).also(::updateState)
        } else {
            path.add(location)
            return getMoveAction(moveLoc).also(::updateState)
        }
    }

    private fun getMoveAction(pos: BlockPos): Collection<Action> {
        val diff = pos.subtract(location)
        val neededDirection = Direction.fromVector(diff, null) ?: return emptyList()

        if (neededDirection == direction) {
            return listOf(Action(Action.Type.FORWARD))
        } else {
            if (neededDirection == direction.rotateYCounterclockwise()) {
                return listOf(Action(Action.Type.TURN_LEFT), Action(Action.Type.FORWARD))
            } else if (neededDirection == direction.rotateYClockwise()) {
                return listOf(Action(Action.Type.TURN_RIGHT), Action(Action.Type.FORWARD))
            } else {
                return listOf(Action(Action.Type.TURN_RIGHT), Action(Action.Type.TURN_RIGHT), Action(Action.Type.FORWARD))
            }
        }
    }

    private fun updateState(actions: Collection<Action>) {
        for (action in actions) {
            when (action.type) {
                Action.Type.TURN_LEFT -> direction = direction.rotateYCounterclockwise()
                Action.Type.TURN_RIGHT -> direction = direction.rotateYClockwise()
                Action.Type.FORWARD -> location = location.offset(direction)
                Action.Type.SHOOT -> hasArrow = false
            }
        }
    }

    private fun pathfind(to: BlockPos, hugTarget: Boolean = false): Collection<BlockPos> {
        val path = ArrayDeque<BlockPos>()
        val visitedLocs = mutableSetOf<BlockPos>()
        path.add(location)
        visitedLocs.add(location)

        while (path.isNotEmpty()) {
            val current = path.last()
            if (current == to) {
                // we made it!
                break
            }

            val adj = sim.getValidAdjacent(current)

            if (hugTarget && to in adj) {
                // we made it next to the target, so we're done
                break
            }

            val safeAdjacent = adj.filter { possible[it.z][it.x].none(SpaceType::danger) }
            val next = safeAdjacent.find { it !in visitedLocs && it !in path }
            if (next != null) {
                path.add(next)
                visitedLocs.add(next)
            } else {
                // backtrack
                path.removeLast()
            }
        }

        path.removeFirstOrNull() // remove the starting location
        return path;
    }

    fun drawDebug(context: WorldRenderContext) {
        for (z in possible.indices) {
            for (x in possible[z].indices) {
                val pos = BlockPos(x, 0, z).toCenterPos()
                val spaceTypes = possible[z][x]

                val text = spaceTypes.joinToString("") { type ->
                    when (type) {
                        SpaceType.EMPTY -> "E"
                        SpaceType.PIT -> "P"
                        SpaceType.WUMPUS -> "W"
                        SpaceType.GOLD -> "G"
                        SpaceType.HOME -> "H"
                    }
                }

                DebugRenderer.drawString(
                    context.matrixStack()!!,
                    context.consumers()!!,
                    text,
                    pos.x,
                    pos.y,
                    pos.z,
                    0xFFFFFF,
                    0.01f,
                    true,
                    0f,
                    true
                )
            }
        }
    }
}