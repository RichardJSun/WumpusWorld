package nanonav

import java.awt.Point

class Player(val board: Board) {
    val safe = mutableSetOf<Point>()
    val potentialPits = mutableSetOf<Point>()
    val knownPits = mutableSetOf<Point>()
    val potentialWumpus = mutableSetOf<Point>()
    val potentialGold = mutableSetOf<Point>()
    val visited = mutableSetOf<Point>()
    val path = mutableListOf<Point>()

    var location: Point = board.start

    var hasArrow = true
    var foundGold = false

    init {
        safe.add(location)
    }

    fun getAction(signals: Set<Signal>): Action {
        val adjacent = board.getValidAdjacent(location)

        if (signals.contains(Signal.GOLD)) {
            foundGold = true
        }

        if (foundGold && location == board.start) {
            // Already at the starting point after finding gold
            return Action(Action.Type.NONE, null)
        }

        if (foundGold) {
            // Backtracking to start
            if (path.isNotEmpty()) {
                val last = path.removeAt(path.lastIndex)
                return Action(Action.Type.MOVE, last)
            } else {
                // No path to backtrack
                return Action(Action.Type.NONE, null)
            }
        }

        val hasBreeze = signals.contains(Signal.BREEZE)
        val hasStench = signals.contains(Signal.STENCH)

        if (hasBreeze) {
            potentialPits.addAll(adjacent.filterNot { it in safe || it in knownPits })
        } else {
            safe.addAll(adjacent)
            potentialPits.removeAll(adjacent)
        }

        if (hasStench) {
            potentialWumpus.addAll(adjacent.filterNot { it in safe })
        } else {
            safe.addAll(adjacent)
            potentialWumpus.removeAll(adjacent)
        }

        if (potentialPits.size == 1) {
            knownPits.addAll(potentialPits)
            potentialPits.clear()
        }

        visited.add(location)
        safe.add(location)

        // Decide next move
        val moveLoc = adjacent.find {
            it !in visited && it in safe && it !in knownPits && it !in potentialWumpus
        }

        if (moveLoc == null) {
            // No safe unvisited adjacent cells, backtrack if possible
            if (path.isNotEmpty()) {
                val last = path.removeAt(path.lastIndex)
                return Action(Action.Type.MOVE, last)
            } else {
                // No moves left
                return Action(Action.Type.NONE, null)
            }
        } else {
            path.add(location)
            return Action(Action.Type.MOVE, moveLoc)
        }
    }

    fun hasNoMoves(): Boolean {
        val adjacent = board.getValidAdjacent(location)
        val unvisited = adjacent.filterNot { it in visited || it in knownPits || it in potentialPits || it in potentialWumpus }
        return unvisited.isEmpty() && path.isEmpty()
    }
}