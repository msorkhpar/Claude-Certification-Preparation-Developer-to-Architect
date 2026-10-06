rootProject.name = "mcp_setup"
include(":harness")
project(":harness").projectDir = file("libs/harness")
include(":55-mcp-config")
project(":55-mcp-config").projectDir = file("libs/55-mcp-config")
