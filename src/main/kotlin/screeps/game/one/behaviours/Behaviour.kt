package screeps.game.one.behaviours

import screeps.api.Creep

interface Behaviour {
    fun run(creep: Creep)
}
