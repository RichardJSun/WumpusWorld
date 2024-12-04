package nanonav

import nanonav.Board.SpaceType
import java.awt.Point

class Player(val board: Board) {
    val possible: Array<Array<MutableSet<SpaceType>>> = Array(4) { Array(4) { SpaceType.entries.toMutableSet() } }

    val path = mutableListOf<Point>()
    val visited = mutableSetOf<Point>()

    var location: Point = board.start

    //val signals: Array<Array<Set<Signal>?>> = Array(4) { arrayOfNulls(4) }

    var hasArrow = true
    var foundGold = false
    var index = -1

    init {
        possible[location.y][location.x].clear()
    }

    fun getAction(signals: Set<Signal>): Action {
        val adjacent = board.getValidAdjacent(location)

        if (signals.contains(Signal.GOLD)) {
            foundGold = true
            index = path.size - 1
        } else possible[location.y][location.x].remove(SpaceType.GOLD)

        if (foundGold) {
            return Action(Action.Type.MOVE, path[index--])
        }

        // we have to be safe if we're standing on it
        possible[location.y][location.x].removeIf(SpaceType::danger)

        visited.add(location)

        val hasBreeze = signals.contains(Signal.BREEZE)
        val hasStench = signals.contains(Signal.STENCH)
        val hasDangerSignal = hasBreeze || hasStench

        if (!hasDangerSignal) {
            adjacent.forEach { possible[it.y][it.x].removeIf(SpaceType::danger) }
        } else if (!hasBreeze) {
            adjacent.forEach { possible[it.y][it.x].remove(SpaceType.PIT) }
        }  else if (!hasStench) {
            adjacent.forEach { possible[it.y][it.x].remove(SpaceType.WUMPUS) }
        }

        val wumpus = adjacent.filter { SpaceType.WUMPUS in possible[it.y][it.x] }
        if (wumpus.size == 1) {
            // if there is only one wumpus possible, shoot it
            return Action(Action.Type.SHOOT, wumpus.first())
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
}