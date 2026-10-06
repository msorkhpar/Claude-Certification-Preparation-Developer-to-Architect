plugins { kotlin("jvm") version "2.4.20" }

// starter, reference or a planted wrong solution: -Psolution=reference. The solution is a folder of files; the tests read it.
val solution = (findProperty("solution") ?: "starter") as String

repositories { mavenCentral() }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets {
    test { kotlin.setSrcDirs(listOf("tests")) }
}
layout.buildDirectory.set(file("../.build-kotlin/$solution"))
tasks.test {
    useJUnitPlatform()
    systemProperty("solution.dir", file(solution).absolutePath)
    testLogging { events("passed", "failed"); showExceptions = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}
