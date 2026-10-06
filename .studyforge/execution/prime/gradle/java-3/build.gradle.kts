plugins { java }

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("com.anthropic:anthropic-java:2.68.0")
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.1")
    implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
