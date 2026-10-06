rootProject.name = "project_setup"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":38-settings-layers")
project(":38-settings-layers").projectDir = file("libs/38-settings-layers")
include(":39-hook-gate")
project(":39-hook-gate").projectDir = file("libs/39-hook-gate")
