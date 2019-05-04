package screeps.game.one.spawn

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import screeps.api.*
import screeps.api.structures.*
import screeps.game.one.BodyDefinition
import screeps.game.one.extensions.*

object GlobalSpawnQueue {
    @Serializable
    private val queue: MutableList<SpawnInfo>
    private var modified: Boolean = false
    val spawnQueue: List<SpawnInfo>
        get() = queue
    val defaultSpawnOptions = CustomSpawnOptions(state = CreepState.IDLE)

    init {
        // load from memory
        queue = try {
            Memory.globalSpawnQueue?.queue?.toMutableList() ?: mutableListOf()
        } catch (e: Error) {
            println("Error while initializing GlobalSpawnQueue: $e")
            mutableListOf()
        }
        println("spawnqueue initialized to $queue")
    }

    fun push(bodyDefinition: BodyDefinition, spawnOptions: CustomSpawnOptions? = null) {
        queue.add(SpawnInfo(bodyDefinition, spawnOptions ?: defaultSpawnOptions))
        modified = true
    }

    fun spawnCreeps(spawns: List<StructureSpawn>) {
        if (queue.isEmpty()) return
        spawns.filter { it.spawning == null }.forEach { spawn ->
            val (bodyDefinition, spawnOptions) = queue.first()
            when (val code = spawn.spawn(bodyDefinition, spawnOptions)) {
                OK -> {
                    queue.removeAt(0)
                    modified = true
                }
                ERR_NOT_ENOUGH_ENERGY -> {
                    val creep = queue.removeAt(0)
                    queue.add(creep)
                    modified = true
                }
                else -> println("Unexpected return code $code when spawning creep ${bodyDefinition.name} with $spawnOptions")
            }
        }
    }

    fun save() {
        if (modified) Memory.globalSpawnQueue = CreepSpawnList(queue)
        modified = false
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    private var Memory.globalSpawnQueue: CreepSpawnList?
        get() {
            val internal = this.asDynamic().globalSpawnQueue
            return if (internal == null) null else Json.parse(internal)
        }
        set(value) {
            val stringyfied = if (value == null) null else Json.stringify(value)
            this.asDynamic().globalSpawnQueue = stringyfied
        }
}

