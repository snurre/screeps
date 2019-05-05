package screeps.game.one.missions

import screeps.api.Room

class RoomUpgradeMission(private val data: RoomUpgradeMissionData) : UpgradeMission(data.controllerId) {
    enum class State {
        EARLY, LINK, RCL8_MAINTENANCE, RCL8_IDLE
    }

    override val missionId: String = data.missionId
    var mission: UpgradeMission?

    init {
        when (data.state) {
            State.RCL8_IDLE -> mission = null
            else -> mission = EarlyGameUpgradeMission(
                this,
                data.controllerId,
                if (controller.level == 8) 1 else 3
            )
        }
    }

    companion object {
        const val maxLevel = 8
        fun forRoom(room: Room, state: State = State.EARLY): RoomUpgradeMission {
            val controller = room.controller ?: throw IllegalStateException("Roomcontroller null")
            val memory = RoomUpgradeMissionData(controller.id, state)
            val mission = RoomUpgradeMission(memory)
            Missions.data.roomUpgrade.add(memory)
            Missions.missions.add(mission)
            println("spawning persistent RoomUpgradeMission for room ${room.name}")
            return mission
        }
    }

    override fun update() {
        if (controller.level == maxLevel) {
            if (data.state == State.EARLY) {
                data.state = State.RCL8_MAINTENANCE
            }

            if (data.state == State.RCL8_IDLE && controller.ticksToDowngrade < 100_000) {
                data.state = State.RCL8_MAINTENANCE
                mission = EarlyGameUpgradeMission(this, controller.id, 1)
            } else if (data.state == State.RCL8_MAINTENANCE && controller.ticksToDowngrade > 140_000) {
                data.state = State.RCL8_IDLE
                mission?.abort()
                mission = null
            }
        }

        mission?.update()
    }

    override fun abort() {
        if (controller.my) throw IllegalStateException("stopping to roomUpgrade my controller in Room ${controller.room}")
        else {
            mission = null
            complete = true
        }
    }
}
