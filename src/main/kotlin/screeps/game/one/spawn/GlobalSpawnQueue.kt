package screeps.game.one.spawn

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import screeps.api.*
import screeps.api.structures.*
import screeps.game.one.BodyDefinition
import screeps.game.one.extensions.*

object GlobalSpawnQueue {
    val defaultSpawnOptions = CustomSpawnOptions(state = CreepState.IDLE)
    val queue: List<SpawnInfo>
        get() = data.queue
    private val data: SpawnQueueData
    private var modified: Boolean = false

    init {
        data = Memory.spawnQueue ?: SpawnQueueData()
    }

    fun push(bodyDefinition: BodyDefinition, spawnOptions: CustomSpawnOptions? = null) {
        data.queue.add(SpawnInfo(bodyDefinition, spawnOptions ?: defaultSpawnOptions))
        modified = true
    }

    fun spawnCreeps(spawns: List<StructureSpawn>) {
        if (data.queue.isEmpty()) return
        spawns.filter { it.spawning == null }.forEach { spawn ->
            val (bodyDefinition, optons) = data.queue.first()
            when (val code = spawn.spawn(bodyDefinition, optons)) {
                OK -> {
                    data.queue.removeAt(0)
                    modified = true
                }
                ERR_NOT_ENOUGH_ENERGY -> {
                    val creep = data.queue.removeAt(0)
                    data.queue.add(creep)
                    modified = true
                }
                else -> println("Unexpected return code $code when spawning creep $bodyDefinition with $optons")
            }
        }
    }

    fun save() {
//        if (modified) Memory.spawnQueue = data
        modified = false
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    private var Memory.spawnQueue: SpawnQueueData?
        get() {
            val internal = this.asDynamic()._spawnQueue
            return if (internal == null) null else Json.parse(internal)
        }
        set(value) {
            val stringyfied = if (value == null) null else Json.stringify(value)
            this.asDynamic()._spawnQueue = stringyfied
        }
}
