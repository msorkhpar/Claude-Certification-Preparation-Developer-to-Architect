plugins { kotlin("jvm") version "2.4.20" }

// starter, reference or a planted wrong solution: -Psolution=reference
val solution = (findProperty("solution") ?: "starter") as String

repositories { mavenCentral() }
dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets {
    main { kotlin.setSrcDirs(listOf(solution)) }
    test { kotlin.setSrcDirs(listOf("tests")) }
}
layout.buildDirectory.set(file("../.build-kotlin/$solution"))
tasks.test {
    useJUnitPlatform()
    // the tests start the solution as a separate process over stdio: it needs the classes and the SDK jars
    systemProperty("server.classpath", sourceSets.main.get().runtimeClasspath.asPath)
    testLogging { events("passed", "failed"); showExceptions = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}
