package screeps.game.one.missions

abstract class MissionData<T : Mission> {
    abstract val missionId: String
    abstract fun restoreMission(): T
    open fun isComplete(): Boolean = false
}
