plugins { java }

// starter, reference or a planted wrong solution: -Psolution=reference
val solution = (findProperty("solution") ?: "starter") as String

java { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
repositories { mavenCentral() }
dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets {
    main { java.setSrcDirs(listOf(solution, "tryit")) }
    test { java.setSrcDirs(listOf("tests")) }
}
layout.buildDirectory.set(file("../.build-java/$solution"))
tasks.test {
    useJUnitPlatform()
    // the tests start the solution as a separate process over stdio: it needs the classes and the SDK jars
    systemProperty("server.classpath", sourceSets.main.get().runtimeClasspath.asPath)
    testLogging { events("passed", "failed"); showExceptions = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}

// Run: the reader's own try-it file, with the logger turned up (no tests, no grade): gradle -Psolution=reference tryIt
tasks.register<JavaExec>("tryIt") { classpath = sourceSets["main"].runtimeClasspath; mainClass.set("TryIt") }
