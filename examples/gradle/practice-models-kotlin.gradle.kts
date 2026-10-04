// Shared by the Kotlin editions of the configuration practices (build side): the rules that turn the folders of the example editions into
// library modules, the same source layout and versions as examples/build.gradle.kts. The practice's own build file applies the Kotlin plugin
// and adds `testImplementation(project(":<example folder>"))` for each model it uses. The modules are built once, into ../.build-kotlin/models,
// whichever solution is under test.
subprojects {
    apply(plugin = if (name == "harness") "java-library" else "org.jetbrains.kotlin.jvm")
    repositories { mavenCentral() }
    layout.buildDirectory.set(rootProject.file("../.build-kotlin/models/$name"))
    // Java and Kotlin both compile for the JDK that runs the build, so their targets agree
    dependencies {
        if (name == "harness") "api"("com.anthropic:anthropic-java:2.68.0") else "implementation"(project(":harness"))
    }
    if (name == "harness") {
        // the harness's own tests are not part of a practice build
        extensions.configure<SourceSetContainer> { named("test") { java.setSrcDirs(emptyList<File>()) } }
    }
    if (name != "harness") {
        extensions.configure<SourceSetContainer> {
            named("main") {
                java.setSrcDirs(emptyList<File>())
                resources.setSrcDirs(emptyList<File>())
            }
        }
        // the Kotlin plugin adds a `kotlin` source directory set to each source set; it is reached by its Gradle core type
        val kotlinMain = (extensions.getByType<SourceSetContainer>().getByName("main") as org.gradle.api.plugins.ExtensionAware)
            .extensions.getByName("kotlin") as org.gradle.api.file.SourceDirectorySet
        kotlinMain.setSrcDirs(listOf(projectDir))
        kotlinMain.exclude("**/*Test.kt", "**/home/**", "**/.gradle/**", "**/*.gradle.kts")
    }
}
