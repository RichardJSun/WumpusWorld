package nanonav

class Simulation(val board: Board) {
    val player = Player(board)
    var solved = false

    fun run() {
        println("Running simulation")
        while (true) {
            val locationSpace = board.cells[player.location.y][player.location.x]
            val signals = board.getSignals(player.location) as MutableSet
            if (locationSpace == Board.SpaceType.WUMPUS) {
                println("Wumpus")
                break
            } else if (locationSpace == Board.SpaceType.PIT) {
                println("Fell in a pit")
                break
            } else if (locationSpace == Board.SpaceType.GOLD) {
                println("Found the gold")
                signals.add(Signal.GOLD)
            } else if (player.foundGold && player.location == board.start) {
                println("Returned to start")
                solved = true
                break
            }
            println("At ${player.location}, signals: $signals")
            val action = player.getAction(signals)
            if (action.type == Action.Type.MOVE) {
                println("Moving to ${action.location}")
                player.location = action.location
            } else if (action.type == Action.Type.SHOOT) {
                println("Shooting at ${action.location}")
                if (board.cells[action.location.y][action.location.x] == Board.SpaceType.WUMPUS) {
                    println("Hit the Wumpus")
                    board.cells[action.location.y][action.location.x] = Board.SpaceType.EMPTY
                } else {
                    println("Missed the Wumpus")
                }
                player.hasArrow = false
            }
        }
    }
}