package screeps.game.one.extensions

import screeps.api.*
import screeps.game.one.Context
import screeps.game.one.Stats
import screeps.utils.copy

fun Room.findStructures() = find(FIND_STRUCTURES)
fun Room.findMyConstructionSites() = find(FIND_MY_CONSTRUCTION_SITES)
fun Room.findMySpawns() = find(FIND_MY_SPAWNS)
fun Room.findEnergy() = find(FIND_SOURCES)
fun Room.findCreeps() = find(FIND_CREEPS)
fun Room.findHostileCreeps() = find(FIND_HOSTILE_CREEPS)
fun Room.findMyCreeps() = find(FIND_MY_CREEPS)
fun Room.findConstructionSites() = find(FIND_CONSTRUCTION_SITES)
fun Room.findDroppedEnergy() = find(FIND_DROPPED_ENERGY)
fun Room.lookAt(pathStep: Room.PathStep) = lookAt(pathStep.x, pathStep.y)

fun Room.buildExtensions() {
    require(controller?.my == true)

    val spawn = findMySpawns().first()
    val startPos = spawn.pos
    val numberOfExtensions: Int = findStructures().count { it.structureType == STRUCTURE_EXTENSION }
    val toPlace = controller!!.availableExtensions - numberOfExtensions
    var placed = 0
    val energySources = findEnergy()
    require(toPlace >= 0)
    val constructionSites = findConstructionSites()
    while (placed < toPlace) {
        //find a road from spawn to energy source
        for (source in energySources) {

        }
    }
}

fun Room.buildRoads() {
    val controller = controller
    if (controller == null || !controller.my) {
        println("cannot buildRoads() in room which is not under our control")
        return
    }
    println("building roads in room $this")

    val spawns = findMySpawns()
    val energySources = findEnergy()

    fun buildRoadBetween(a: RoomPosition, b: RoomPosition) =
        findPath(a, b, options { ignoreCreeps = true }).filter { step ->
            lookAt(step).none { (it.type == LOOK_STRUCTURES && it.structure!!.structureType == STRUCTURE_ROAD) || (it.type == LOOK_CONSTRUCTION_SITES && it.constructionSite!!.structureType == STRUCTURE_ROAD) }
        }.forEach { step ->
            when (val code = createConstructionSite(step.x, step.y, STRUCTURE_ROAD)) {
                OK -> run { }
                else -> println("could not place road at [x=${step.x},y=${step.y}] code=($code)")
            }
        }

    //build roads from controller to each spawn
    for (spawn in spawns) {
        buildRoadBetween(controller.pos, spawn.pos)

        //build roads from each spawn to each source
        for (source in energySources) {
            buildRoadBetween(source.pos, spawn.pos)
        }
    }
}

fun Room.buildStorage() {
    if (controller == null || controller?.my == false) return //not our room
    if (controller!!.availableStorage != 1) return //cannot build storage yet

    val hasStorage =
        storage != null || Context.constructionSites.values.any { it.structureType == STRUCTURE_STORAGE && it.room?.name == name }
    if (hasStorage) return //already built or being  built

    val spawn = findMySpawns().first()
    var placed = false
    var pos = spawn.pos.copy(spawn.pos.x - 2)
    while (!placed) {
        when (val code = createConstructionSite(pos, STRUCTURE_STORAGE)) {
            OK -> placed = true
            ERR_INVALID_TARGET -> pos = pos.copy(x = pos.x - 1)
            else -> println("unexpected return value $code when attempting to place storage")
        }
    }
}

fun Room.buildTowers() {
    if (controller?.my != true) return //not under control
    val numberOfTowers =
        Context.constructionSites.values.count { it.room?.name == name && it.structureType == STRUCTURE_TOWER } + Context.structures.values.count { it.room.name == name && it.structureType == STRUCTURE_TOWER }
    val towersToPlace = controller!!.availableTowers - numberOfTowers
    if (towersToPlace == 0) return //no need to place towers

    val spawn = find(FIND_MY_SPAWNS).first()
    var placed = 0
    var x = spawn.pos.x
    var y = spawn.pos.y + 1

    while (placed < towersToPlace) {
        y += 1
        when (val success = createConstructionSite(x, y, STRUCTURE_TOWER)) {
            OK -> placed += 1
            ERR_INVALID_TARGET -> run { }
            else -> println("unexpected return value $success when attempting to place tower")
        }
    }
}

fun Room.tick() {
    Stats.write(this)

    buildStorage()
    buildTowers()

    if (findHostileCreeps().isNotEmpty()) {
        Context.towers.filter { tower -> tower.room.name == name }.tick()
    }
}

fun Record<String, Room>.tick() {
    this.values.forEach { room ->
        Stats.write(room)
        room.buildStorage()
        room.buildTowers()
        val hostiles = room.findHostileCreeps()
        if (hostiles.isNotEmpty()) {
            Context.towers.filter { tower -> tower.room.name == room.name && tower.energy > 0 }
                .forEach { it.attack(hostiles.minBy { creep -> creep.hits }!!) }
        }
    }
}
