plugins { kotlin("jvm") version "2.4.20" }

repositories { mavenCentral() }

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.15.0")
    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.15.0")
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
    // the tests start the solution as a separate process over stdio: it needs the classes and the SDK jars
    systemProperty("server.classpath", sourceSets.main.get().runtimeClasspath.asPath)
}

tasks.register<JavaExec>("tryIt") { classpath = sourceSets["main"].runtimeClasspath; mainClass.set("TryItKt") }
