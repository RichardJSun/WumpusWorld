package nanonav

import nanonav.Board.SpaceType
import java.awt.Point
import java.util.LinkedList

class Player(val board: Board) {
    val possible: Array<Array<MutableSet<SpaceType>>> = Array(4) { Array(4) { SpaceType.entries.toMutableSet() } }

    val path = mutableListOf<Point>()
    val visited = mutableSetOf<Point>()
    val targetSteps = mutableListOf<Point>()

    var location: Point = board.start

    //val signals: Array<Array<Set<Signal>?>> = Array(4) { arrayOfNulls(4) }

    var hasArrow = true
    var foundGold = false

    init {
        possible[location.y][location.x].removeIf(SpaceType::danger)
    }

    fun getAction(signals: Set<Signal>): Action {
        val adjacent = board.getValidAdjacent(location)

        if (signals.contains(Signal.GOLD)) {
            foundGold = true
            targetSteps.clear()
            targetSteps.addAll(pathfind(board.start))

            val next = targetSteps.removeFirstOrNull()

            if (next != null) {
                // take the first step
                path.add(location)
                return Action(Action.Type.MOVE, next)
            } else error("No path to return to")
        } else possible[location.y][location.x].remove(SpaceType.GOLD)

        // we have to be safe if we're standing on it
        possible[location.y][location.x].removeIf(SpaceType::danger)

        visited.add(location)

        if (targetSteps.isNotEmpty()) {
            val next = targetSteps.removeFirst()
            path.add(location)
            return Action(Action.Type.MOVE, next)
        }

        val hasBreeze = signals.contains(Signal.BREEZE)
        val hasStench = signals.contains(Signal.STENCH)
        val hasDangerSignal = hasBreeze || hasStench

        if (!hasDangerSignal) {
            adjacent.forEach { possible[it.y][it.x].removeIf(SpaceType::danger) }
        } else if (!hasBreeze) {
            adjacent.forEach { possible[it.y][it.x].remove(SpaceType.PIT) }
        }  else if (!hasStench) {
            adjacent.forEach { possible[it.y][it.x].remove(SpaceType.WUMPUS) }
        } else if (hasStench) {
            // everything not adjacent cannot be the wumpus
            possible.withIndex()
                .flatMap { (y, row) -> row.mapIndexed { x, set -> Point(x, y) to set } }
                .filter { (point, _) -> point !in adjacent }
                .forEach { (_, set) -> set.remove(SpaceType.WUMPUS) }
        }

        val possibleWumpusLocs = possible.withIndex()
            .flatMap { (y, row) -> row.mapIndexed { x, set -> Point(x, y) to set } }
            .filter { (_, set) -> SpaceType.WUMPUS in set }
            .map { (point, _) -> point }
        if (possibleWumpusLocs.size == 1 && targetSteps.isEmpty()) {
            // if only 1 is possible, let's shoot it
            val adjWumpus = adjacent.find { it == possibleWumpusLocs.first() }
            if (adjWumpus != null) {
                // we took a shot which should be right
                // if it's not, the game already ended
                possible[adjWumpus.y][adjWumpus.x].clear()
                possible[adjWumpus.y][adjWumpus.x].add(SpaceType.EMPTY)

                return Action(Action.Type.SHOOT, adjWumpus)
            } else {
                // if it's not adjacent, we need to pathfind to it
                targetSteps.clear()
                targetSteps.addAll(pathfind(possibleWumpusLocs.first()))

                // remove the last step since that is the wumpus location
                targetSteps.removeLastOrNull()

                val next = targetSteps.removeFirstOrNull()

                if (next != null) {
                    // take the first step
                    path.add(location)
                    return Action(Action.Type.MOVE, next)
                }
            }
        }

        if (!signals.contains(Signal.GLITTER)) {
            adjacent.forEach { possible[it.y][it.x].remove(SpaceType.GOLD) }
        } else {
            val glitters = adjacent.filter { SpaceType.GOLD in possible[it.y][it.x] }
            // if there is only one gold, move to it
            if (glitters.size == 1) {
                path.add(location)
                return Action(Action.Type.MOVE, glitters.first())
            } else {
                // if there are multiple golds, we need to check safety first
                val safeGlitters = glitters.filter { possible[it.y][it.x].none(SpaceType::danger) }

                // visit the first one, since all will be visited eventually
                if (safeGlitters.isNotEmpty()) {
                    path.add(location)
                    return Action(Action.Type.MOVE, safeGlitters.first())
                }
            }
        }

        val safeMoves = adjacent.filter { possible[it.y][it.x].none(SpaceType::danger) }
        val moveLoc = safeMoves.find { it !in visited }

        if (moveLoc == null) {
            println("Backtracking")
            val last = path.removeLast()
            return Action(Action.Type.MOVE, last)
        } else {
            path.add(location)
            return Action(Action.Type.MOVE, moveLoc)
        }
    }

    fun pathfind(to: Point): Collection<Point> {
        val path = LinkedList<Point>()
        val visitedLocs = mutableSetOf<Point>()
        path.add(location)
        visitedLocs.add(location)

        while (path.isNotEmpty()) {
            val current = path.last()
            if (current == to) {
                // we made it!
                break
            }

            val safeAdjacent = board.getValidAdjacent(current).filter { possible[it.y][it.x].none(SpaceType::danger) }
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
}