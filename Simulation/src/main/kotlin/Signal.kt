package nanonav

enum class Signal(val bit: Byte) {
    NOTHING(0),
    BREEZE(1),
    STENCH(2),
    GLITTER(4);
}