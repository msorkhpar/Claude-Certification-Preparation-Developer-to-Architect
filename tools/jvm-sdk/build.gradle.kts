plugins { kotlin("jvm") version "2.4.20"; application }
repositories { mavenCentral() }
dependencies {
    implementation("com.anthropic:anthropic-java:2.68.0")
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.1")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.15.0")
}
application { mainClass.set("MainKt") }
tasks.register("sizes") {
    doLast {
        val files = configurations.runtimeClasspath.get().files
        files.sortedBy { it.name }.forEach { println("JAR ${it.length()} ${it.name}") }
        println("TOTAL ${files.sumOf { it.length() }} in ${files.size} jars")
    }
}
