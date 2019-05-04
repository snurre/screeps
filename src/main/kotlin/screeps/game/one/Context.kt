package screeps.game.one

import screeps.api.*
import screeps.api.structures.*
import screeps.game.one.extensions.*
import screeps.utils.lazyPerTick
import screeps.utils.toMap

object Context {
    val constructionSites: Map<String, ConstructionSite> by lazyPerTick { Game.constructionSites.toMap() }
    val creeps: Map<String, Creep> by lazyPerTick { Game.creeps.toMap() }
    val haulerCount: Int by lazyPerTick { creeps.count { it.value.isHauler() } }
    val minerCount: Int by lazyPerTick { creeps.count { it.value.isMiner() } }
    val rooms: Map<String, Room> = Game.rooms.toMap()
    val structures: Map<String, Structure> by lazyPerTick { Game.structures.toMap() }
    val targets: Map<String, Creep> by lazyPerTick {
        creeps.filter { it.value.memory.targetId != null }.mapKeys { (_, creep) -> creep.memory.targetId!! }
    }
    val towers: List<StructureTower> by lazyPerTick { structures.values.filter { it.structureType == STRUCTURE_TOWER } as List<StructureTower> }
    val workerCount: Int by lazyPerTick { creeps.count { it.value.isWorker() } }
}
