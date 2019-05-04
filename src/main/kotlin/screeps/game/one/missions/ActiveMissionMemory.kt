package screeps.game.one.missions

import kotlinx.serialization.Serializable

@Serializable
data class ActiveMissionMemory(
    /*
    Rightnow there is no polymorphic serializer for kotlin-js so we have to resort to this
     */
    val upgradeMissions: MutableList<UpgradeMissionMemory> = mutableListOf(),
    val colonizeMissions: MutableList<ColonizeMissionMemory> = mutableListOf()
)
