import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class McpSetupTest {
    // the folder that holds .mcp.json, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final List<String> AGENT_FILES = List.of(".claude/agents/explorer.md", ".claude/agents/scaffolder.md");
    private static final Pattern SECRET_KEY = Pattern.compile("token|key|secret|authorization", Pattern.CASE_INSENSITIVE);

    private record Agent(String description, List<String> tools) {}

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode loadJson(String rel) {
        try {
            return new ObjectMapper().readTree(read(rel));
        } catch (IOException e) {
            throw new AssertionError(rel + " is not valid JSON: " + e.getMessage());
        }
    }

    private static String[] frontMatter(String text) {
        Matcher head = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(text);
        return head.find() ? new String[] {head.group(1), text.substring(head.end())} : new String[] {"", text};
    }

    private static Agent agent(String rel) {
        String head = frontMatter(read(rel))[0];
        Matcher description = Pattern.compile("^description:\\s*(.*)$", Pattern.MULTILINE).matcher(head);
        Matcher tools = Pattern.compile("^tools:\\s*(.*)$", Pattern.MULTILINE).matcher(head);
        return new Agent(description.find() ? description.group(1).strip() : "",
            tools.find() ? Stream.of(tools.group(1).split(",")).map(String::strip).toList() : null);
    }

    private static JsonNode servers() {
        return loadJson(".mcp.json").path("mcpServers");
    }

    private static List<String> rules(String kind) {
        List<String> out = new ArrayList<>();
        loadJson(".claude/settings.json").path("permissions").path(kind).forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    void m1_everyMcpToolReferenceBelongsToAConfiguredServer() {
        JsonNode configured = servers();
        assertTrue(configured.size() > 0, "configure at least one server in .mcp.json");
        List<String> refs = new ArrayList<>();
        for (String rel : AGENT_FILES) {
            List<String> tools = agent(rel).tools();
            if (tools != null) tools.stream().filter(t -> t.startsWith("mcp__")).forEach(refs::add);
        }
        for (String kind : List.of("allow", "deny")) rules(kind).stream().filter(r -> r.startsWith("mcp__")).forEach(refs::add);
        assertFalse(refs.isEmpty(), "the setup refers to at least one MCP tool");
        for (String ref : refs) assertTrue(configured.has(ref.split("__")[1]), ref + " names a server that .mcp.json does not configure");
    }

    @Test
    void e1_credentialsComeFromTheEnvironmentAndTheTokenHasNoDefault() {
        JsonNode configured = servers();
        configured.fields().forEachRemaining(server -> {
            for (String field : List.of("headers", "env")) {
                server.getValue().path(field).fields().forEachRemaining(kv -> assertTrue(!SECRET_KEY.matcher(kv.getKey()).find() || kv.getValue().asText().contains("${"),
                    server.getKey() + " " + field + "." + kv.getKey() + " holds a literal credential"));
            }
        });
        String auth = configured.path("tickets").path("headers").path("Authorization").asText("");
        assertTrue(Pattern.compile("\\$\\{[A-Z_][A-Z0-9_]*\\}").matcher(auth).find(), "the tickets token is a ${VAR} reference");
        assertFalse(auth.contains(":-"), "a default for a token would be a credential in the file");
    }

    @Test
    void e2_theExplorerIsReadOnlyAndSaysWhenToUseIt() {
        Agent explorer = agent(".claude/agents/explorer.md");
        assertTrue(explorer.description().startsWith("Use when"), "the description says when to delegate");
        assertNotNull(explorer.tools(), "list the tools: without a tools line the subagent inherits every tool");
        assertTrue(explorer.tools().contains("Read") && explorer.tools().contains("Grep"), "an explorer reads and searches");
        assertEquals(List.of(), explorer.tools().stream().filter(t -> !List.of("Read", "Grep", "Glob", "mcp__docs__search").contains(t)).toList(), "read-only tools only");
    }

    @Test
    void e3_theScaffolderWritesOnlyInTheGeneratedFolder() {
        List<String> tools = agent(".claude/agents/scaffolder.md").tools();
        assertNotNull(tools, "list the tools: without a tools line the subagent inherits every tool");
        assertTrue(tools.contains("Edit") && !tools.contains("Bash"), "the scaffolder edits files and runs no commands");
        List<String> writes = rules("allow").stream().filter(r -> Pattern.compile("^(Edit|Write|MultiEdit)\\b").matcher(r).find()).toList();
        assertFalse(writes.isEmpty(), "allow the scaffolder to edit the generated folder");
        for (String rule : writes) assertTrue(rule.matches("Edit\\(src/generated/[^)]*\\)"), rule + " is not an Edit rule limited to the generated folder (a Write path rule is never consulted)");
    }

    @Test
    void e4_theTicketsServerIsReadOnlyForAgents() {
        List<String> deny = rules("deny");
        for (String tool : List.of("mcp__tickets__create_ticket", "mcp__tickets__delete_ticket")) assertTrue(deny.contains(tool), "deny " + tool);
        for (String rule : rules("allow")) {
            if (rule.startsWith("mcp__tickets")) assertTrue(rule.matches("mcp__tickets__(get|list|search)_\\w+"), rule + " approves more than reading tickets");
        }
    }

    @Test
    void e5_theEnvironmentFileIsDeniedAndNoWholeToolIsApproved() {
        assertTrue(rules("deny").contains("Read(./.env)"), "deny reading the environment file");
        assertEquals(List.of(), rules("allow").stream().filter(r -> List.of("Bash", "Bash(*)", "Edit", "Write", "Read", "mcp__tickets").contains(r)).toList(),
            "a bare allow rule approves every call of that tool");
    }

    @Test
    void e6_theTeamNoteListsEveryVariableAndSaysWhichSessionToStart() {
        String note = read("docs/team-setup.md");
        Set<String> vars = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\$\\{([A-Za-z_]\\w*)").matcher(read(".mcp.json"));
        while (m.find()) vars.add(m.group(1));
        for (String var : vars) assertTrue(Pattern.compile("\\b" + var + "\\b").matcher(note).find(), "the note does not list " + var);
        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : note.split("\n")) {
            String[] cells = line.strip().replaceAll("^\\||\\|$", "").split("\\|");
            if (line.startsWith("|") && cells.length >= 2 && List.of("resume", "fork", "fresh").contains(cells[1].strip().toLowerCase())) rows.put(cells[0].strip().toLowerCase(), cells[1].strip().toLowerCase());
        }
        String[][] expected = {{"yesterday", "resume"}, {"compare", "fork"}, {"rewritten", "fresh"}};
        for (String[] e : expected) {
            String situation = rows.keySet().stream().filter(k -> k.contains(e[0])).findFirst().orElse(null);
            assertNotNull(situation, "the table has no row for the '" + e[0] + "' situation");
            assertEquals(e[1], rows.get(situation), "'" + e[0] + "' should be " + e[1]);
        }
    }

    @Test
    void e7_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
        List<String> hits = new ArrayList<>();
        Pattern home = Pattern.compile("(/home/\\w+|/Users/\\w+|C:\\\\Users)");
        Pattern email = Pattern.compile("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+");
        Pattern key = Pattern.compile("sk-ant-[\\w-]{6,}");
        try (Stream<Path> s = Files.walk(ROOT)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                String text = Files.readString(p);
                String rel = ROOT.relativize(p).toString();
                if (home.matcher(text).find()) hits.add(rel + ": home path");
                if (email.matcher(text).find()) hits.add(rel + ": email address");
                if (key.matcher(text).find()) hits.add(rel + ": key");
            }
        }
        assertEquals(List.of(), hits);
    }
}
