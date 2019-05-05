package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
class RoomUpgradeMissionData(
    val controllerId: String,
    var state: RoomUpgradeMission.State
) : MissionData<RoomUpgradeMission>() {
    override val missionId: String = "upgrade_$controllerId"
    override fun restoreMission(): RoomUpgradeMission = RoomUpgradeMission(this)
}
