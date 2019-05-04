package screeps.game.one.extensions

import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.game.one.spawn.GlobalSpawnQueue
import screeps.game.one.spawn.CustomSpawnOptions
import screeps.game.one.BodyDefinition
import screeps.game.one.missions.Missions
import screeps.game.one.missions.RoomUpgradeMission

fun StructureSpawn.spawn(
    bodyDefinition: BodyDefinition,
    options: CustomSpawnOptions = GlobalSpawnQueue.defaultSpawnOptions
): ScreepsReturnCode {
    if (room.energyAvailable < bodyDefinition.cost) return ERR_NOT_ENOUGH_ENERGY
    val body = bodyDefinition.getBiggest(room.energyAvailable)
    val name = "${bodyDefinition.name}_T${body.tier}_${Game.time}"
    println("actual mission = ${options.toSpawnOptions().memory?.missionId}")
    return spawnCreep(body.body.toTypedArray(), name, options.toSpawnOptions()).also {
        if (it == OK) println("Spawning $name with spawnOptions $options")
    }
}

fun StructureSpawn.tick() = RoomUpgradeMission.forRoom(room)

fun Record<String, StructureSpawn>.tick() =
    this.values.filter { it.my && it.room.controller!!.my && Missions.missionMemory.upgradeMissions.none { mission -> mission.controllerId == it.room.controller!!.id } }.forEach { it.tick() }
