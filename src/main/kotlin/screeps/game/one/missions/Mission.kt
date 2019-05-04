package screeps.game.one.missions

abstract class Mission(open val parent: Mission? = null) {
    abstract val missionId: String
    abstract fun update()

    open var complete = false
        protected set
}
