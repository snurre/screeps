package screeps.game.one.extensions

import screeps.api.structures.StructureTower

fun StructureTower.tick() = attack(room.findHostileCreeps().minBy { it.hits }!!)

fun List<StructureTower>.tick() = filter { tower -> tower.energy > 0 }.forEach { it.tick() }
