package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
class ColonizeMissionMemory(var x: Int, var y: Int, val roomName: String) : MissionMemory<ColonizeMission>() {
    var state: ColonizeMission.State = ColonizeMission.State.CLAIM
    override val missionId: String
        get() = "colonize_$roomName"

    override fun restoreMission(): ColonizeMission =
        ColonizeMission(this)
    override fun isComplete() = state == ColonizeMission.State.DONE
}
