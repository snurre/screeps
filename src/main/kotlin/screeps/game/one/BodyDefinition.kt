package screeps.game.one

import screeps.api.*

enum class BodyDefinition(val body: Array<BodyPartConstant>, val maxSize: Int = 0) {
    WORKER(arrayOf(WORK, CARRY, MOVE), maxSize = 5),
    MINER(arrayOf(WORK, WORK, MOVE), maxSize = 2),
    MINER_BIG(
        arrayOf(
            WORK,
            WORK,
            WORK,
            WORK,
            WORK,
            MOVE,
            MOVE
        ), maxSize = 1
    ), //completely drains a Source
    WORKER_BIG(
        arrayOf(
            WORK,
            WORK,
            WORK,
            WORK,
            CARRY,
            MOVE,
            MOVE
        )
    ),
    HAULER(arrayOf(CARRY, CARRY, MOVE), maxSize = 5),
    SCOUT(arrayOf(MOVE), maxSize = 1),
    CLAIMER(arrayOf(CLAIM, MOVE), maxSize = 1);

    val cost = body.sumBy { BODYPART_COST[it]!! }

    data class Body(val tier: Int, val body: List<BodyPartConstant>)

    fun getBiggest(availableEnergy: Int): Body {
        var energyCost = availableEnergy
        val body = mutableListOf<BodyPartConstant>()
        var size = 0

        while (energyCost - cost >= 0 && (maxSize == 0 || size < maxSize)) {
            energyCost -= cost
            body.addAll(body)
            size += 1
        }
        body.sortBy { it.value }

        return Body(size, body)
    }
}
