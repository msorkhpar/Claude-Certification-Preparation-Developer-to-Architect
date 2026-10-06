rootProject.name = "memory_setup"

// The tests judge the project with the course's own models of the documented rules, which are the Java editions of four examples. They are
// found in the folder that holds examples/ (/w in the course's runner, else the nearest folder above this one that has it).
gradle.extra["repoRoot"] = (listOf(File("/w")) + generateSequence(settingsDir) { it.parentFile })
    .firstOrNull { File(it, "examples/gradle/practice-models.settings.gradle.kts").isFile } ?: error("no examples folder found above $settingsDir")
extra["lang"] = "java"
extra["models"] = listOf("38-settings-layers", "39-hook-gate", "56-builtin-tools", "57-memory-loading")
apply(from = File(gradle.extra["repoRoot"] as File, "examples/gradle/practice-models.settings.gradle.kts"))
