package screeps.game.one.spawn

import kotlinx.serialization.Serializable
import screeps.game.one.BodyDefinition
import screeps.game.one.extensions.CreepState

@Serializable
data class SpawnInfo(
    val bodyDefinition: BodyDefinition,
    val spawnOptions: CustomSpawnOptions
)
