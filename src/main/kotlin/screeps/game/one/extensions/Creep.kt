package screeps.game.one.extensions

import screeps.api.*
import screeps.api.structures.*
import screeps.game.one.*
import screeps.game.one.behaviours.*
import traveler.TravelToOptions
import traveler.TravelerCreep

fun Creep.travelTo(target: RoomPosition, travelToOptions: TravelToOptions? = null): ScreepsReturnCode {
    return if (travelToOptions == null) {
        (this.unsafeCast<TravelerCreep>()).travelTo(target)
    } else {
        (this.unsafeCast<TravelerCreep>()).travelTo(target, travelToOptions)
    }
}

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

fun Creep.isBigMiner() = hasBody(BodyDefinition.MINER_BIG)

fun Creep.isHauler() = hasBody(BodyDefinition.HAULER)

fun Creep.isMiner() = hasBody(BodyDefinition.MINER)

fun Creep.isWorker() = hasBody(BodyDefinition.WORKER)

fun Creep.hasBody(bodyDefinition: BodyDefinition) = name.startsWith(bodyDefinition.name)

fun Creep.moveInRandomDirection() {
    move(DIRECTIONS.random())
}

fun <T : RoomObject> Creep.findClosest(roomObjects: Collection<T>): T? = findClosest(roomObjects.toTypedArray())

fun <T : RoomObject> Creep.findClosest(roomObjects: Array<out T>): T? {
    var closest: T? = null
    var minDistance = Int.MAX_VALUE
    for (roomObject in roomObjects) {
        val dist = (roomObject.pos.x - pos.x) * (roomObject.pos.x - pos.x) +
                (roomObject.pos.y - pos.y) * (roomObject.pos.y - pos.y)

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

fun Creep.getAssignedEnergySource() = Game.getObjectById<Identifiable>(memory.assignedEnergySource).apply {
    if (this == null) {
        this@getAssignedEnergySource.memory.assignedEnergySource = null
    }
}

fun Creep.refillFrom(miner: Creep) {
    require(miner.isMiner())
    val minerTile = miner.room.lookAt(miner.pos).firstOrNull()
    when {
        minerTile == null -> println("assigned miner ${miner.id} is not yet mining")
        minerTile.type == LOOK_RESOURCES && minerTile.resource!!.resourceType == RESOURCE_ENERGY -> refillFrom(
            minerTile.resource!!
        )
        minerTile.type == LOOK_STRUCTURES && minerTile.structure!!.structureType == STRUCTURE_CONTAINER -> refillFrom(
            minerTile.structure as StructureContainer
        )
    }
}

fun Creep.refillFrom(resource: Resource) {
    if (pickup(resource) == ERR_NOT_IN_RANGE) {
        travelTo(resource.pos)
    }
}

fun Creep.refillFrom(source: Structure) {
    when (withdraw(source, RESOURCE_ENERGY)) {
        OK -> run { }
        ERR_NOT_IN_RANGE -> travelTo(source.pos)
        ERR_NOT_ENOUGH_RESOURCES -> memory.assignedEnergySource = null
        else -> {
            println("${name} could now withdraw from (${source.id})")
            say("could now withdraw from (${source.id})")
            memory.assignedEnergySource = null
        }
    }
}

fun Creep.run(behaviour: Behaviour) {
    behaviour.run(this)
}

fun Creep.shouldContinueMining() = isMiner() || carry.energy < carryCapacity

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

fun Map<String, Creep>.assignedEnergySources() = values.mapNotNull { it.memory.assignedEnergySource }.distinct()
