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
    val data: MissionsData
    val missions: MutableList<Mission> = mutableListOf()

    init {
        data = Memory.missions ?: MissionsData()
    }

    fun complete() {
        // remove completed mission TODO do this with mission.complete = true
        val removed = data.roomUpgrade.removeAll {
            val controller = Game.getObjectById<StructureController>(it.controllerId)
            controller == null || !controller.my
        }
        if (removed) {
            println("removed a mission")
            missions.clear()
            save()
        }
    }

    fun load() = profiled("missions.load") {
        fun <T : Mission> List<MissionData<T>>.restore() {
            filter { missions.none { it.missionId == it.missionId } }.forEach { missions.add(it.restoreMission()) }
        }
        data.roomUpgrade.restore()
        data.colonize.restore()
    }

    fun update() = profiled("missions.update") {
        for (mission in missions) {
            mission.update()
        }
        missions.removeAll { it.complete }
        data.colonize.removeAll { it.isComplete() }
    }

    fun save() = profiled("missions.save") {
//        Memory.missions = data
    }

    fun start() {
//        if (missions.isEmpty() && data.roomUpgrade.isEmpty()) {
//            val q = RoomUpgradeMission(GameLoop.mainSpawn.room.controller!!.missionId)
//            data.roomUpgrade.add(q.memory)
//            missions.add(q)
//        }
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    private var Memory.missions: MissionsData?
        get() {
            val internal = this.asDynamic()._missionMemory
            return if (internal == null) null else Json.parse(internal)
        }
        set(value) {
            val stringyfied = if (value == null) null else Json.stringify(value)
            this.asDynamic()._missionMemory = stringyfied
        }
}
