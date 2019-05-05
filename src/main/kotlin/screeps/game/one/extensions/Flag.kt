package screeps.game.one.extensions

import screeps.api.*
import screeps.game.one.missions.ColonizeMission
import screeps.game.one.missions.Missions

fun Flag.tick() = ColonizeMission.forRoom(pos)

fun Record<String, Flag>.tick() {
    this.entries.filter { (name, flag) -> name == "colonize" && Missions.missions.none { it is ColonizeMission && it.pos.roomName == flag.pos.roomName } }
        .map { it.component2() }
        .forEach { it.tick() }
}
