plugins { kotlin("jvm") version "2.4.20" }

// starter, reference or a planted wrong solution: -Psolution=reference
val solution = (findProperty("solution") ?: "starter") as String
apply(from = File(gradle.extra["repoRoot"] as File, "examples/gradle/practice-models-kotlin.gradle.kts"))

repositories { mavenCentral() }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.4")
    testImplementation(project(":55-mcp-config"))
}
sourceSets {
    test { kotlin.setSrcDirs(listOf("tests")) }
}
layout.buildDirectory.set(file("../.build-kotlin/$solution"))
tasks.test {
    useJUnitPlatform()
    systemProperty("config.dir", layout.projectDirectory.dir(solution).asFile.absolutePath)
    testLogging { events("passed", "failed"); showExceptions = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}
