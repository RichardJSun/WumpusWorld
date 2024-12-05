package nanonav.wumpusworld

data class Action(val type: Type) {

    enum class Type {
        // make a 90 degree rotation left
        TURN_LEFT,
        // make a 90 degree rotation right
        TURN_RIGHT,
        // move forward 1 step
        FORWARD,
        // use a bucket to pickup lava
        SHOOT
    }
}