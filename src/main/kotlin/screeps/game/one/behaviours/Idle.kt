package screeps.game.one.behaviours

import screeps.api.Creep
import screeps.api.Game
import screeps.api.Room
import screeps.api.structures.Structure
import screeps.api.structures.StructureRoad
import screeps.api.structures.StructureSpawn
import screeps.api.values
import screeps.game.one.BodyDefinition
import screeps.game.one.Context
import screeps.game.one.extensions.*

object Idle : Behaviour {
    var structureThatNeedRepairingIndex = 0
    private val structureThatNeedRepairing: List<Structure>
        get () {
            val room = Game.rooms.values.firstOrNull { it.storage != null }

            return room?.findStructures().orEmpty().filterNot { Context.targets.containsKey(it.id) }
                .filter { it.hits < it.hitsMax / 2 && it.hits < 2_000_000 }
                .sortedBy { it.hits }
                .take(5) //TODO only repairing 5 is arbitrary
        }

    override fun run(creep: Creep) {
        if (creep.memory.missionId != null) return // we do not care about creeps on a mission
        creep.memory.targetId = null //just making sure it is reset
        val constructionSite = creep.findClosest(creep.room.findMyConstructionSites())
        val controller = creep.room.controller
        val towersInNeedOfRefill = Context.towers.filter { it.room == creep.room && it.energy < it.energyCapacity }
        when {
            //make sure spawn does not dry up
            notEnoughtSpawnEnergy(creep.room) -> {
                creep.memory.state = CreepState.TRANSFERRING_ENERGY
            }

            //make sure towe does not dry up
            towersInNeedOfRefill.isNotEmpty() -> {
                creep.memory.state = CreepState.TRANSFERRING_ENERGY
                creep.memory.targetId = towersInNeedOfRefill.first().id
            }

            creep.isHauler() && creep.room.storage != null -> {
                creep.memory.state = CreepState.TRANSFERRING_ENERGY
                creep.memory.targetId = creep.room.storage!!.id
            }

            //check if we need to construct something
            constructionSite != null -> {
                creep.memory.state = CreepState.CONSTRUCTING
                creep.memory.targetId = constructionSite.id
            }
            //check if we need to upgrade the controller
            controller != null && controller.level < 8 && Context.creeps.none { it.value.memory.state == CreepState.UPGRADING } -> {
                creep.memory.state = CreepState.UPGRADING
                creep.memory.targetId = controller.id
            }
            structureThatNeedRepairing.isNotEmpty() && structureThatNeedRepairingIndex < structureThatNeedRepairing.size -> {
                val structure = structureThatNeedRepairing[structureThatNeedRepairingIndex++]
                creep.memory.state = CreepState.REPAIR
                creep.memory.targetId = structure.id
                println("repairing ${structure.structureType} (${structure.id})")
            }

            controller?.level == 8 && controller.ticksToDowngrade < 10_000 && Context.creeps.none { it.value.memory.state == CreepState.UPGRADING } -> {
                creep.memory.state = CreepState.UPGRADING
                creep.memory.targetId = controller.id
            }

            creep.room.energyAvailable < creep.room.energyCapacityAvailable -> {
                creep.memory.state = CreepState.TRANSFERRING_ENERGY

            }
            //if still idle upgrade controller
            controller != null && controller.level < 8 -> {
                creep.memory.state = CreepState.UPGRADING
                creep.memory.targetId = controller.id
            }
            else -> { //get out of the way
                if (creep.pos.look().any { it.structure is StructureRoad }) {
                    creep.moveInRandomDirection()
                }
            }
        }
    }

    private fun notEnoughtSpawnEnergy(room: Room) =
        room.energyAvailable < BodyDefinition.WORKER.cost
                // or at least 2/3 of energy available
                || room.energyCapacityAvailable > BodyDefinition.WORKER.cost
                && room.energyAvailable < room.energyCapacityAvailable * 2.0 / 3.0
}
