package screeps.game.one.extensions

import screeps.game.one.spawn.SpawnInfo
import screeps.game.one.BodyDefinition

fun SpawnInfo.isHauler() = this.bodyDefinition == BodyDefinition.HAULER
fun SpawnInfo.isMiner() = this.bodyDefinition.name.startsWith(BodyDefinition.MINER.name)
fun SpawnInfo.isWorker() = this.bodyDefinition == BodyDefinition.WORKER
