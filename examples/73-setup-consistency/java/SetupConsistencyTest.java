import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupConsistencyTest {
    private static Path project(Path dir, String mcp, String settings, Map<String, String> agentFiles) throws IOException {
        Files.createDirectories(dir.resolve(".claude/agents"));
        Files.writeString(dir.resolve(".mcp.json"), "{\"mcpServers\": " + mcp + "}");
        Files.writeString(dir.resolve(".claude/settings.json"), "{\"permissions\": " + settings + "}");
        for (var e : agentFiles.entrySet()) Files.writeString(dir.resolve(".claude/agents/" + e.getKey() + ".md"), e.getValue());
        return dir;
    }

    @Test
    void theFlawedProjectHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(List.of("literal-secret: tickets headers.Authorization", "unknown-server: wiki (explorer)", "agent-bare-bash: explorer",
            "agent-inherits-all: scaffolder", "env-readable", "bare-write-allowed"), SetupConsistency.audit(SetupConsistency.HERE.resolve("project-before")));
        assertEquals(List.of(), SetupConsistency.audit(SetupConsistency.HERE.resolve("project-after")));
    }

    @Test
    void aSubagentWithoutAToolsLineInheritsEverythingAndIsNotAnEmptyList() {
        var found = SetupConsistency.agents(SetupConsistency.HERE.resolve("project-before"));
        assertNull(found.get("scaffolder"));
        assertEquals(List.of("Read", "Grep", "Bash", "mcp__wiki__search"), found.get("explorer"));
    }

    @Test
    void onlyAValueWithNoEnvironmentReferenceIsALiteralSecret() throws IOException {
        var servers = new ObjectMapper().readTree("{\"a\": {\"headers\": {\"Authorization\": \"Bearer ${TOKEN}\", \"X-Trace\": \"abc\"}}, \"b\": {\"env\": {\"API_KEY\": \"abc\", \"REGION\": \"eu\"}}}");
        assertEquals(List.of("b env.API_KEY"), SetupConsistency.literalSecrets(servers));
    }

    @Test
    void aPermissionRuleForAServerThatIsNotConfiguredIsFound(@TempDir Path dir) throws IOException {
        Path root = project(dir, "{\"docs\": {\"type\": \"stdio\", \"command\": \"x\"}}", "{\"allow\": [\"mcp__ghost__read\"], \"deny\": [\"Read(./.env)\"]}",
            Map.of("a", "---\nname: a\ndescription: Use when.\ntools: Read\n---\nbody\n"));
        assertEquals(List.of("unknown-server: ghost (settings)"), SetupConsistency.audit(root));
        assertEquals(Map.of("a", List.of("Read")), SetupConsistency.load(root).agents());
    }
}
