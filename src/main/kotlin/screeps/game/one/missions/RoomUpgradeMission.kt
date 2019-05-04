package screeps.game.one.missions

import screeps.api.Room

class RoomUpgradeMission(private val memory: UpgradeMissionMemory) : UpgradeMission(memory.controllerId) {
    enum class State {
        EARLY, LINK, RCL8_MAINTENANCE, RCL8_IDLE
    }

    override val missionId: String = memory.missionId
    var mission: UpgradeMission?

    init {
        when (memory.state) {
            State.RCL8_IDLE -> mission = null
            else -> mission = EarlyGameUpgradeMission(
                this,
                memory.controllerId,
                if (controller.level == 8) 1 else 3
            )
        }
    }

    companion object {
        const val maxLevel = 8
        fun forRoom(room: Room, state: State = State.EARLY): RoomUpgradeMission {
            val controller = room.controller ?: throw IllegalStateException("Roomcontroller null")
            val memory = UpgradeMissionMemory(controller.id, state)
            val mission = RoomUpgradeMission(memory)
            Missions.missionMemory.upgradeMissions.add(memory)
            Missions.activeMissions.add(mission)
            println("spawning persistent RoomUpgradeMission for room ${room.name}")
            return mission
        }
    }

    override fun update() {
        if (controller.level == maxLevel) {
            if (memory.state == State.EARLY) {
                memory.state = State.RCL8_MAINTENANCE
            }

            if (memory.state == State.RCL8_IDLE && controller.ticksToDowngrade < 100_000) {
                memory.state = State.RCL8_MAINTENANCE
                mission = EarlyGameUpgradeMission(this, controller.id, 1)
            } else if (memory.state == State.RCL8_MAINTENANCE && controller.ticksToDowngrade > 140_000) {
                memory.state = State.RCL8_IDLE
                mission?.abort()
                mission = null
            }
        }

        mission?.update()
    }

    override fun abort() {
        if (controller.my) throw IllegalStateException("stopping to upgrade my controller in Room ${controller.room}")
        else {
            mission = null
            complete = true
        }
    }
}
