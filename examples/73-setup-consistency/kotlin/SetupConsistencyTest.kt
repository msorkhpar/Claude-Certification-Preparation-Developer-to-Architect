import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SetupConsistencyTest {
    private fun project(dir: Path, mcp: String, settings: String, agentFiles: Map<String, String>): Path {
        Files.createDirectories(dir.resolve(".claude/agents"))
        Files.writeString(dir.resolve(".mcp.json"), "{\"mcpServers\": $mcp}")
        Files.writeString(dir.resolve(".claude/settings.json"), "{\"permissions\": $settings}")
        for ((name, text) in agentFiles) Files.writeString(dir.resolve(".claude/agents/$name.md"), text)
        return dir
    }

    @Test
    fun theFlawedProjectHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(listOf("literal-secret: tickets headers.Authorization", "unknown-server: wiki (explorer)", "agent-bare-bash: explorer",
            "agent-inherits-all: scaffolder", "env-readable", "bare-write-allowed"), audit(HERE.resolve("project-before")))
        assertEquals(listOf<String>(), audit(HERE.resolve("project-after")))
    }

    @Test
    fun aSubagentWithoutAToolsLineInheritsEverythingAndIsNotAnEmptyList() {
        val found = agents(HERE.resolve("project-before"))
        assertNull(found["scaffolder"])
        assertEquals(listOf("Read", "Grep", "Bash", "mcp__wiki__search"), found["explorer"])
    }

    @Test
    fun onlyAValueWithNoEnvironmentReferenceIsALiteralSecret() {
        val servers = ObjectMapper().readTree("{\"a\": {\"headers\": {\"Authorization\": \"Bearer \${TOKEN}\", \"X-Trace\": \"abc\"}}, \"b\": {\"env\": {\"API_KEY\": \"abc\", \"REGION\": \"eu\"}}}")
        assertEquals(listOf("b env.API_KEY"), literalSecrets(servers))
    }

    @Test
    fun aPermissionRuleForAServerThatIsNotConfiguredIsFound(@TempDir dir: Path) {
        val root = project(dir, "{\"docs\": {\"type\": \"stdio\", \"command\": \"x\"}}", "{\"allow\": [\"mcp__ghost__read\"], \"deny\": [\"Read(./.env)\"]}",
            mapOf("a" to "---\nname: a\ndescription: Use when.\ntools: Read\n---\nbody\n"))
        assertEquals(listOf("unknown-server: ghost (settings)"), audit(root))
        assertEquals(mapOf("a" to listOf("Read")), load(root).agents)
    }
}
