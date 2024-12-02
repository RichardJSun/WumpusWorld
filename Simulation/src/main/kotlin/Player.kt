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

    //val signals: Array<Array<Set<Signal>?>> = Array(4) { arrayOfNulls(4) }

    var hasArrow = true
    var foundGold = false
    var index = -1

    init {
        safe.add(location)
    }

    fun getAction(signals: Set<Signal>): Action {
        val adjacent = board.getValidAdjacent(location)

        if (signals.contains(Signal.GOLD)) {
            foundGold = true
            index = path.size - 1
        }

        if (foundGold) {
            return Action(Action.Type.MOVE, path[index--])
        }

        val hasBreeze = signals.contains(Signal.BREEZE)
        val hasStench = signals.contains(Signal.STENCH)
        val hasDangerSignal = hasBreeze || hasStench

        if (hasDangerSignal) {
            if (hasBreeze) {
                potentialPits.addAll(adjacent.filterNot { it in safe })
            }

            if (hasStench) {
                if (potentialWumpus.isEmpty()) {
                    potentialWumpus.addAll(adjacent.filterNot { safe.contains(it) })
                } else if (potentialWumpus.size == 1) {
                    safe.addAll(adjacent.filter { it != potentialWumpus.first() })
                    if (hasArrow) {
                        return Action(Action.Type.SHOOT, potentialWumpus.first())
                    }
                }
            } 

            if (!hasStench) {
                potentialWumpus.removeAll(adjacent)
            }

            if (!hasBreeze) {
                potentialPits.removeAll(adjacent)
            }
        } else {
            safe.addAll(adjacent)
        }

        if (signals.contains(Signal.GLITTER)) {
            if (potentialGold.isEmpty()) potentialGold.addAll(adjacent.filterNot { it in visited || it in knownPits })
        } else {
            potentialGold.removeAll(adjacent)
        }

        if (potentialPits.size == 1) {
            // if there is only one potential pit, mark that potential pit as a guaranteed pit
            knownPits.add(potentialPits.first())
            potentialPits.clear()
        }

        visited.add(location)
        // if we visited something it is safe
        safe.add(location)

        if (potentialGold.isNotEmpty()) {
            if (potentialGold.size == 1) {
                // only 1 potential spot
                adjacent.find { it in potentialGold }?.let {
                    path.add(location)
                    return Action(Action.Type.MOVE, it)
                }
            }

            val adjacentGlitter = adjacent.filter { it in potentialGold }
            if (adjacentGlitter.isNotEmpty()) {
                println("adj ${adjacentGlitter}")
            }
            val safeAdjacentToGlitter = adjacentGlitter.filter { pos ->
                pos !in visited &&
                (pos in safe ||
                (pos !in knownPits &&
                        pos !in potentialPits &&
                        pos !in potentialWumpus &&
                        pos !in visited))
            }
            println("safe adj ${safeAdjacentToGlitter}")
            if (safeAdjacentToGlitter.isNotEmpty()) {
                path.add(location)
                return Action(Action.Type.MOVE, safeAdjacentToGlitter.first())
            }
        }

        val moveLoc = adjacent.find {
            it !in visited && (it in safe || it !in potentialWumpus && it !in knownPits && it !in potentialPits)
        }

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