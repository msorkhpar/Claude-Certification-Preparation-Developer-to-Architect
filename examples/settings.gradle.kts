// One Gradle build for the Java and Kotlin editions of every example (the Python and TypeScript editions need none).
// An edition is the project :<example dir>:<java|kotlin>, at its own folder, found by the folders that exist. Its build file there names
// the libraries only it uses; the shared build rules are in build.gradle.kts. A project's path is its folder's path, so one edition is
// built and tested alone with `gradle :<example dir>:<java|kotlin>:test`.
rootProject.name = "examples"

include(":harness")
project(":harness").projectDir = file("../harness/jvm")

file(".").listFiles { f -> f.isDirectory && Regex("""\d\d-.+""").matches(f.name) }!!.sortedBy { it.name }.forEach { dir ->
    for (lang in listOf("java", "kotlin")) {
        if (File(dir, lang).isDirectory) include(":${dir.name}:$lang")
    }
}
