// Shared by the Java and Kotlin editions of the configuration practices (settings side). A practice's tests read a project the way Claude Code
// would, and judge it with the course's own models of the documented rules: the Java or Kotlin edition of an example, built here as one module
// each (so that two examples never share a namespace). A practice's settings.gradle.kts sets
//   gradle.extra["repoRoot"]   the folder that holds examples/ and harness/
//   extra["lang"]              "java" or "kotlin"
//   extra["models"]            the example folders whose edition the tests use, for example listOf("38-settings-layers")
// and applies this file. The example editions stay where they are: nothing is copied.
@Suppress("UNCHECKED_CAST")
val models = extra["models"] as List<String>
val lang = extra["lang"] as String
val repoRoot = gradle.extra["repoRoot"] as File

include(":harness")
project(":harness").projectDir = File(repoRoot, "harness/jvm")
for (example in models) {
    include(":$example")
    project(":$example").projectDir = File(repoRoot, "examples/$example/$lang")
}
