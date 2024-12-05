package nanonav

import java.awt.Point
import java.io.File
import java.io.FileNotFoundException
import java.util.EnumSet

class Board {
    val cells: Array<Array<SpaceType>>
    val start: Point

    // Constructor to load board from a file
    constructor(filename: String) {
        val result = loadFromFile(filename)
        cells = result.first
        start = result.second
    }

    private fun loadFromFile(filename: String): Pair<Array<Array<SpaceType>>, Point> {
        val file = File(filename)
        if (!file.exists()) {
            throw FileNotFoundException("The file $filename does not exist.")
        }

        val lines = file.readLines().filter { it.isNotBlank() }
        val boardSize = lines.size
        val tempCells = Array(boardSize) { Array<SpaceType?>(boardSize) { null } }
        var startPoint: Point? = null

        for ((rowIndex, line) in lines.withIndex()) {
            val tokens = line.trim().split("\\s+".toRegex())
            if (tokens.size != boardSize) {
                throw IllegalArgumentException("Incorrect number of columns in row ${rowIndex + 1}. Expected $boardSize but got ${tokens.size}.")
            }
            for ((colIndex, token) in tokens.withIndex()) {
                tempCells[rowIndex][colIndex] = when (token) {
                    "E" -> SpaceType.EMPTY
                    "P" -> SpaceType.PIT
                    "G" -> SpaceType.GOLD
                    "W" -> SpaceType.WUMPUS
                    "S" -> {
                        startPoint = Point(colIndex, rowIndex)
                        SpaceType.EMPTY
                    }
                    else -> throw IllegalArgumentException("Invalid token '$token' at row ${rowIndex + 1}, column ${colIndex + 1}.")
                }
            }
        }

        // Check if any null values remain in the tempCells
        for (row in tempCells) {
            if (row.any { it == null }) {
                throw IllegalStateException("Board initialization failed due to null values.")
            }
        }

        // Set default start position if not specified
        if (startPoint == null) {
            startPoint = Point(0, boardSize - 1)
        }

        // Cast tempCells to non-nullable type
        val finalizedCells = Array(boardSize) { rowIndex ->
            Array(boardSize) { colIndex ->
                tempCells[rowIndex][colIndex]!!
            }
        }

        println("Board configuration loaded successfully from '$filename'.")
        return Pair(finalizedCells, startPoint)
    }

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
        if (point.y + 1 < cells.size) valid.add(Point(point.x, point.y + 1))
        if (point.y - 1 >= 0) valid.add(Point(point.x, point.y - 1))
        if (point.x + 1 < cells[0].size) valid.add(Point(point.x + 1, point.y))
        if (point.x - 1 >= 0) valid.add(Point(point.x - 1, point.y))
        return valid
    }

    fun printPathGrid(path: List<Point>) {
        println("\nPlayer's Path:")
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                val point = Point(x, y)
                val tileContent = when {
                    point == start -> "S"
                    point in path -> {
                        val content = when (cells[y][x]) {
                            SpaceType.EMPTY -> "."
                            SpaceType.PIT -> "P"
                            SpaceType.WUMPUS -> "W"
                            SpaceType.GOLD -> "G"
                        }
                        "*$content*"
                    }
                    else -> "   "
                }
                print("[${tileContent}]")
            }
            println()
        }
    }

    fun printFullGrid() {
        println("\nFull Grid:")
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                val tileContent = when (cells[y][x]) {
                    SpaceType.EMPTY -> "E"
                    SpaceType.PIT -> "P"
                    SpaceType.WUMPUS -> "W"
                    SpaceType.GOLD -> "G"
                }
                print("[$tileContent]")
            }
            println()
        }
    }

    enum class SpaceType(val danger: Boolean = false) {
        EMPTY,
        PIT(danger = true),
        WUMPUS(danger = true),
        GOLD
    }
}