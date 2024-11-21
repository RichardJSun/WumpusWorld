package nanonav

class Simulation {
    val board = Board()
    val player = Player(board)

    fun run() {
        println("Running simulation")
        while (true) {
            val locationSpace = board.cells[player.location.y][player.location.x]
            if (locationSpace == Board.SpaceType.WUMPUS) {
                println("Wumpus")
                break
            } else if (locationSpace == Board.SpaceType.PIT) {
                println("Fell in a pit")
                break
            }
            val signals = board.getSignals(player.location)
            println("At ${player.location}, signals: $signals")
            val move = player.getMove(signals)
            println("Moving to $move")
            player.location.location = move
        }
    }
}