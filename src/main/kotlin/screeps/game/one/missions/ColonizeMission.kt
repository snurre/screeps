package screeps.game.one.missions

import screeps.api.*
import screeps.game.one.BodyDefinition
import screeps.game.one.Context
import screeps.game.one.spawn.CustomSpawnOptions
import screeps.game.one.extensions.*

class ColonizeMission(private val memory: ColonizeMissionMemory) : Mission() {
    enum class State {
        SPAWNING_CLAIMER,
        CLAIM,
        DONE_CLAIM,
        SPAWNING_BUILDER,
        BUILD_SPAWN,
        NO_SPAWN_LOCATION,
        DONE_BUILD,
        DONE
    }

    override var complete = memory.isComplete()
    override val missionId: String = memory.missionId
    val pos = RoomPosition(memory.x, memory.y, memory.roomName)
    var claimerName: String? = null
    private val workernames: MutableList<String> = mutableListOf()

    override fun update() {
        when (memory.state) {
            State.SPAWNING_CLAIMER -> {
                val claimer = Context.creeps.values.find {
                    it.memory.missionId == missionId && it.memory.state == CreepState.CLAIM
                }
                if (claimer == null) {
                    BodyDefinition.CLAIMER.requestCreepOnce(CustomSpawnOptions(CreepState.CLAIM, missionId))
                } else {
                    this.claimerName = claimer.name
                    memory.state = State.CLAIM
                }
            }

            State.CLAIM -> {
                if (claimerName == null || claimerName !in Context.creeps) {
                    memory.state = State.SPAWNING_CLAIMER
                    return
                }

                val claimer = Context.creeps[claimerName!!]!!
                if (claimer.pos.inRangeTo(pos, 1)) {
                    if (claimer.room.controller?.my == true) {
                        memory.state == State.BUILD_SPAWN
                        claimer.memory.state = CreepState.IDLE
                        claimer.memory.missionId = null
                    } else {
                        claimer.claimController(claimer.room.controller!!)
                        //claimer.reserveController(claimer.room.controller!!)
                    }
                } else {
                    val res = claimer.travelTo(pos)
                    if (res != OK) {
                        println("claimer could not move to room ${pos.roomName} because of $res")
                    }
                }
            }

            State.SPAWNING_BUILDER -> {
                val workers = Context.creeps.values.filter {
                    it.memory.missionId == missionId && it.isWorker()
                }

                if (workers.size < MIN_WORKERS) {
                    BodyDefinition.WORKER.requestCreepOnce(CustomSpawnOptions(CreepState.REFILL, missionId))
                } else {
                    this.workernames.addAll(workers.map { it.name })
                    memory.state = State.BUILD_SPAWN
                }
            }

            State.BUILD_SPAWN -> {
                if (workernames.count { it in Context.creeps } < MIN_WORKERS) {
                    memory.state = State.SPAWNING_BUILDER
                    workernames.clear()
                }

                for (workerName in workernames) {
                    if (workerName !in Context.creeps) continue
                    val worker = Context.creeps[workerName]!!
                    buildSpawn(worker)
                }
            }

            State.DONE_BUILD -> {

                workernames.map { Context.creeps[it] }.filterNotNull().forEach {
                    it.memory.state = CreepState.IDLE
                    it.memory.missionId = null
                }
                memory.state = State.DONE
            }

            else -> {
            }
        }

    }

    private fun buildSpawn(worker: Creep) {
        if (worker.carry.energy < worker.carryCapacity) return
        worker.memory.state = CreepState.MISSION

        if (worker.pos.roomName == pos.roomName) {
            if (worker.memory.targetId == null) {
                val constructionSite = findSpawnPosition(Context.rooms[memory.roomName]!!)
                if (constructionSite == null) {
                    if (worker.room.findMySpawns().isNotEmpty()) {
                        memory.state = State.DONE_BUILD
                    } else {
                        memory.state = State.NO_SPAWN_LOCATION
                    }
                    return

                } else {
                    worker.memory.targetId = constructionSite.id
                }
            }
            if (worker.memory.state != CreepState.REFILL && worker.memory.state != CreepState.CONSTRUCTING) {
                worker.memory.state = CreepState.CONSTRUCTING
            }
        } else {
            val res = worker.travelTo(pos)
            if (res != OK) {
                println("worker ${worker.name} could not move to room ${pos.roomName} because of $res")
            }
        }
    }

    private fun findSpawnPosition(room: Room): ConstructionSite? {
        val constructionSite = room.findMyConstructionSites().firstOrNull { it.structureType == STRUCTURE_SPAWN }

        if (constructionSite == null) {
            for ((name, flag) in Game.flags) {
                if (name == "spawn" && flag.pos.roomName == pos.roomName) {
                    room.createConstructionSite(flag.pos, STRUCTURE_SPAWN)
                    return null
                }
            }
            return null
        } else return constructionSite
    }

    companion object {
        private const val MIN_WORKERS = 2
        fun forRoom(room: Room): ColonizeMission {
            val controller = room.controller ?: throw IllegalStateException("Room $room has no controller")
            return forRoom(controller.pos)
        }

        fun forRoom(room: RoomPosition): ColonizeMission {
            val memory = ColonizeMissionMemory(room.x, room.y, room.roomName)
            val mission = ColonizeMission(memory)
            Missions.missionMemory.colonizeMissions.add(memory)
            Missions.activeMissions.add(mission)
            println("spawning persistent ColonizeMission for room ${room.roomName}")

            return mission
        }
    }
}
