package screeps.game.one

import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room

object Stats {
    private const val FN_PREFIX = "cpu.usage.fn."
    private const val RESET_TICK_TIMEOUT = 19 // 60s / 3.2 (s/tick) = 19 ticks
    private var globalreset = true // is initialized on global reset
    private var resetInTicks = RESET_TICK_TIMEOUT

    var Memory.stats: dynamic
        get() = this.asDynamic().stats
        set(value) = run { this.asDynamic().stats = value }

    init {
        if (Memory.stats == null) {
            Memory.stats = Any()
        }
    }

    fun write(room: Room) {
        val roomName = "rooms.${room.name}"

        Memory.stats["$roomName.mine"] = room.controller?.level ?: 0
        Memory.stats["$roomName.energyAvailable"] = room.energyAvailable
        Memory.stats["$roomName.energyCapacityAvailable"] = room.energyCapacityAvailable
        room.storage?.let { Memory.stats["$roomName.storage"] = it.store }
        room.controller?.let {
            val controller = "$roomName.controller"
            Memory.stats["$controller.level"] = it.level
            Memory.stats["$controller.progress"] = it.progress
            Memory.stats["$controller.progressTotal"] = it.progressTotal
        }
    }

    fun write(key: String, value: Any) {
        Memory.stats[key] = value
    }

    fun tickStarts() {
        resetInTicks -= 1
        if (resetInTicks < 0) {
            resetInTicks = RESET_TICK_TIMEOUT
            Memory.stats = Any()
        }
    }

    fun tickEnds() {
        Memory.stats["cpu.used"] = Game.cpu.getUsed()
        Memory.stats["cpu.limit"] = Game.cpu.limit
        Memory.stats["cpu.bucket"] = Game.cpu.bucket

        if (globalreset) {
            Memory.stats["globalreset"] = Game.time
            globalreset = false
        }
    }

    fun <R> profiled(name: String, prefix: String? = null, block: () -> R): R {
        val key = FN_PREFIX + if (prefix != null) ".$prefix." else "" + name
        val cpuBefore = Game.cpu.getUsed()

        val result = block()

        val cpuUsed = Game.cpu.getUsed() - cpuBefore
        Memory.stats[key] = cpuUsed
        return result
    }
}

fun <R> profiled(name: String, prefix: String? = null, block: () -> R) = Stats.profiled(name, prefix, block)
