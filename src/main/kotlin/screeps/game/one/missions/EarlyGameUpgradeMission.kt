package screeps.game.one.missions

import screeps.api.Creep
import screeps.game.one.BodyDefinition
import screeps.game.one.Context
import screeps.game.one.spawn.CustomSpawnOptions
import screeps.game.one.spawn.GlobalSpawnQueue
import screeps.game.one.extensions.*

class EarlyGameUpgradeMission(
    override val parent: UpgradeMission,
    controllerId: String,
    private val minWorkerCount: Int
) : UpgradeMission(controllerId) {

    override val missionId: String
        get() = parent.missionId

    // TODO must not cache workers!!
    private val workers: MutableList<Creep> = mutableListOf()

    init {
        workers.addAll(Context.creeps.values.filter { it.memory.missionId == missionId })
    }

    override fun update() {
        if (workers.size < minWorkerCount) {
            workers.clear()
            workers.addAll(Context.creeps.values.filter { it.memory.missionId == missionId })

            if (workers.size < minWorkerCount
                && workers.size + GlobalSpawnQueue.spawnQueue.count { it.spawnOptions.missionId == missionId } < minWorkerCount
            ) {
                BodyDefinition.WORKER.requestCreep(
                    CustomSpawnOptions(
                        CreepState.UPGRADING,
                        missionId
                    )
                )
                println("requested creep for EarlyGameUpgradeMission $missionId in ${controller.room}")
            }
        }

        for (worker in workers) {
            if (worker.memory.state == CreepState.IDLE) {
                worker.memory.state = CreepState.UPGRADING
                worker.memory.targetId = controller.id
            }
        }
        workers.clear() // TODO do this more efficiently
    }

    override fun abort() {
        // return workers to pool
        for (worker in workers) {
            worker.memory.missionId = null
            worker.memory.state = CreepState.IDLE
        }
    }
}
