plugins { kotlin("jvm") version "2.4.20" }

dependencies {
    implementation("com.anthropic:anthropic-java:2.68.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
