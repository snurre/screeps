package screeps.game.one.spawn

import kotlinx.serialization.Serializable
import screeps.game.one.spawn.SpawnInfo

@Serializable
data class CreepSpawnList(val queue: List<SpawnInfo>)
