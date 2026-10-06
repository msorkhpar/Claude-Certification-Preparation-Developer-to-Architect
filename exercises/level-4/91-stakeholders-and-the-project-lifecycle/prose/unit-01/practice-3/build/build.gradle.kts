plugins { java }

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories { mavenCentral() }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    systemProperty("solution.dir", layout.projectDirectory.dir("project").asFile.absolutePath)
}

sourceSets {
    main { java.srcDir("project") }
}
