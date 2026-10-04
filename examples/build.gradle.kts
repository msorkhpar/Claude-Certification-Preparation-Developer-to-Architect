// The shared build rules of every JVM example edition: one place for versions, source layout, tests and the run task.
//   gradle --offline test            run the tests of every edition
//   gradle --offline runExample      print each edition's output to <exOut>/ex-<dir>-<java|kotlin>-out.txt
// Flags: -PbuildRoot=<dir> (where build output goes), -PexOut=<dir> (where printed output goes).
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins { kotlin("jvm") version "2.4.20" apply false }

val buildRoot = (findProperty("buildRoot") ?: "$rootDir/.build-jvm") as String
val exOut = (findProperty("exOut") ?: "$buildRoot/out") as String

// Libraries only some examples use (the MCP SDKs, the YAML reader); everything else comes from the harness.
val extras = mapOf(
    "32-java" to listOf("io.modelcontextprotocol.sdk:mcp:2.0.1"),
    "33-java" to listOf("io.modelcontextprotocol.sdk:mcp:2.0.1", "org.apache.tomcat.embed:tomcat-embed-core:11.0.10"),
    "32-kotlin" to listOf("io.modelcontextprotocol:kotlin-sdk-server:0.15.0", "io.modelcontextprotocol:kotlin-sdk-client:0.15.0"),
    "33-kotlin" to listOf("io.modelcontextprotocol:kotlin-sdk-server:0.15.0", "io.modelcontextprotocol:kotlin-sdk-client:0.15.0", "io.ktor:ktor-server-cio:3.5.1", "io.ktor:ktor-client-cio:3.5.1"),
    "38" to listOf("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2"),
    "39" to listOf("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2"),
    "40" to listOf("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2"),
)

subprojects {
    val projName = name
    val isHarness = path == ":harness"
    val isKotlin = name.endsWith("-kotlin")
    apply(plugin = if (isKotlin) "org.jetbrains.kotlin.jvm" else "java-library")
    repositories { mavenCentral() }
    layout.buildDirectory.set(file("$buildRoot/${if (isHarness) "harness" else name}"))
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // gradle warm (online, once): resolves every dependency of every edition into the Gradle cache, so that later runs work offline
    tasks.register("warm") {
        doLast { configurations.filter { it.isCanBeResolved }.forEach { c -> try { c.resolve() } catch (e: Exception) { logger.warn("not resolved: ${c.name}: ${e.message?.lines()?.first()}") } } }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        if (isHarness) {
            "api"("com.anthropic:anthropic-java:2.68.0")
        } else {
            "implementation"(project(":harness"))
            (extras["${projName.take(2)}-${if (isKotlin) "kotlin" else "java"}"] ?: extras[projName.take(2)])?.forEach { "implementation"(it) }
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events("failed"); showExceptions = true; exceptionFormat = TestExceptionFormat.SHORT }
        val summary = file("$exOut/ex-$projName-test.txt")
        addTestListener(object : TestListener {
            override fun beforeSuite(s: TestDescriptor) {}
            override fun beforeTest(t: TestDescriptor) {}
            override fun afterTest(t: TestDescriptor, r: TestResult) {}
            override fun afterSuite(s: TestDescriptor, r: TestResult) {
                if (s.parent == null) { summary.parentFile.mkdirs(); summary.writeText("tests ${r.testCount}, passed ${r.successfulTestCount}, failed ${r.failedTestCount}\n") }
            }
        })
    }
    if (!isHarness) {
        // the editions are flat folders: Name.java or Name.kt next to NameTest.java or NameTest.kt
        val ext = if (isKotlin) "kt" else "java"
        val skip = listOf("**/home/**", "**/.gradle/**")
        extensions.configure<SourceSetContainer> {
            named("main") { java.setSrcDirs(if (isKotlin) emptyList<File>() else listOf(projectDir)); java.exclude("**/*Test.java", *skip.toTypedArray()); resources.setSrcDirs(emptyList<File>()) }
            named("test") { java.setSrcDirs(if (isKotlin) emptyList<File>() else listOf(projectDir)); java.include("**/*Test.java"); resources.setSrcDirs(emptyList<File>()) }
        }
        if (isKotlin) {
            extensions.configure<KotlinJvmProjectExtension> {
                sourceSets.getByName("main").kotlin.setSrcDirs(listOf(projectDir)).also { sourceSets.getByName("main").kotlin.exclude("**/*Test.kt", *skip.toTypedArray()) }
                sourceSets.getByName("test").kotlin.setSrcDirs(listOf(projectDir)).also { sourceSets.getByName("test").kotlin.include("**/*Test.kt") }
            }
        }
        val spec = groovy.json.JsonSlurper().parse(File(projectDir.parentFile, "example.json")) as Map<*, *>
        val mainFile = ((spec["files"] as Map<*, *>)[if (isKotlin) "kotlin" else "java"] as String).substringBeforeLast('.')
        val mainClassName = if (isKotlin) "${mainFile}Kt" else mainFile
        val mainSources = the<SourceSetContainer>()["main"]
        tasks.register<JavaExec>("runExample") {
            group = "verification"
            dependsOn("classes")
            classpath = mainSources.runtimeClasspath
            this.mainClass.set(mainClassName)
            val out = file("$exOut/ex-$projName-out.txt")
            var stream: java.io.OutputStream? = null
            doFirst { out.parentFile.mkdirs(); stream = out.outputStream(); standardOutput = stream!! }
            doLast { stream?.close() }
        }
    }
}
