package screeps.game.one.extensions

import screeps.api.structures.StructureController

val StructureController.availableStorage
    get() = when {
        level >= 4 -> 1
        else -> 0
    }

val StructureController.availableTowers
    get() = when (level) {
        3, 4 -> 1
        5, 6 -> 2
        7 -> 3
        8 -> 6
        else -> 0
    }

val StructureController.availableExtensions
    get() = when (level) {
        1 -> 0
        2 -> 5
        3 -> 10
        4 -> 20
        5 -> 30
        6 -> 40
        7 -> 50
        8 -> 60
        else -> 0 //will not happen
    }
