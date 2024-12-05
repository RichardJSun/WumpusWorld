package nanonav

class Simulation(val board: Board) {
    val player = Player(board)
    var success = false

    fun run() {
        println("Running simulation")
        while (true) {
            val locationSpace = board.cells[player.location.y][player.location.x]
            val signals = board.getSignals(player.location).toMutableSet()

            if (locationSpace == Board.SpaceType.WUMPUS) {
                println("Encountered the Wumpus at ${player.location}!")
                break
            } else if (locationSpace == Board.SpaceType.PIT) {
                println("Fell into a pit at ${player.location}!")
                break
            } else if (locationSpace == Board.SpaceType.GOLD) {
                println("Found the gold at ${player.location}!")
                signals.add(Signal.GOLD)
            } else if (player.foundGold && player.location == board.start) {
                println("Returned to the starting point at ${player.location} with the gold!")
                success = true
                break
            }

            println("At ${player.location}, signals: $signals")
            val action = player.getAction(signals)

            if (action.type == Action.Type.MOVE) {
                println("Moving to ${action.location}")
                player.location = action.location!!
            } else if (action.type == Action.Type.SHOOT) {
                println("Shooting at ${action.location}")
                if (board.cells[action.location!!.y][action.location.x] == Board.SpaceType.WUMPUS) {
                    println("Hit the Wumpus at ${action.location}!")
                    board.cells[action.location.y][action.location.x] = Board.SpaceType.EMPTY
                } else {
                    println("Missed the Wumpus at ${action.location}.")
                }
                player.hasArrow = false
            } else if (action.type == Action.Type.NONE) {
                println("No possible moves left.")
                break
            }

            // Check for failure conditions
            if (player.hasNoMoves()) {
                println("No possible path found.")
                break
            }
        }

        // After the simulation ends, display the grid
        displayGrid()
    }

    private fun displayGrid() {
        if (success) {
            board.printPathGrid(player.path + player.location)
        } else {
            board.printFullGrid()
            println("\nNo possible path found.")
        }
    }
}