pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.id == "kotlin2js" || requested.id.id == "kotlin-dce-js" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                requested.id.id == "kotlinx-serialization" -> useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}

rootProject.name = "screeps"
