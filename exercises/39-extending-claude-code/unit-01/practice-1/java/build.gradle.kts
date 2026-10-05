plugins { java }

// starter, reference or a planted wrong solution: -Psolution=reference
val solution = (findProperty("solution") ?: "starter") as String
apply(from = File(gradle.extra["repoRoot"] as File, "examples/gradle/practice-models-java.gradle.kts"))

java { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
repositories { mavenCentral() }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.4")
    testImplementation(project(":39-hook-gate"))
}
sourceSets {
    test { java.setSrcDirs(listOf("tests")) }
}
layout.buildDirectory.set(file("../.build-java/$solution"))
tasks.test {
    useJUnitPlatform()
    systemProperty("config.dir", layout.projectDirectory.dir(solution).asFile.absolutePath)
    testLogging { events("passed", "failed"); showExceptions = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}

// Run: the reader's own try-it file, with the logger turned up (no tests, no grade): gradle -Psolution=reference tryIt
tasks.register<JavaExec>("tryIt") { classpath = sourceSets["main"].runtimeClasspath; mainClass.set("TryIt") }
