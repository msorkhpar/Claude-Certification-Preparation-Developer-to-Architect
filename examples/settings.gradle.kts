// One Gradle build for the Java and Kotlin editions of every example (the Python and TypeScript editions need none).
// A project is <example dir>-java or <example dir>-kotlin, found by the folders that exist; the shared build rules are in build.gradle.kts.
rootProject.name = "examples"

include(":harness")
project(":harness").projectDir = file("../harness/jvm")

file(".").listFiles { f -> f.isDirectory && Regex("""\d\d-.+""").matches(f.name) }!!.sortedBy { it.name }.forEach { dir ->
    for (lang in listOf("java", "kotlin")) {
        if (File(dir, lang).isDirectory) {
            include(":${dir.name}-$lang")
            project(":${dir.name}-$lang").projectDir = File(dir, lang)
        }
    }
}
