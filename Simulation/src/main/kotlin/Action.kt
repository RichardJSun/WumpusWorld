package nanonav

import java.awt.Point

data class Action(val type: Type, val location: Point) {

    enum class Type {
        MOVE,
        SHOOT
    }
}