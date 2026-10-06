import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * One file, two roles: run with --serve it is an MCP server over stdio; run alone it starts itself as a server and talks to it twice.
 *
 * <p>First through the SDK client (what a host application does), then by hand with raw JSON-RPC lines (what the SDK hides). Everything is
 * local: the server is a child process and the two sides talk through pipes. `io.modelcontextprotocol.sdk:mcp` 2.0.1, checked on 2026-10-04.
 */
public final class NotesExample {
    private static final System.Logger LOG = System.getLogger(NotesExample.class.getName());
    /** The server's state and the four things it offers. The tool schemas are written out: the Java SDK does not derive them from code. */
    static final class Notes {
        final List<String> titles = new ArrayList<>();

        SyncToolSpecification addNote() {
            Map<String, Object> schema = Map.of("type", "object", "properties", props("title", "string", "text", "string"), "required", List.of("title", "text"));
            McpSchema.Tool tool = McpSchema.Tool.builder("add_note", schema).description("Save a note.").annotations(McpSchema.ToolAnnotations.builder().readOnlyHint(false).build()).build();
            return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
                String title = String.valueOf(request.arguments().get("title")).strip();
                if (title.isEmpty()) return result("title is required", true); // an expected failure: the model reads the message and can retry
                titles.add(title);
                System.err.println("saved note " + titles.size()); // stderr is for logs; stdout carries the protocol
                return result("Saved note " + titles.size() + ": " + title, false);
            }).build();
        }

        SyncToolSpecification searchNotes() {
            Map<String, Object> limit = new LinkedHashMap<>(Map.of("type", "integer"));
            limit.put("default", 5);
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("query", Map.of("type", "string"));
            properties.put("limit", limit);
            Map<String, Object> schema = Map.of("type", "object", "properties", properties, "required", List.of("query"));
            McpSchema.Tool tool = McpSchema.Tool.builder("search_notes", schema).description("Find notes by a word in the title.").annotations(McpSchema.ToolAnnotations.builder().readOnlyHint(true).build()).build();
            return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
                String query = String.valueOf(request.arguments().get("query")).toLowerCase();
                int max = request.arguments().get("limit") instanceof Number n ? n.intValue() : 5;
                List<String> hits = new ArrayList<>();
                for (int i = 0; i < titles.size(); i++) if (titles.get(i).toLowerCase().contains(query)) hits.add((i + 1) + ". " + titles.get(i));
                String text = String.join("\n", hits.subList(0, Math.min(max, hits.size())));
                return result(text.isEmpty() ? "No notes match \"" + request.arguments().get("query") + "\"" : text, false);
            }).build();
        }

        SyncResourceSpecification count() {
            McpSchema.Resource resource = McpSchema.Resource.builder("notes://count", "count").mimeType("text/plain").build();
            return new SyncResourceSpecification(resource, (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", titles.size() + " note" + (titles.size() == 1 ? "" : "s"), null))));
        }

        SyncPromptSpecification reviewNotes() {
            McpSchema.Prompt prompt = new McpSchema.Prompt("review_notes", null, List.of(new McpSchema.PromptArgument("tone", null, false)));
            return new SyncPromptSpecification(prompt, (exchange, request) -> {
                Object tone = request.arguments() == null ? null : request.arguments().get("tone");
                StringBuilder text = new StringBuilder("Review these notes in a " + (tone == null ? "brief" : tone) + " tone:");
                for (String t : titles) text.append("\n- ").append(t);
                return new McpSchema.GetPromptResult(null, List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text.toString()))));
            });
        }
    }

    static Map<String, Object> props(String... nameAndType) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < nameAndType.length; i += 2) out.put(nameAndType[i], Map.of("type", nameAndType[i + 1]));
        return out;
    }

    static McpSchema.CallToolResult result(String text, boolean isError) {
        return McpSchema.CallToolResult.builder().addTextContent(text).isError(isError).build();
    }

    static void serve(Notes notes, StdioServerTransportProvider transport) {
        McpServer.sync(transport).serverInfo("notes", "1.0.0")
            .capabilities(McpSchema.ServerCapabilities.builder().tools(false).resources(false, false).prompts(false).build())
            .tools(notes.addNote(), notes.searchNotes()).resources(notes.count()).prompts(notes.reviewNotes()).build();
    }

    static String words(McpSchema.CallToolResult result) {
        StringBuilder out = new StringBuilder();
        for (McpSchema.Content c : result.content()) if (c instanceof McpSchema.TextContent t) out.append(t.text());
        return out.toString();
    }

    static String py(boolean value) {
        return value ? "True" : "False";
    }

    static void withSdkClient(List<String> command) {
        ServerParameters params = ServerParameters.builder(command.get(0)).args(command.subList(1, command.size())).build();
        McpSyncClient client = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper())).clientInfo(new McpSchema.Implementation("host", "1.0.0")).build();
        try {
            McpSchema.InitializeResult init = client.initialize();
            System.out.println("server: " + init.serverInfo().name() + " " + init.serverInfo().version());
            List<String> declared = new ArrayList<>();
            if (init.capabilities().tools() != null) declared.add("tools");
            if (init.capabilities().resources() != null) declared.add("resources");
            if (init.capabilities().prompts() != null) declared.add("prompts");
            System.out.println("declares: " + String.join(", ", declared));
            for (McpSchema.Tool tool : client.listTools().tools()) {
                @SuppressWarnings("unchecked") List<String> required = (List<String>) tool.inputSchema().get("required");
                @SuppressWarnings("unchecked") Map<String, Object> properties = (Map<String, Object>) tool.inputSchema().get("properties");
                List<String> args = new ArrayList<>();
                for (String p : properties.keySet()) args.add(required.contains(p) ? p : "[" + p + "]");
                System.out.println("tool: " + tool.name() + "(" + String.join(", ", args) + ") read-only hint " + py(tool.annotations().readOnlyHint()));
            }
            McpSchema.CallToolResult ok = client.callTool(new McpSchema.CallToolRequest("add_note", Map.of("title", "Plan", "text", "ship it")));
            System.out.println("add_note -> '" + words(ok) + "', isError " + py(Boolean.TRUE.equals(ok.isError())));
            McpSchema.CallToolResult bad = client.callTool(new McpSchema.CallToolRequest("add_note", Map.of("title", " ", "text", "x")));
            System.out.println("add_note with a blank title -> isError " + py(Boolean.TRUE.equals(bad.isError())) + ", says why: " + py(words(bad).contains("title is required")));
            System.out.println("search_notes -> '" + words(client.callTool(new McpSchema.CallToolRequest("search_notes", Map.of("query", "PLAN")))) + "'");
            McpSchema.TextResourceContents count = (McpSchema.TextResourceContents) client.readResource(new McpSchema.ReadResourceRequest("notes://count")).contents().get(0);
            System.out.println("resource notes://count -> " + count.text());
            McpSchema.PromptMessage message = client.getPrompt(new McpSchema.GetPromptRequest("review_notes", Map.of())).messages().get(0);
            System.out.println("prompt review_notes -> role " + message.role().name().toLowerCase() + ", '" + ((McpSchema.TextContent) message.content()).text().replace("\n", "\\n") + "'");
        } finally {
            client.closeGracefully();
        }
    }

    /** The same server, spoken to line by line: one JSON-RPC message per line on stdin and stdout, nothing else on stdout. */
    static void wire(List<String> command) throws IOException, InterruptedException {
        Process child = new ProcessBuilder(command).start();
        StringBuilder logs = new StringBuilder();
        Thread drain = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(child.getErrorStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = err.readLine()) != null; ) logs.append(line).append('\n');
            } catch (IOException ignored) {
            }
        });
        drain.start();
        BufferedWriter in = new BufferedWriter(new OutputStreamWriter(child.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader out = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8));
        ObjectMapper json = new ObjectMapper();
        int[] stray = {0};
        java.util.function.Consumer<String> send = line -> {
            try {
                in.write(line + "\n");
                in.flush();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
        java.util.function.IntFunction<JsonNode> receive = id -> {
            try {
                while (true) {
                    String line = out.readLine();
                    if (line == null) throw new IllegalStateException("the server closed stdout");
                    JsonNode message;
                    try {
                        message = json.readTree(line);
                    } catch (RuntimeException e) {
                        stray[0]++;
                        continue;
                    }
                    if (message.path("id").asInt(-1) == id) return message;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
        send.accept("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"wire\",\"version\":\"1\"}}}");
        JsonNode first = receive.apply(1).get("result");
        TreeSet<String> caps = new TreeSet<>();
        for (String k : List.of("tools", "resources", "prompts")) if (first.get("capabilities").has(k)) caps.add("'" + k + "'");
        System.out.println("-> initialize (id 1)      <- protocol " + first.get("protocolVersion").asString() + ", server " + first.get("serverInfo").get("name").asString() + ", capabilities [" + String.join(", ", caps) + "]");
        send.accept("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        send.accept("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        TreeSet<String> names = new TreeSet<>();
        receive.apply(2).get("result").get("tools").forEach(t -> names.add("'" + t.get("name").asString() + "'"));
        System.out.println("-> tools/list (id 2)      <- tools [" + String.join(", ", names) + "]");
        send.accept("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"add_note\",\"arguments\":{\"title\":\"Wire\",\"text\":\"by hand\"}}}");
        System.out.println("-> tools/call (id 3)      <-  " + receive.apply(3).get("result").get("content").get(0).get("text").asString());
        send.accept("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"search_notes\",\"arguments\":{\"query\":\"wire\",\"limit\":\"two\"}}}");
        JsonNode answer = receive.apply(4);
        System.out.println("-> tools/call with limit 'two' (id 4)   <- " + (answer.path("result").path("isError").asBoolean(false) ? "a result with isError" : answer.has("error") ? "a protocol error" : "a result"));
        in.close();
        if (!child.waitFor(10, TimeUnit.SECONDS)) child.destroyForcibly();
        drain.join(2000);
        System.out.println("lines on stdout that were not JSON: " + stray[0]);
        System.out.println("the server's own log went to stderr: " + py(logs.toString().contains("saved note 1")));
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--serve")) {
            serve(new Notes(), new StdioServerTransportProvider(McpJsonDefaults.getMapper()));
            return;
        }
        List<String> command = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", System.getProperty("java.class.path"), "NotesExample", "--serve");
        withSdkClient(command);
        System.out.println();
        wire(command);
    }
}
