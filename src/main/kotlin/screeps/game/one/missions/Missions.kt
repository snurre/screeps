package screeps.game.one.missions

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import screeps.api.Game
import screeps.api.Memory
import screeps.api.structures.StructureController
import screeps.game.one.profiled

object Missions {
    val missionMemory: ActiveMissionMemory
    val activeMissions: MutableList<Mission> = mutableListOf()

    init {
        missionMemory = Memory.activeMissions ?: ActiveMissionMemory(mutableListOf())
    }

    fun complete() {
        // remove completed mission TODO do this with mission.complete = true
        val removed = missionMemory.upgradeMissions.removeAll {
            val controller = Game.getObjectById<StructureController>(it.controllerId)
            controller == null || !controller.my
        }
        if (removed) {
            println("removed a mission")
            activeMissions.clear()
            save()
        }
    }

    fun load() = profiled("missions.load") {
        fun <T : Mission> List<MissionMemory<T>>.restore() {
            filter { activeMissions.none { it.missionId == it.missionId } }.forEach { activeMissions.add(it.restoreMission()) }
        }
        missionMemory.upgradeMissions.restore()
        missionMemory.colonizeMissions.restore()
    }

    fun update() = profiled("missions.update") {
        for (mission in activeMissions) {
            mission.update()
        }
        activeMissions.removeAll { it.complete }
        missionMemory.colonizeMissions.removeAll { it.isComplete() }
    }

    fun save() = profiled("missions.save") {
        Memory.activeMissions = missionMemory
    }

    fun start() {
//        if (activeMissions.isEmpty() && missionMemory.upgradeMissions.isEmpty()) {
//            val q = RoomUpgradeMission(GameLoop.mainSpawn.room.controller!!.missionId)
//            missionMemory.upgradeMissions.add(q.memory)
//            activeMissions.add(q)
//        }
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    private var Memory.activeMissions: ActiveMissionMemory?
        get() {
            val internal = this.asDynamic()._missionMemory
            return if (internal == null) null else Json.parse(internal)
        }
        set(value) {
            val stringyfied = if (value == null) null else Json.stringify(value)
            this.asDynamic()._missionMemory = stringyfied
        }
}
