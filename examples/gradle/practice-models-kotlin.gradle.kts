// Shared by the Kotlin editions of the configuration practices (build side): the rules that turn the folders of the example editions into
// library modules, the same source layout and versions as examples/build.gradle.kts. The practice's own build file applies the Kotlin plugin
// and adds `testImplementation(project(":<example folder>"))` for each model it uses. The modules are built once, into ../.build-kotlin/models,
// whichever solution is under test.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

subprojects {
    apply(plugin = if (name == "harness") "java-library" else "org.jetbrains.kotlin.jvm")
    repositories { mavenCentral() }
    layout.buildDirectory.set(rootProject.file("../.build-kotlin/models/$name"))
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    tasks.withType<KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }
    dependencies {
        if (name == "harness") "api"("com.anthropic:anthropic-java:2.68.0") else "implementation"(project(":harness"))
    }
    if (name != "harness") {
        extensions.configure<SourceSetContainer> {
            named("main") {
                java.setSrcDirs(emptyList<File>())
                resources.setSrcDirs(emptyList<File>())
            }
        }
        extensions.configure<KotlinJvmProjectExtension> {
            sourceSets.getByName("main").kotlin.setSrcDirs(listOf(projectDir)).also { sourceSets.getByName("main").kotlin.exclude("**/*Test.kt", "**/home/**", "**/.gradle/**", "**/*.gradle.kts") }
        }
    }
}
