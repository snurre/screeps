package screeps.game.one.extensions

import screeps.game.one.BodyDefinition
import screeps.game.one.Context
import screeps.game.one.spawn.CustomSpawnOptions
import screeps.game.one.spawn.GlobalSpawnQueue

fun BodyDefinition.requestCreep(options: CustomSpawnOptions) {
    val candidate =
        Context.creeps.values.firstOrNull { it.memory.state == CreepState.IDLE && it.body.contentEquals(body) }
    if (candidate != null) {
        options.transfer(candidate.memory)
    } else {
        GlobalSpawnQueue.push(this, options)
    }
}

fun BodyDefinition.requestCreepOnce(options: CustomSpawnOptions) {
    if (GlobalSpawnQueue.queue.none { it.spawnOptions == options && it.bodyDefinition == this }) {
        requestCreep(options)
    }
}
