import org._10ne.gradle.rest.RestTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.31")
    }
}

plugins {
    id("kotlin2js") version "1.3.31"
    id("kotlin-dce-js") version "1.3.31"
    id("org.tenne.rest") version "0.4.2"
//    id("kotlinx-serialization")
}

repositories {
    jcenter()
    maven(url = "https://dl.bintray.com/exav/screeps-kotlin")
    maven(url = "https://kotlin.bintray.com/kotlinx")
    mavenCentral()
}

dependencies {
    implementation("ch.delconte.screeps-kotlin:screeps-kotlin-types:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.11.0")
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test-js"))
}

val screepsUser: String? by project
val screepsPassword: String? by project
val screepsToken: String? by project
val screepsHost: String? by project
val screepsBranch: String? by project
val branch = screepsBranch ?: "kotlin-start"
val host = screepsHost ?: "https://screeps.com"

tasks {
    "compileKotlin2Js"(Kotlin2JsCompile::class) {
        kotlinOptions {
            moduleKind = "commonjs"
            outputFile = "$buildDir/screeps/main.js"
            sourceMap = true
            metaInfo = true
            freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental,kotlinx.serialization.ImplicitReflectionSerializer")
        }
    }

    "runDceKotlinJs"(KotlinJsDce::class) {
        keep("main.loop", "main.Traveler", "Traveler")
        dceOptions.devMode = true
    }
}
