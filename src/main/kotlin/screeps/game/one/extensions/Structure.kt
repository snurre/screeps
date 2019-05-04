package screeps.game.one.extensions

import screeps.api.StructureConstant
import screeps.api.structures.Structure

fun Iterable<Structure>.filterIsStrucure(structureType: StructureConstant): List<Structure> =
    this.filter { it.structureType == structureType }
