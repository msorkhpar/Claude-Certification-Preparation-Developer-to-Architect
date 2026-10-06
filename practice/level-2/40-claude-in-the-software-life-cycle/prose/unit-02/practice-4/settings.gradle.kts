rootProject.name = "pipeline_setup"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":40-workflow-lint")
project(":40-workflow-lint").projectDir = file("libs/40-workflow-lint")
