package nanonav

import java.awt.Point

class Player(val board: Board) {
    val safe = mutableSetOf<Point>()
    val potentialPits = mutableSetOf<Point>()
    val knownPits = mutableSetOf<Point>()
    val potentialWumpus = mutableSetOf<Point>()
    val potentialGold = mutableSetOf<Point>()
    val visited = mutableSetOf<Point>()

    val location: Point = board.start
    val targets = mutableSetOf<Point>()

    val signals: Array<Array<Set<Signal>?>> = Array(4) { arrayOfNulls(4) }

    var hasArrow = true

    init {
        safe.add(location)
    }

    fun getMove(signals: Set<Signal>): Point {
        val adjacent = board.getValidAdjacent(location)

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
    }
}