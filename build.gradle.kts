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
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // JGit dependency for Git operations latest: 7.3.0.202506031305-r
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.10.0.202406032230-r")

    // Logging dependency (optional, but good practice for JGit's verbose logging)
    // JGit uses SLF4J, so we'll provide a simple implementation like Logback Classic
    implementation("ch.qos.logback:logback-classic:1.5.18") // Use a recent stable Logback version

    // Test dependencies (optional, but good for testing)
    testImplementation(kotlin("test"))
}

// Configure the main class for the executable JAR
application {
    mainClass.set("org.mikeb.kgstat.GitRepoScanner") // Update this if your package/file name changes
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
