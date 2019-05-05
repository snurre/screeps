import screeps.game.one.GameLoop


private class Traveler {
    companion object {
        init {
            js("var Traveler = require('Traveler');")
        }
    }
}

@Suppress("unused")
fun loop() {
    Traveler()
    GameLoop.tick()
}
