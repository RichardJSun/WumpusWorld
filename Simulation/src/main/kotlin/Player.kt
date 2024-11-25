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
                potentialPits.addAll(adjacent)
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
            if (potentialGold.isEmpty()) potentialGold.addAll(adjacent)
        } else {
            potentialGold.removeAll(adjacent)
        }

        if (potentialPits.size == 1) {
            // if there is only one potential pit, mark that potential pit as a guaranteed pit
            knownPits.add(potentialPits.first())
            potentialPits.clear()
        }

        visited.add(location)
        path.add(location)

        val moveLoc = adjacent.find {
            it !in visited && (it in safe || it !in potentialWumpus && it !in knownPits && it !in potentialPits)
        }

        if (moveLoc == null) {
            println("Backtracking")
            val last = path.removeAt(path.size - 2)
            return Action(Action.Type.MOVE, last)
        } else {
            return Action(Action.Type.MOVE, moveLoc)
        }
    }
}