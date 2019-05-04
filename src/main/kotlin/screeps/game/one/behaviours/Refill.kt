package screeps.game.one.behaviours

import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureStorage
import screeps.game.one.*
import screeps.game.one.extensions.*
import screeps.utils.lazyPerTick

object Refill : Behaviour {
    const val MAX_MINER_WORK_PARTS_PER_SOURCE = 5
    const val MAX_CREEP_PER_DROPPED_ENERGY = 1
    private val droppedEnergyByRoom: MutableMap<Room, List<Resource>> = mutableMapOf()
    private val minersByRoom: MutableMap<Room, Array<Creep>> = mutableMapOf()
    private val sourcesWithCreepCounts: MutableMap<String, Int> by lazyPerTick {
        val sources = Context.creeps.assignedEnergySources()
        val m = mutableMapOf<String, Int>()
        for (source in sources) {
            m[source] = Context.creeps.values
                .filter { it.memory.assignedEnergySource == source }
                .sumBy { creep -> creep.body.count { bodyPart -> bodyPart.type == WORK } }
        }
        m
    }

    override fun run(creep: Creep) {
        if (creep.isMiner()) {
            miner(creep)
        } else {
            val canWork = worker(creep)
            miner(creep)
        }
    }

    private fun worker(creep: Creep): Boolean {
        /*
        workers can be assigned to a
        * miner
        * container
        * source
        * in the future... carry

         */
        if (creep.shouldContinueMining()) {
            val source: Identifiable? = creep.getAssignedEnergySource() ?: creep.requestEnergy()

            if (source == null) {
                println("no energy available for worker ${creep.name}")
                return false
            } else {
                creep.memory.assignedEnergySource = source.id
            }

            when (source) {
                is Creep -> creep.refillFrom(source)
                is Source -> run {} //println("my source is Source")
                is Resource -> creep.refillFrom(source)
                is StructureContainer -> creep.refillFrom(source)
                is StructureStorage -> creep.refillFrom(source)
                else -> println("dont know the type of my source")
            }

            return true
        } else {
            creep.memory.state = CreepState.IDLE
            return true
        }
    }

    private val mutableMap = sourcesWithCreepCounts

    private fun Creep.requestEnergy(): Identifiable? {
        val droppedEnergy = droppedEnergyByRoom.getOrPut(room) { room.findDroppedEnergy().sortedBy { it.amount } }

        //find a source that is close and has some free spots
        for (energy in droppedEnergy) {
            if (energy.amount >= carryCapacity && sourcesWithCreepCounts.getOrElse(
                    energy.id,
                    { 0 }) < MAX_CREEP_PER_DROPPED_ENERGY
            ) {
                return energy
            }
        }

        //assign to storage if not a hauler (hauling from storage to storage is a bit useless)
        val storage = room.storage
        if (!this.isHauler() && storage != null && storage.my && storage.store.energy > carryCapacity) {
            return storage
        }

        //assign to a miner
        val miners = minersByRoom.getOrPut(this.room) {
            Context.creeps.filter { it.value.isMiner() && it.value.room.name == this.room.name } //TODO assign to miners in other rooms that serve the same colony
                .values.toTypedArray()
        }
        if (miners.isNotEmpty()) {
            //biggest miner first
            // TODO this could be bad because sourcesWithCreepCounts only updated in the beginning of the tick
            // and many could be assigned to same miner

            if (this.isHauler()) {
                val haulers: Map<String, Creep> by lazyPerTick {
                    Context.creeps.filter { it.value.isHauler() }
                }
                val minerWithoutHauler =
                    miners.filterNot { miner -> haulers.any { hauler -> hauler.value.memory.assignedEnergySource == miner.id } }
                        .firstOrNull()
                if (minerWithoutHauler != null) return minerWithoutHauler
            } else return miners.maxBy { creep ->
                val creepsAssignedToMiner = sourcesWithCreepCounts[creep.id] ?: 0
                val minerOutput = creep.body.count { it.type == WORK } * 2
                minerOutput.toDouble() / (creepsAssignedToMiner + 1)
            }
        }

        val containers = room.findStructures()
            .filter { it.structureType == STRUCTURE_CONTAINER }
            .filter { (it as StructureContainer).store.energy > 0 }
        if (containers.isNotEmpty()) {
            println("assigning creep $name's energysource to a container")
        }
        return findClosest(containers)
    }

