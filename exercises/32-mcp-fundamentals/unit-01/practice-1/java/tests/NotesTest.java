import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

// The solution is compiled as the main source set (-Psolution=...); each test starts it as a separate process over stdio.
class NotesTest {
    /** The value of a call, or the error it threw. */
    record Att<T>(T value, Throwable error) {
        boolean ok() { return error == null; }
    }

    static <T> Att<T> attempt(Supplier<T> call) {
        try {
            return new Att<>(call.get(), null);
        } catch (Throwable e) {
            return new Att<>(null, e);
        }
    }

    /** Start the solution as a server over stdio, connect the SDK client to it and run steps. */
    static <T> T session(Function<McpSyncClient, T> steps) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ServerParameters params = ServerParameters.builder(java).args("-cp", System.getProperty("server.classpath"), "NotesServer").build();
        McpSyncClient client = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper())).requestTimeout(Duration.ofSeconds(30)).build();
        try {
            client.initialize();
            return steps.apply(client);
        } finally {
            client.closeGracefully();
        }
    }

    static String text(McpSchema.CallToolResult r) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content c : r.content()) if (c instanceof McpSchema.TextContent t) sb.append(t.text());
        return sb.toString();
    }

    static String text(McpSchema.ReadResourceResult r) {
        return ((McpSchema.TextResourceContents) r.contents().get(0)).text();
    }

    static boolean isError(McpSchema.CallToolResult r) { return Boolean.TRUE.equals(r.isError()); }

    static Att<McpSchema.CallToolResult> call(McpSyncClient c, String tool, Map<String, Object> args) {
        return attempt(() -> c.callTool(new McpSchema.CallToolRequest(tool, args)));
    }

    static Att<McpSchema.CallToolResult> add(McpSyncClient c, String title, String body) { return call(c, "add_note", Map.of("title", title, "text", body)); }

    static Att<McpSchema.CallToolResult> add(McpSyncClient c, String title) { return add(c, title, "body"); }

    static Att<McpSchema.CallToolResult> search(McpSyncClient c, Map<String, Object> args) { return call(c, "search_notes", args); }

    static Att<McpSchema.ReadResourceResult> read(McpSyncClient c, String uri) { return attempt(() -> c.readResource(new McpSchema.ReadResourceRequest(uri))); }

    static String saved(Att<McpSchema.CallToolResult> a) { return a.ok() ? text(a.value()) : String.valueOf(a.error()); }

    static String readText(Att<McpSchema.ReadResourceResult> a) { return a.ok() ? text(a.value()) : String.valueOf(a.error()); }

    @Test
    void m1_aClientCanSaveANoteFindItAndReadItBack() {
        record Out(McpSchema.Implementation info, Att<McpSchema.CallToolResult> saved, Att<McpSchema.CallToolResult> found, Att<McpSchema.ReadResourceResult> got) {}
        Out o = session(c -> {
            var saved = add(c, "Plan", "ship it");
            return new Out(c.getServerInfo(), saved, search(c, Map.of("query", "ship")), read(c, "notes://note/1"));
        });
        assertEquals("notes", o.info().name());
        assertEquals("1.0.0", o.info().version());
        assertEquals("Saved note 1: Plan", saved(o.saved()));
        assertFalse(o.saved().ok() && isError(o.saved().value()));
        assertEquals("1. Plan", saved(o.found()));
        assertEquals("Plan\n\nship it", readText(o.got()));
    }

    @Test
    void e1_theServerDeclaresToolsResourcesAndPromptsAndNamesItsTools() {
        record Out(McpSchema.ServerCapabilities caps, Att<McpSchema.ListToolsResult> listed) {}
        Out o = session(c -> new Out(c.getServerCapabilities(), attempt(c::listTools)));
        assertTrue(o.caps().tools() != null && o.caps().resources() != null && o.caps().prompts() != null);
        assertTrue(o.listed().ok(), String.valueOf(o.listed().error()));
        var tools = new java.util.TreeMap<String, McpSchema.Tool>();
        o.listed().value().tools().forEach(t -> tools.put(t.name(), t));
        assertEquals(List.of("add_note", "search_notes"), List.copyOf(tools.keySet()));
        assertTrue(tools.values().stream().allMatch(t -> t.description() != null && !t.description().isEmpty()));
        assertEquals(List.of("title", "text"), tools.get("add_note").inputSchema().get("required"));
        assertEquals(List.of("query"), tools.get("search_notes").inputSchema().get("required"));
        Map<?, ?> limit = (Map<?, ?>) ((Map<?, ?>) tools.get("search_notes").inputSchema().get("properties")).get("limit");
        assertEquals("integer", limit.get("type"));
        assertEquals(5, ((Number) limit.get("default")).intValue());
    }

    @Test
    void e2_badInputComesBackAsAToolErrorTheModelCanRead() {
        List<Att<McpSchema.CallToolResult>> r = session(c -> List.of(add(c, "  ", "x"), add(c, "T", "   "), add(c, "T", "x".repeat(501)), add(c, "T", "x".repeat(500)),
                search(c, Map.of("query", " ")), search(c, Map.of("query", "x", "limit", 0)), search(c, Map.of("query", "x", "limit", 21))));
        String[] messages = {"title is required", "text is required", "text is too long (max 500)", null, "query is required", "limit must be between 1 and 20", "limit must be between 1 and 20"};
        for (int i = 0; i < messages.length; i++) {
            assertTrue(r.get(i).ok(), String.valueOf(r.get(i).error()));
            if (messages[i] == null) {
                assertFalse(isError(r.get(i).value()));
                assertEquals("Saved note 1: T", text(r.get(i).value()));
            } else {
                assertTrue(isError(r.get(i).value()), messages[i]);
                assertTrue(text(r.get(i).value()).contains(messages[i]), text(r.get(i).value()));
            }
        }
    }

    @Test
    void e3_searchIgnoresCaseKeepsIdOrderHonoursTheLimitAndSaysWhenNothingMatches() {
        List<Att<McpSchema.CallToolResult>> r = session(c -> {
            add(c, "Alpha", "first");
            add(c, "beta", "ALPHA again");
            add(c, "Gamma", "third");
            add(c, "alphabet", "soup");
            return List.of(search(c, Map.of("query", "ALPHA")), search(c, Map.of("query", "alpha", "limit", 2)), search(c, Map.of("query", "  zeta ")), search(c, Map.of("query", "third", "limit", 20)));
        });
        assertEquals("1. Alpha\n2. beta\n4. alphabet", saved(r.get(0)));
        assertEquals("1. Alpha\n2. beta", saved(r.get(1)));
        assertEquals("No notes match \"zeta\"", saved(r.get(2)));
        assertFalse(r.get(2).ok() && isError(r.get(2).value()));
        assertEquals("3. Gamma", saved(r.get(3)));
    }

    @Test
    void e4_toolAnnotationsTellAClientWhichToolOnlyReads() {
        Att<McpSchema.ListToolsResult> listed = session(c -> attempt(c::listTools));
        assertTrue(listed.ok(), String.valueOf(listed.error()));
        var annotations = new java.util.HashMap<String, McpSchema.ToolAnnotations>();
        listed.value().tools().forEach(t -> annotations.put(t.name(), t.annotations()));
        assertNotNull(annotations.get("search_notes"));
        assertEquals(Boolean.TRUE, annotations.get("search_notes").readOnlyHint());
        assertNotNull(annotations.get("add_note"));
        assertEquals(Boolean.FALSE, annotations.get("add_note").readOnlyHint());
        assertEquals(Boolean.FALSE, annotations.get("add_note").destructiveHint());
        assertEquals(Boolean.FALSE, annotations.get("add_note").idempotentHint());
    }

    @Test
    void e5_resourcesGiveTheCountANoteByIdAndAnErrorForAMissingOne() {
        record Out(Att<McpSchema.ListResourcesResult> resources, Att<McpSchema.ListResourceTemplatesResult> templates, List<Att<McpSchema.ReadResourceResult>> counts,
                   Att<McpSchema.ReadResourceResult> second, List<Att<McpSchema.ReadResourceResult>> bad) {}
        Out o = session(c -> {
            var resources = attempt(c::listResources);
            var templates = attempt(c::listResourceTemplates);
            var zero = read(c, "notes://count");
            add(c, "One");
            var one = read(c, "notes://count");
            add(c, "Two");
            var two = read(c, "notes://count");
            return new Out(resources, templates, List.of(zero, one, two), read(c, "notes://note/2"), List.of(read(c, "notes://note/3"), read(c, "notes://note/0"), read(c, "notes://note/abc")));
        });
        assertTrue(o.resources().ok(), String.valueOf(o.resources().error()));
        assertEquals(List.of("notes://count"), o.resources().value().resources().stream().map(McpSchema.Resource::uri).toList());
        assertTrue(o.templates().ok(), String.valueOf(o.templates().error()));
        assertEquals(List.of("notes://note/{id}"), o.templates().value().resourceTemplates().stream().map(McpSchema.ResourceTemplate::uriTemplate).toList());
        assertEquals(List.of("0 notes", "1 note", "2 notes"), o.counts().stream().map(NotesTest::readText).toList());
        assertEquals("Two\n\nbody", readText(o.second()));
        for (var bad : o.bad()) {
            assertFalse(bad.ok());
            assertTrue(String.valueOf(bad.error().getMessage()).contains("No note"), String.valueOf(bad.error()));
        }
    }

    @Test
    void e6_thePromptListsTheNotesAndDefaultsTheTone() {
        record Out(Att<McpSchema.ListPromptsResult> listed, Att<McpSchema.GetPromptResult> empty, Att<McpSchema.GetPromptResult> byDefault, Att<McpSchema.GetPromptResult> formal) {}
        Out o = session(c -> {
            var listed = attempt(c::listPrompts);
            var empty = attempt(() -> c.getPrompt(new McpSchema.GetPromptRequest("review_notes", Map.of())));
            add(c, "Alpha");
            add(c, "Beta");
            return new Out(listed, empty, attempt(() -> c.getPrompt(new McpSchema.GetPromptRequest("review_notes", Map.of()))),
                    attempt(() -> c.getPrompt(new McpSchema.GetPromptRequest("review_notes", Map.of("tone", "formal")))));
        });
        assertTrue(o.listed().ok(), String.valueOf(o.listed().error()));
        assertEquals(List.of("review_notes"), o.listed().value().prompts().stream().map(McpSchema.Prompt::name).toList());
        McpSchema.PromptArgument argument = o.listed().value().prompts().get(0).arguments().get(0);
        assertEquals("tone", argument.name());
        assertFalse(Boolean.TRUE.equals(argument.required()));
        assertTrue(o.empty().ok(), String.valueOf(o.empty().error()));
        assertEquals("There are no notes to review.", ((McpSchema.TextContent) o.empty().value().messages().get(0).content()).text());
        assertTrue(o.byDefault().ok(), String.valueOf(o.byDefault().error()));
        assertEquals(1, o.byDefault().value().messages().size());
        assertEquals(McpSchema.Role.USER, o.byDefault().value().messages().get(0).role());
        assertEquals("Review these notes in a brief tone:\n- Alpha\n- Beta", ((McpSchema.TextContent) o.byDefault().value().messages().get(0).content()).text());
        assertTrue(o.formal().ok(), String.valueOf(o.formal().error()));
        assertTrue(((McpSchema.TextContent) o.formal().value().messages().get(0).content()).text().contains("in a formal tone"));
    }

    @Test
    void e7_idsAreSequentialAndAFailedCallDoesNotUseOne() {
        record Out(List<Att<McpSchema.CallToolResult>> calls, Att<McpSchema.ReadResourceResult> one, Att<McpSchema.ReadResourceResult> count) {}
        Out o = session(c -> new Out(List.of(add(c, "  First  ", "a"), add(c, "", "b"), add(c, "First", "c"), add(c, "Third", "   "), add(c, "Fourth", "d")),
                read(c, "notes://note/1"), read(c, "notes://count")));
        assertEquals("Saved note 1: First", saved(o.calls().get(0)));
        assertTrue(o.calls().get(1).ok() && isError(o.calls().get(1).value()));
        assertEquals("Saved note 2: First", saved(o.calls().get(2)));
        assertTrue(o.calls().get(3).ok() && isError(o.calls().get(3).value()));
        assertEquals("Saved note 3: Fourth", saved(o.calls().get(4)));
        assertEquals("First\n\na", readText(o.one()));
        assertEquals("3 notes", readText(o.count()));
    }
}
