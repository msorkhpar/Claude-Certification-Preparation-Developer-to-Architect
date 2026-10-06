// Shared by the Java editions of the configuration practices (build side): the rules that turn the folders of the example editions into
// library modules, the same source layout and versions as examples/build.gradle.kts. The practice's own build file adds
// `testImplementation(project(":<example folder>"))` for each model it uses. The modules are built once, into ../.build-java/models,
// whichever solution is under test.
subprojects {
    apply(plugin = "java-library")
    repositories { mavenCentral() }
    layout.buildDirectory.set(rootProject.file("../.build-java/models/$name"))
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
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
                java.setSrcDirs(listOf(projectDir))
                java.exclude("**/*Test.java", "**/home/**", "**/.gradle/**", "**/*.gradle.kts")
                resources.setSrcDirs(emptyList<File>())
            }
        }
    }
}
