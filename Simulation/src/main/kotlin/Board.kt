package nanonav

import java.awt.Point
import java.util.EnumSet

class Board {
    val cells: Array<Array<SpaceType>> = Array(4) { Array(4) { SpaceType.EMPTY } }
    val start = Point(0, cells.size-1)

    fun getSignals(location: Point): Set<Signal> {
        val spaces = getValidAdjacent(location).map { cells[it.y][it.x] }
        val signals = EnumSet.noneOf(Signal::class.java)
        spaces.forEach {
            when (it) {
                SpaceType.PIT -> signals.add(Signal.BREEZE)
                SpaceType.WUMPUS -> signals.add(Signal.STENCH)
                SpaceType.GOLD -> signals.add(Signal.GLITTER)
                else -> {}
            }
        }
        return signals
    }

    fun getValidAdjacent(point: Point): Set<Point> {
        val valid = mutableSetOf<Point>()
        if (point.y+1 < cells.size) valid.add(Point(point.x, point.y+1))
        if (point.y-1 >= 0) valid.add(Point(point.x, point.y-1))
        if (point.x+1 < cells[0].size) valid.add(Point(point.x+1, point.y))
        if (point.x-1 >= 0) valid.add(Point(point.x-1, point.y))
        return valid
    }

    enum class SpaceType {
        EMPTY,
        START,
        PIT,
        WUMPUS,
        GOLD
    }
}