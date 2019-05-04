package screeps.game.one.spawn

import kotlinx.serialization.Serializable
import screeps.api.CreepMemory
import screeps.api.options
import screeps.api.structures.SpawnOptions
import screeps.game.one.extensions.*

@Serializable
data class CustomSpawnOptions(
    val state: CreepState = CreepState.IDLE,
    val missionId: String? = null,
    val targetId: String? = null,
    val assignedEnergySource: String? = null
) {
    fun toSpawnOptions(): SpawnOptions = options {
        memory = object : CreepMemory {}.apply { transfer(this) }
    }

    fun transfer(memory: CreepMemory) {
        memory.state = state
        memory.missionId = missionId
        memory.targetId = targetId
        memory.assignedEnergySource = assignedEnergySource
    }
}
