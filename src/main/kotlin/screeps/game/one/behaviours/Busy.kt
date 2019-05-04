package screeps.game.one.behaviours

import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureController
import screeps.game.one.*
import screeps.game.one.extensions.*

object Busy : Behaviour {
    override fun run(creep: Creep) {
        if (creep.carry.energy == 0) {
            creep.memory.state = CreepState.REFILL
            creep.memory.targetId = null
            return
        }

        if (creep.memory.state == CreepState.TRANSFERRING_ENERGY) {
            fun findTarget(): Structure? {
                val targets = creep.room.findStructures()
                    .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
                    .filter { it.unsafeCast<EnergyContainer>().energy < it.unsafeCast<EnergyContainer>().energyCapacity }

                return creep.findClosest(targets)
            }

            val target = if (creep.memory.targetId != null) {
                Game.getObjectById(creep.memory.targetId)
            } else findTarget()

            if (target != null) {
                when (creep.transfer(target, RESOURCE_ENERGY)) {
                    OK -> run { }
                    ERR_NOT_IN_RANGE -> creep.travelTo(target.pos)
                    else -> creep.memory.state = CreepState.IDLE
                }
            } else {
                creep.memory.state = CreepState.IDLE
                creep.memory.targetId = null
            }
        }

        if (creep.memory.state == CreepState.UPGRADING) {
            val controller =
                creep.memory.targetId?.let { Game.getObjectById(it) as? StructureController }
                    ?: creep.room.controller!!
            if (creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                creep.travelTo(controller.pos)
            }
        }

        if (creep.memory.state == CreepState.CONSTRUCTING) {
            val constructionSite = Context.constructionSites[creep.memory.targetId!!]
            if (constructionSite != null) {
                if (creep.build(constructionSite) == ERR_NOT_IN_RANGE) {
                    creep.travelTo(constructionSite.pos)
                }
            } else {
                println("construction of ${creep.memory.targetId} is done")
                creep.memory.targetId = null
                creep.memory.state = CreepState.IDLE
                creep.room.buildRoads()
            }
        }

        if (creep.memory.state == CreepState.REPAIR) {
            require(creep.memory.targetId != null)
            val structure = Game.getObjectById<Structure>(creep.memory.targetId!!)

            fun done() {
                println("finished repairing ${creep.memory.targetId}")
                creep.memory.state = CreepState.IDLE
                creep.memory.targetId = null
            }
            if (structure == null || structure.hits == structure.hitsMax) {
                done()
            } else {
                if (creep.repair(structure) == ERR_NOT_IN_RANGE) {
                    creep.travelTo(structure.pos)
                }
            }
        }
    }
}