    private fun miner(creep: Creep) {
        if (creep.shouldContinueMining()) {
            var assignedSource = creep.memory.assignedEnergySource
            if (assignedSource == null) {
                val energySources = creep.room.findEnergy()
                val source = creep.requestSource(energySources)
                if (source == null) {
                    println("no energy sources available for creep ${creep.name} in ${creep.room}")
                    return
                }
                creep.memory.assignedEnergySource = source.id
                assignedSource = source.id
            }

            val source = Game.getObjectById<Source>(assignedSource)
            if (source == null) {
                creep.memory.assignedEnergySource = null
                return
            }

            val useContainerMining =
                creep.room.controller?.level ?: 0 >= 3 && creep.isBigMiner()
            if (useContainerMining) {
                containerMining(creep, source)
            } else {
                when (creep.harvest(source)) {
                    ERR_NOT_IN_RANGE -> {
                        when (val moveCode = creep.travelTo(source.pos)) {
                            OK, ERR_TIRED -> {
                            }
                            //TODO handle no path
                            else -> println("unexpected code $moveCode when moving $creep to ${source.pos}")
                        }
                    }
                }
            }


        } else {
            creep.memory.state = CreepState.IDLE
            creep.memory.assignedEnergySource = null
        }
    }

    private fun containerMining(creep: Creep, source: Source) {
        val sourceToContainerMaxRange = 3

        //TODO
        /*Container mining:
     If RCL > 3 we can place containers to reduce loss to decay of dropped resources.
     The miner needs to stand exactly on the container and repair it from time to time
     Obviously this is only beneficial if we already have a big miner
    */

        //TODO make sure the computations happen not all the time
        //TODO this assumes the container can be built by workers -> workers must be present
        data class Pos(val x: Int, val y: Int)

        val pathToSource = source.room.findPath(creep.pos, source.pos)

        //figure out where to place the container
        val containertile: Pos = if (pathToSource.size < 2) {
            Pos(creep.pos.x, creep.pos.y)
        } else {
            creep.moveByPath(pathToSource) //mov to location
            val tileBeforeLast = pathToSource[pathToSource.lastIndex]
            Pos(tileBeforeLast.x, tileBeforeLast.y)
        }

        //check if there is already a container for this source
        val containers = source.pos.findInRange<Structure>(FIND_STRUCTURES, sourceToContainerMaxRange)
            .filter { it.structureType == STRUCTURE_CONTAINER }
        when (containers.size) {
            0 -> {
                if (source.room.lookAt(containertile.x, containertile.y)
                        .any { it.type == LOOK_CONSTRUCTION_SITES && it.constructionSite!!.structureType == STRUCTURE_CONTAINER }
                ) {
                    return
                }

                when (source.room.createConstructionSite(
                    containertile.x, containertile.y,
                    STRUCTURE_CONTAINER
                )) {
                    OK -> println("building container for source ${source.id}]")
                    else -> println("error placing construction site for source ${source.id}")
                }

            }
            1 -> {
                val container = containers.single()
                //set target and move to
                if (creep.pos.x != container.pos.x || creep.pos.y != container.pos.y) {
                    creep.travelTo(container.pos) //TODO deal with return
                } else {
                    creep.harvest(source)
                }
            }
            else -> {
                //TODO what?
                println("Error! multiple containers within $sourceToContainerMaxRange range of source ${source.id}")
            }
        }

    }


    private fun Creep.requestSource(energySources: Array<Source>): Source? {

        println("sourcesWithCreepCounts=$sourcesWithCreepCounts")

        //find a source that is close and has some free spots
        energySources.sort { a, b -> (dist2(this.pos, a.pos) - dist2(this.pos, b.pos)) }

        for (energySource in energySources) {
            val bodyPartsUsed = sourcesWithCreepCounts.getOrElse(energySource.id, { 0 })
            if (bodyPartsUsed < MAX_MINER_WORK_PARTS_PER_SOURCE) {
                //assign creep to energy source
                sourcesWithCreepCounts[energySource.id] = bodyPartsUsed + body.count { it.type == WORK }
                return energySource
            }
        }

        return null
    }

    private fun dist2(from: RoomPosition, to: RoomPosition) =
        (to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y)


}
