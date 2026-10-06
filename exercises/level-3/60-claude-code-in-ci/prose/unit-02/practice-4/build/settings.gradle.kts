rootProject.name = "ci_review"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":40-workflow-lint")
project(":40-workflow-lint").projectDir = file("libs/40-workflow-lint")
include(":60-ci-gate")
project(":60-ci-gate").projectDir = file("libs/60-ci-gate")
