import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "2.1.21"
    id("application")
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("io.gitlab.arturbosch.detekt") version("1.23.8")
    id("com.github.ben-manes.versions") version("0.52.0")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.mikeb"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.3.0.202506031305-r")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
}

// Configure the main class for the executable JAR
application {
    mainClass.set("org.mikeb.gstat.Gstat")
}
kotlin {
    jvmToolchain(17)
}

fun String.isNonStable(): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(this)
  return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}

tasks.register("generateVersionFile") {
    val versionFile = file("src/main/kotlin/org/mikeb/gstat/Version.kt")
    outputs.file(versionFile)

    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText("""
            package org.mikeb.gstat

            object Version {
                const val VERSION = "${project.version}"
            }
            
        """.trimIndent())
    }
}
