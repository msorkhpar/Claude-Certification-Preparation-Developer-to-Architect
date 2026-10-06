rootProject.name = "plugin_setup"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":39-hook-gate")
project(":39-hook-gate").projectDir = file("libs/39-hook-gate")
