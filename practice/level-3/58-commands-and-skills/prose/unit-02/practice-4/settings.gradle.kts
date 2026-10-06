rootProject.name = "skill_setup"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":58-skill-model")
project(":58-skill-model").projectDir = file("libs/58-skill-model")
