package screeps.game.one.missions

import screeps.api.Creep
import screeps.api.RoomPosition
import screeps.game.one.extensions.travelTo

sealed class ColonizeSubMission {
    abstract fun update()
}

class ClaimMission(private val claimer: Creep, private val position: RoomPosition) : ColonizeSubMission() {
    override fun update() {
        claimer.travelTo(position)
    }
}
