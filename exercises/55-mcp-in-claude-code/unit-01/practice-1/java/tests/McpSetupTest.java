import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class McpSetupTest {
    // config.dir is the project folder that holds .mcp.json and .claude/: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final List<String> SERVERS = List.of("core", "docs", "github", "schema");

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> readJson(String rel) {
        Map<String, Object> data;
        try {
            data = McpConfig.parse(read(rel));
        } catch (IllegalArgumentException error) {
            throw new AssertionError(rel + " is not valid JSON: " + error.getMessage());
        }
        assertNotNull(data, rel + " must hold a JSON object");
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    private static Map<String, Object> shared() {
        return asMap(readJson(".mcp.json").get("mcpServers"));
    }

    private static Map<String, Object> server(String name) {
        Object found = shared().get(name);
        assertTrue(found instanceof Map, ".mcp.json has no server named " + name);
        return asMap(found);
    }

    private static Map<String, Object> wrapped(Map<String, Object> servers) {
        return Map.of("mcpServers", servers);
    }

    @Test
    void m1_theSharedFileDeclaresTheFourTeamServersWithTheRightShape() {
        assertEquals(SERVERS, new ArrayList<>(new TreeSet<>(shared().keySet())), "declare exactly core, docs, github and schema");
        assertTrue("http".equals(server("github").get("type")) && "http".equals(server("core").get("type")), "github and core are remote http servers");
        assertTrue("stdio".equals(server("docs").getOrDefault("type", "stdio")) && "stdio".equals(server("schema").getOrDefault("type", "stdio")), "docs and schema run as local processes");
        assertTrue("python3".equals(server("docs").get("command")) && "python3".equals(server("schema").get("command")));
        assertEquals(List.of(), McpConfig.lint(wrapped(shared())));
    }

    @Test
    void e1_credentialsAreReadFromTheEnvironmentAndNeverWrittenInTheFile() {
        assertEquals("Bearer ${GITHUB_TOKEN}", asMap(server("github").get("headers")).get("Authorization"), "send the token as Bearer ${GITHUB_TOKEN}");
        assertEquals("${DOCS_API_KEY}", asMap(server("docs").get("env")).get("DOCS_API_KEY"), "pass DOCS_API_KEY through from the environment");
        Set<String> names = new TreeSet<>();
        Matcher m = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::-[^}]*)?\\}").matcher(McpConfig.show(server("github").getOrDefault("headers", Map.of())));
        while (m.find()) names.add(m.group(1));
        names.retainAll(McpConfig.COVERED);
        assertTrue(names.isEmpty(), "a credential name that Claude Code reads as empty toward a remote server cannot carry the token");
        for (Object entry : shared().values()) {
            for (String field : List.of("headers", "env")) {
                for (Map.Entry<String, Object> kv : asMap(asMap(entry).get(field)).entrySet()) {
                    String value = (String) kv.getValue();
                    assertTrue(Pattern.compile("\\$\\{[A-Za-z_]\\w*\\}").matcher(value).find() && !value.contains(":-"), kv.getKey() + ": no default value for a credential, it would be committed");
                }
            }
        }
        McpConfig.ExpandedServer out = McpConfig.expandServer(server("github"), Map.of("GITHUB_TOKEN", "t"));
        assertEquals("Bearer t", asMap(out.entry().get("headers")).get("Authorization"));
        assertEquals(List.of(), out.warnings());
    }

    @Test
    void e2_endpointsAndPathsThatAreNotSecretHaveADefaultSoTheFileWorksUnset() {
        for (String name : List.of("github", "core")) {
            assertTrue(String.valueOf(server(name).getOrDefault("url", "")).matches("\\$\\{[A-Z_]+:-https://[^}]+\\}"), name + ": url needs a ${VAR:-default} with an https default");
        }
        for (String name : List.of("docs", "schema")) {
            List<Object> args = asList(server(name).get("args"));
            assertTrue(!args.isEmpty() && ((String) args.get(0)).startsWith("${CLAUDE_PROJECT_DIR:-.}/"), name + ": CLAUDE_PROJECT_DIR is set for the server, not for the command, so it needs a default");
            McpConfig.ExpandedServer out = McpConfig.expandServer(server(name), Map.of());
            assertTrue(out.warnings().stream().noneMatch(w -> w.contains("CLAUDE_PROJECT_DIR")) && ((String) asList(out.entry().get("args")).get(0)).startsWith("./tools/"));
        }
        String url = (String) McpConfig.expandServer(server("github"), Map.of()).entry().get("url");
        assertTrue(url.startsWith("https://") && !url.contains("${"));
    }

    @Test
    void e3_onlyTheSmallCoreServerIsLoadedAtTheStart() {
        assertEquals(Boolean.TRUE, server("core").get("alwaysLoad"), "core is used on every turn: alwaysLoad true");
        List<String> others = SERVERS.stream().filter(n -> !n.equals("core") && Boolean.TRUE.equals(asMap(shared().get(n)).get("alwaysLoad"))).toList();
        assertEquals(List.of(), others, "tool search should find the others on demand");
    }

    @Test
    void e4_sharedServersStayInTheProjectFileAndPersonalOnesInTheUserScopeFile() {
        Map<String, Object> user = asMap(readJson("user-scope.example.json").get("mcpServers"));
        assertEquals(List.of("scratch"), new ArrayList<>(user.keySet()), "the user-scope example holds one personal server, scratch");
        assertFalse(shared().containsKey("scratch"), "a personal server does not belong in the committed file");
        assertTrue(user.keySet().stream().noneMatch(shared()::containsKey), "the same name in two scopes: the whole entry of the higher scope wins and a warning is shown");
        Map<String, Map<String, Map<String, Object>>> scopes = new LinkedHashMap<>();
        scopes.put("project", castServers(shared()));
        scopes.put("user", castServers(user));
        McpConfig.Resolved resolved = McpConfig.resolveServers(scopes);
        List<String> expected = new ArrayList<>(SERVERS);
        expected.add("scratch");
        assertEquals(new TreeSet<>(expected), new TreeSet<>(resolved.servers().keySet()));
        assertEquals(List.of(), resolved.warnings());
        assertTrue(user.values().stream().noneMatch(e -> Pattern.compile("/home/|/Users/").matcher(McpConfig.show(e)).find()), "no absolute home path: use ${HOME}");
    }

    private static Map<String, Map<String, Object>> castServers(Map<String, Object> servers) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        servers.forEach((k, v) -> out.put(k, asMap(v)));
        return out;
    }

    @Test
    void e5_permissionsAllowTheReadOnlyServersByNameAndDenyTheDestructiveTool() {
        Map<String, Object> settings = readJson(".claude/settings.json");
        String[][] table = {{"mcp__docs__search_docs", "allow"}, {"mcp__schema__read_schema", "allow"}, {"mcp__github__list_pull_requests", "ask"}, {"mcp__github__create_issue", "ask"},
            {"mcp__github__delete_repository", "deny"}, {"mcp__core__ping", "ask"}, {"mcp__scratch__anything", "ask"}};
        List<String> wrong = new ArrayList<>();
        for (String[] row : table) {
            String got = McpConfig.mcpDecision(settings, row[0]);
            if (!got.equals(row[1])) wrong.add(row[0] + ": got " + got + ", want " + row[1]);
        }
        assertEquals(List.of(), wrong);
        List<Object> rules = asList(asMap(settings.get("permissions")).get("allow"));
        assertTrue(rules.stream().map(String::valueOf).noneMatch(r -> r.equals("*") || r.equals("mcp__*") || r.startsWith("mcp__*")), "an allow rule must name its server; mcp__* is ignored");
    }

    @Test
    void e6_theToolDescriptionSaysWhenToUseItInsteadOfGrepAndFitsTheLimit() {
        List<Object> tools = asList(readJson("docs/tool-descriptions.json").get("tools"));
        List<Map<String, Object>> found = tools.stream().map(McpSetupTest::asMap).filter(t -> "search_docs".equals(t.get("name"))).toList();
        assertFalse(found.isEmpty(), "describe the search_docs tool");
        String text = String.valueOf(found.get(0).getOrDefault("description", ""));
        assertTrue(text.length() <= 2048 && McpConfig.truncate(text).equals(text), "Claude Code cuts a description at 2,048 characters");
        assertTrue(Pattern.compile("instead of Grep").matcher(text.substring(0, Math.min(300, text.length()))).find(), "put the boundary against Grep in the first 300 characters");
        assertTrue(text.contains("Returns") || text.contains("returns"), "say what comes back");
        assertTrue(Pattern.compile("\\b(does not|doesn't|not search)\\b").matcher(text).find(), "say what it does not do");
        Map<String, Object> props = asMap(asMap(found.get(0).get("inputSchema")).get("properties"));
        assertTrue(!props.isEmpty() && props.values().stream().allMatch(p -> !String.valueOf(asMap(p).getOrDefault("description", "")).isEmpty()), "every parameter needs a description");
        assertEquals(List.of("query"), asMap(found.get(0).get("inputSchema")).get("required"));
    }

    @Test
    void e7_theNotesListEveryServerWithItsScopeAndReadTheCatalogAsAResource() {
        String notes = read("docs/mcp-servers.md");
        for (String name : SERVERS) {
            Matcher row = Pattern.compile("^\\|\\s*`" + name + "`\\s*\\|\\s*(\\w+)\\s*\\|(.*)$", Pattern.MULTILINE).matcher(notes);
            assertTrue(row.find(), "add a table row for " + name);
            assertEquals("project", row.group(1), name + " is shared, so its scope is project");
        }
        List<String[]> refs = new ArrayList<>();
        Matcher r = Pattern.compile("@([\\w-]+):(\\w+://[\\w./-]+)").matcher(notes);
        while (r.find()) refs.add(new String[] {r.group(1), r.group(2)});
        assertTrue(!refs.isEmpty() && refs.stream().allMatch(ref -> SERVERS.contains(ref[0])), "@server:protocol://path must name a configured server");
        assertTrue(refs.stream().anyMatch(ref -> ref[0].equals("schema") && ref[1].equals("schema://orders")), "show how to read the orders schema");
        Matcher schemaRow = Pattern.compile("^\\|\\s*`schema`.*$", Pattern.MULTILINE).matcher(notes);
        assertTrue(schemaRow.find() && schemaRow.group().toLowerCase().contains("resource"), "the schema server exposes resources");
    }

    @Test
    void e8_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
        List<String> hits = new ArrayList<>();
        String[][] patterns = {{"home path", "(/home/\\w+|/Users/\\w+|C:\\\\Users)"}, {"email address", "[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+"},
            {"key", "(sk-ant-[\\w-]{6,}|ghp_\\w{6,}|Bearer [A-Za-z0-9]{12,})"}};
        try (Stream<Path> walk = Files.walk(ROOT)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList())) {
                String text = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
                for (String[] p : patterns) if (Pattern.compile(p[1]).matcher(text).find()) hits.add(ROOT.relativize(path) + ": " + p[0]);
            }
        }
        assertEquals(List.of(), hits);
    }
}
