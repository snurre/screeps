package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
class UpgradeMissionMemory(
    val controllerId: String,
    var state: RoomUpgradeMission.State
) :
    MissionMemory<RoomUpgradeMission>() {
    override val missionId: String = "upgrade_$controllerId"
    override fun restoreMission(): RoomUpgradeMission = RoomUpgradeMission(this)
}
