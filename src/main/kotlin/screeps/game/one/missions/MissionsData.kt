package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
data class MissionsData(
    val roomUpgrade: MutableList<RoomUpgradeMissionData> = mutableListOf(),
    val colonize: MutableList<ColonizeMissionData> = mutableListOf()
)
