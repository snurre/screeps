package screeps.game.one.spawn

import kotlinx.serialization.Serializable

@Serializable
data class SpawnQueueData(val queue: MutableList<SpawnInfo> = mutableListOf())
