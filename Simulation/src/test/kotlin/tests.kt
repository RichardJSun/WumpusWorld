package nanonav

import org.junit.jupiter.api.Timeout
import kotlin.test.Test
import kotlin.test.BeforeTest

object SimulationTest {

    private lateinit var board: Board

    @BeforeTest
    fun setup() {
        board = Board()
    }

    @Timeout(10)
    @Test
    fun `test easy world 1`() {
        // Setup board with configuration:
        // ["e", "e", "e", "g"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "e", "w"]
        board.cells[0][3] = Board.SpaceType.GOLD
        board.cells[3][3] = Board.SpaceType.WUMPUS

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(10)
    @Test
    fun `test easy world 2`() {
        // Setup board with configuration:
        // ["e", "e", "e", "h"],
        // ["e", "g", "e", "e"],
        // ["e", "e", "e", "w"],
        // ["e", "e", "e", "e"]
        board.cells[0][3] = Board.SpaceType.PIT
        board.cells[1][1] = Board.SpaceType.GOLD
        board.cells[2][3] = Board.SpaceType.WUMPUS

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(10)
    @Test
    fun `test easy world 3`() {
        // Setup board with configuration:
        // ["h", "h", "e", "e"],
        // ["e", "e", "e", "w"],
        // ["e", "e", "g", "e"],
        // ["e", "e", "e", "e"]
        board.cells[0][0] = Board.SpaceType.PIT
        board.cells[0][1] = Board.SpaceType.PIT
        board.cells[1][3] = Board.SpaceType.WUMPUS
        board.cells[2][2] = Board.SpaceType.GOLD

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(30)
    @Test
    fun `test medium world 1`() {
        // Setup board with configuration:
        // ["h", "h", "g", "h"],
        // ["h", "e", "e", "e"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "e", "w"]
        board.cells[0][0] = Board.SpaceType.PIT
        board.cells[0][1] = Board.SpaceType.PIT
        board.cells[0][2] = Board.SpaceType.GOLD
        board.cells[0][3] = Board.SpaceType.PIT
        board.cells[1][0] = Board.SpaceType.PIT
        board.cells[3][3] = Board.SpaceType.WUMPUS

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(30)
    @Test
    fun `test medium world 2`() {
        // Setup board with configuration:
        // ["e", "e", "h", "w"],
        // ["e", "e", "e", "g"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "h", "h"]
        board.cells[0][2] = Board.SpaceType.PIT
        board.cells[0][3] = Board.SpaceType.WUMPUS
        board.cells[1][3] = Board.SpaceType.GOLD
        board.cells[3][2] = Board.SpaceType.PIT
        board.cells[3][3] = Board.SpaceType.PIT

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(30)
    @Test
    fun `test medium world 3`() {
        // Setup board with configuration:
        // ["w", "e", "g", "e"],
        // ["e", "h", "e", "e"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "e", "h"]
        board.cells[0][0] = Board.SpaceType.WUMPUS
        board.cells[0][2] = Board.SpaceType.GOLD
        board.cells[1][1] = Board.SpaceType.PIT
        board.cells[3][3] = Board.SpaceType.PIT

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(60)
    @Test
    fun `test hard world 1`() {
        // Setup board with configuration:
        // ["h", "h", "g", "e"],
        // ["h", "h", "e", "e"],
        // ["e", "h", "e", "e"],
        // ["e", "e", "w", "e"]
        board.cells[0][0] = Board.SpaceType.PIT
        board.cells[0][1] = Board.SpaceType.PIT
        board.cells[0][2] = Board.SpaceType.GOLD
        board.cells[1][0] = Board.SpaceType.PIT
        board.cells[1][1] = Board.SpaceType.PIT
        board.cells[2][1] = Board.SpaceType.PIT
        board.cells[3][2] = Board.SpaceType.WUMPUS

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }

    @Timeout(120)
    @Test
    fun `test hardest world`() {
        // not needed to solve this BOARD, would have to assume it's solvable

        // Setup board with configuration:
        // ["e", "e", "h", "g"],
        // ["e", "e", "h", "w"],
        // ["e", "e", "e", "e"],
        // ["e", "e", "h", "h"]
        board.cells[0][2] = Board.SpaceType.PIT
        board.cells[0][3] = Board.SpaceType.GOLD
        board.cells[1][2] = Board.SpaceType.PIT
        board.cells[1][3] = Board.SpaceType.WUMPUS
        board.cells[3][2] = Board.SpaceType.PIT
        board.cells[3][3] = Board.SpaceType.PIT

        val simulation = Simulation(board)
        simulation.run()
        assert(simulation.solved)
    }
}