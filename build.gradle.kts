group = "nz.co.jedsimson.lgp"
version = "1.0"

plugins {
    val kotlinVersion = "1.5.31"

    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
}

repositories {
    mavenCentral()
}

dependencies {
    val frameworkVersion = "5.3"

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kotlin co-routine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")

    // Core abstractions and components of the LGP framework
    implementation("nz.co.jedsimson.lgp:core:$frameworkVersion")
    implementation("nz.co.jedsimson.lgp:core:$frameworkVersion:sources")

    // Implementations for core LGP framework components
    implementation("nz.co.jedsimson.lgp:lib:$frameworkVersion")
    implementation("nz.co.jedsimson.lgp:lib:$frameworkVersion:sources")

    // Logging
    implementation("org.slf4j:slf4j-simple:1.7.29")
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(configurations.runtimeClasspath
            .get()
            .map { if (it.isDirectory) it else zipTree(it) })
    }
}