rootProject.name = "plugin_setup"

// The tests judge the project with the course's own models of the documented rules, which are the Kotlin edition of one example. They are
// found in the folder that holds examples/ (/w in the course's runner, else the nearest folder above this one that has it).
gradle.extra["repoRoot"] = (listOf(File("/w")) + generateSequence(settingsDir) { it.parentFile })
    .firstOrNull { File(it, "examples/gradle/practice-models.settings.gradle.kts").isFile } ?: error("no examples folder found above $settingsDir")
extra["lang"] = "kotlin"
extra["models"] = listOf("39-hook-gate")
apply(from = File(gradle.extra["repoRoot"] as File, "examples/gradle/practice-models.settings.gradle.kts"))
