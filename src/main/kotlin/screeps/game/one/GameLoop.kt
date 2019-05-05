package screeps.game.one

import screeps.api.*
import screeps.game.one.extensions.*
import screeps.game.one.missions.Missions
import screeps.game.one.spawn.CustomSpawnOptions
import screeps.game.one.spawn.GlobalSpawnQueue
import screeps.utils.lazyPerTick

object GameLoop {
    private val mainSpawn = Game.spawns["Spawn1"]!!
    private val energySources: Array<Source> by lazyPerTick { mainSpawn.room.findEnergy() }
    private val minMiners: Int = energySources.size
    private val minWorkers: Int = energySources.size * 2

    fun tick() {
        Stats.tickStarts()

        if (Game.time % 100 == 0) {
            gc()
        }

        spawnWorkersInOtherRooms()
        spawnMiners()
        spawnWorkers()
        spawnHaulers()

        Missions.start()

        GlobalSpawnQueue.spawnCreeps(listOf(mainSpawn))

        Game.rooms.tick()

        Missions.complete()
        Missions.load()

        Game.flags.tick()
        Game.spawns.tick()

        Missions.update()

        Context.creeps.tick()

        GlobalSpawnQueue.save()
        Missions.save()

        Stats.tickEnds()
    }

    private fun gc() {
        js(
            """
        for (var name in Memory.creeps) {
            if (!Game.creeps[name]) {
                delete Memory.creeps[name];
                console.log('Clearing non-existing creep memory:', name);
            }
        }
        """
        )
    }

    private fun spawnHaulers() {
        if (Context.haulerCount < minMiners && mainSpawn.room.storage != null) {
            if (GlobalSpawnQueue.queue.count { it.isHauler() } < minMiners - Context.haulerCount) {
                GlobalSpawnQueue.push(BodyDefinition.HAULER)
            }
        }
    }

    private fun spawnMiners() {
        if (Context.minerCount < minMiners && GlobalSpawnQueue.queue.count { it.isMiner() } < minMiners - Context.minerCount) {
            // TODO we cannot spawn small miners
            GlobalSpawnQueue.push(BodyDefinition.MINER_BIG, CustomSpawnOptions(CreepState.REFILL))
        }
    }

    private fun spawnWorkers() {
        if (Context.workerCount < minWorkers) {
            if (GlobalSpawnQueue.queue.count { it.isWorker() } < minWorkers - Context.workerCount) {
                GlobalSpawnQueue.push(BodyDefinition.WORKER)
            }
        }
    }

    private fun spawnWorkersInOtherRooms() {
        for ((_, spawn) in Game.spawns) {
            if (spawn == mainSpawn) continue
            if (spawn.room.findMyCreeps().count { it.isWorker() } < 3) {
                spawn.spawn(BodyDefinition.WORKER)
            }
        }
    }
}
