package screeps.game.one.missions

import screeps.api.Game
import screeps.api.structures.StructureController

//sealed class UpgradeMission1
//class RoomUpgradeMission : UpgradeMission1()
//sealed class RunningUpgradeMission : UpgradeMission1()
//class EasyUpgradeMission : RunningUpgradeMission()
//class LinkUpgradeMission : RunningUpgradeMission()
//class RCL8UpgradeMission : RunningUpgradeMission()

/**
 * Mission to upgrade a controller using multiple creeps to carry energy
 * Can be cached safely
 *
 * @throws IllegalStateException if it can't be initialized
 */
abstract class UpgradeMission(private val controllerId: String) : Mission() {
    val controller: StructureController
        get() = Game.getObjectById(controllerId)
            ?: throw IllegalStateException("could not find controller $controllerId probably due to lack of vision")

    abstract fun abort()
}
