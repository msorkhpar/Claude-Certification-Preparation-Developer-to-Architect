plugins { kotlin("jvm") version "2.4.20" }

repositories { mavenCentral() }

dependencies {
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(":58-skill-model"))
}

layout.buildDirectory.set(layout.projectDirectory.dir("target"))

tasks.test {
    useJUnitPlatform()
    testLogging {
        quiet {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
    systemProperty("config.dir", layout.projectDirectory.dir("project").asFile.absolutePath)
}

sourceSets {
    main { kotlin.srcDir("project") }
}
