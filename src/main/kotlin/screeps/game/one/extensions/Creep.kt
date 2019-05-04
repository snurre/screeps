package screeps.game.one.extensions

import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.game.one.BodyDefinition
import screeps.game.one.behaviours.Behaviour
import screeps.game.one.behaviours.Busy
import screeps.game.one.behaviours.Idle
import screeps.game.one.behaviours.Refill
import traveler.TravelToOptions
import traveler.TravelerCreep

val DIRECTIONS = setOf(
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    TOP_LEFT
)

fun Creep.isBigMiner() = this.hasBody(BodyDefinition.MINER_BIG)

fun Creep.isHauler() = this.hasBody(BodyDefinition.HAULER)

fun Creep.isMiner() = this.hasBody(BodyDefinition.MINER)

fun Creep.isWorker() = this.hasBody(BodyDefinition.WORKER)

fun Creep.hasBody(bodyDefinition: BodyDefinition) = this.name.startsWith(bodyDefinition.name)

fun Creep.moveInRandomDirection() {
    this.move(DIRECTIONS.random())
}

fun Creep.travelTo(target: RoomPosition, travelToOptions: TravelToOptions? = null): ScreepsReturnCode {
    return if (travelToOptions == null) {
        (this.unsafeCast<TravelerCreep>()).travelTo(target)
    } else {
        (this.unsafeCast<TravelerCreep>()).travelTo(target, travelToOptions)
    }
}

fun <T : RoomObject> Creep.findClosest(roomObjects: Collection<T>): T? = findClosest(roomObjects.toTypedArray())

fun <T : RoomObject> Creep.findClosest(roomObjects: Array<out T>): T? {
    var closest: T? = null
    var minDistance = Int.MAX_VALUE
    for (roomObject in roomObjects) {
        val dist = (roomObject.pos.x - this.pos.x) * (roomObject.pos.x - this.pos.x) +
                (roomObject.pos.y - this.pos.y) * (roomObject.pos.y - this.pos.y)

        if (dist < minDistance) {
            minDistance = dist
            closest = roomObject
        }
    }
    return closest
}

fun <T : RoomObject> Creep.findClosestNotEmpty(roomObjects: Array<out T>): T {
    require(roomObjects.isNotEmpty())
    return findClosest(roomObjects)!!
}

fun Creep.getAssignedEnergySource() = Game.getObjectById<Identifiable>(this.memory.assignedEnergySource).apply {
    if (this == null) {
        this@getAssignedEnergySource.memory.assignedEnergySource = null
    }
}

fun Creep.refillFrom(miner: Creep) {
    require(miner.isMiner())
    val minerTile = miner.room.lookAt(miner.pos).firstOrNull()
    when {
        minerTile == null -> println("assigned miner ${miner.id} is not yet mining")
        minerTile.type == LOOK_RESOURCES && minerTile.resource!!.resourceType == RESOURCE_ENERGY -> this.refillFrom(
            minerTile.resource!!
        )
        minerTile.type == LOOK_STRUCTURES && minerTile.structure!!.structureType == STRUCTURE_CONTAINER -> this.refillFrom(
            minerTile.structure as StructureContainer
        )
    }
}

fun Creep.refillFrom(resource: Resource) {
    if (this.pickup(resource) == ERR_NOT_IN_RANGE) {
        this.travelTo(resource.pos)
    }
}

fun Creep.refillFrom(source: Structure) {
    when (this.withdraw(source, RESOURCE_ENERGY)) {
        OK -> run { }
        ERR_NOT_IN_RANGE -> this.travelTo(source.pos)
        ERR_NOT_ENOUGH_RESOURCES -> this.memory.assignedEnergySource = null
        else -> {
            println("${this.name} could now withdraw from (${source.id})")
            this.say("could now withdraw from (${source.id})")
            this.memory.assignedEnergySource = null
        }
    }
}

fun Creep.run(behaviour: Behaviour) {
    behaviour.run(this)
}

fun Creep.shouldContinueMining() = this.isMiner() || this.carry.energy < this.carryCapacity

fun Creep.tick() {
    when (memory.state) {
        CreepState.UNKNOWN -> {
            println("creep ${name} was in UKNOWN state. Resuming from IDLE")
            run(Idle)
        }
        CreepState.IDLE -> run(Idle)
        CreepState.REFILL -> run(Refill)
        else -> run(Busy)
    }
}

fun Map<String, Creep>.tick() = values.filterNot { it.spawning }.forEach { it.tick() }

fun Map<String, Creep>.onMission(missionId: String): List<Creep> = values.filter { it.memory.missionId == missionId }

fun Map<String, Creep>.assignedEnergySources() = this.values.mapNotNull { it.memory.assignedEnergySource }.distinct()
