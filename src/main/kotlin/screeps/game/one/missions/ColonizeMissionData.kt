package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
class ColonizeMissionData(
    var x: Int, var y: Int, val roomName: String,
    var state: ColonizeMission.State = ColonizeMission.State.CLAIM
) : MissionData<ColonizeMission>() {
    override val missionId: String = "colonize_$roomName"
    override fun restoreMission(): ColonizeMission = ColonizeMission(this)
    override fun isComplete() = state == ColonizeMission.State.DONE
}
