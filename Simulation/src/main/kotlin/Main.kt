package nanonav


fun main() {
    val board = Board()


    /*        board.cells[0][2] = Board.SpaceType.PIT
            board.cells[0][3] = Board.SpaceType.GOLD
            board.cells[1][0] = Board.SpaceType.PIT
            board.cells[1][3] = Board.SpaceType.PIT
            board.cells[2][3] = Board.SpaceType.WUMPUS
            board.cells[3][3] = Board.SpaceType.PIT*/

    /*        board.cells[0][2] = Board.SpaceType.PIT
            board.cells[0][3] = Board.SpaceType.WUMPUS
            board.cells[1][3] = Board.SpaceType.GOLD
            board.cells[3][2] = Board.SpaceType.PIT
            board.cells[3][3] = Board.SpaceType.PIT*/

    board.cells[0][1] = Board.SpaceType.PIT
    board.cells[1][2] = Board.SpaceType.GOLD
    board.cells[0][2] = Board.SpaceType.PIT

    val simulation = Simulation(board)
    simulation.run()
}