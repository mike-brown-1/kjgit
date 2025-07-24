plugins {
    kotlin("jvm") version "2.1.21"
    id("application")
    id("org.graalvm.buildtools.native") version "0.10.2"
    id("io.gitlab.arturbosch.detekt") version("1.23.3")
    id("com.github.ben-manes.versions") version("0.51.0")
}

group = "org.mikeb"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // JGit dependency for Git operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    // Logging dependency (optional, but good practice for JGit's verbose logging)
    // JGit uses SLF4J, so we'll provide a simple implementation like Logback Classic
    implementation("ch.qos.logback:logback-classic:1.5.18") // Use a recent stable Logback version

    // Test dependencies (optional, but good for testing)
    testImplementation(kotlin("test"))
}

// Configure the main class for the executable JAR
application {
    mainClass = "org.mikeb.kgstat.GitRepoScannerKt" // Update this if your package/file name changes
}
kotlin {
    jvmToolchain(17)
}
