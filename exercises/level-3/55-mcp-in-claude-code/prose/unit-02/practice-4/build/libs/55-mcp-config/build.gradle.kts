plugins { kotlin("jvm") version "2.4.20" }

repositories { mavenCentral() }
dependencies {
    implementation(project(":harness"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.4")
}
