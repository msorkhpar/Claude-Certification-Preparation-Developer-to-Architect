import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import reactor.core.publisher.Mono;

/**
 * A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.
 *
 * <p>The server and the client are `io.modelcontextprotocol.sdk:mcp` 2.0.1 (the server runs in an embedded Tomcat), with scripted callbacks in
 * place of a person and a model. This SDK speaks the 2025-11-25 revision, so the flow differs from the Python example next to it: there is an
 * `initialize` handshake and a session id, and the server calls the client in the middle of the request, as a request of its own on the
 * response stream. The log under the program output is what the client's transport sent and received, one JSON-RPC message at a time.
 * Checked on 2026-10-04 against the "Streamable HTTP" page of the MCP specification.
 */
public final class MrtrHttp {
    static final Map<String, Object> CONFIRM_SCHEMA = Map.of("type", "object", "properties", Map.of("confirm", Map.of("type", "boolean", "title", "Confirm the deployment")), "required", List.of("confirm"));

    static McpSchema.CallToolResult text(String text, boolean isError) {
        return McpSchema.CallToolResult.builder().addTextContent(text).isError(isError).build();
    }

    static SyncToolSpecification deploy() {
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("service", Map.of("type", "string"), "env", Map.of("type", "string")), "required", List.of("service", "env"));
        McpSchema.Tool tool = McpSchema.Tool.builder("deploy", schema).description("Deploy a service; production needs a person's confirmation.").build();
        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            String service = (String) request.arguments().get("service"), env = (String) request.arguments().get("env");
            if (env.equals("production")) {
                if (exchange.getClientCapabilities().elicitation() == null) return text("Deploying to production needs confirmation, and this client cannot be asked.", true);
                McpSchema.ElicitResult answer = exchange.createElicitation(new McpSchema.ElicitFormRequest("Deploy " + service + " to production?", CONFIRM_SCHEMA, null));
                if (answer.action() != McpSchema.ElicitResult.Action.ACCEPT || !Boolean.TRUE.equals(answer.content().get("confirm"))) return text("Deployment cancelled", false);
            }
            return text("Deployed " + service + " to " + env, false);
        }).build();
    }

    static SyncToolSpecification releaseNotes() {
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("service", Map.of("type", "string")), "required", List.of("service"));
        McpSchema.Tool tool = McpSchema.Tool.builder("release_notes", schema).description("Write release notes with the client's model.").build();
        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            String service = (String) request.arguments().get("service");
            McpSchema.CreateMessageResult completion = exchange.createMessage(McpSchema.CreateMessageRequest.builder(
                List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent("Write one sentence of release notes for " + service + "."))), 100).build());
            return text(service + ": " + ((McpSchema.TextContent) completion.content()).text(), false);
        }).build();
    }

    /** The MCP endpoint on 127.0.0.1: an embedded Tomcat on a free port, with one session-aware transport for the one client of this program. */
    record Running(Tomcat tomcat, String url) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    static Running serveOnLoopback() throws Exception {
        Logger.getLogger("org.apache").setLevel(Level.WARNING);
        HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder().jsonMapper(McpJsonDefaults.getMapper()).mcpEndpoint("/mcp").build();
        McpServer.sync(transport).serverInfo("deployer", "1.0.0").capabilities(McpSchema.ServerCapabilities.builder().tools(false).build()).tools(deploy(), releaseNotes()).build();
        Tomcat tomcat = new Tomcat();
        String base = Files.createTempDirectory("mrtr-tomcat").toString();
        tomcat.setBaseDir(base);
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addContext("", base);
        Tomcat.addServlet(context, "mcp", transport).setAsyncSupported(true);
        context.addServletMappingDecoded("/mcp", "mcp");
        tomcat.start();
        return new Running(tomcat, "http://127.0.0.1:" + tomcat.getConnector().getLocalPort());
    }

    /** One line for a JSON-RPC message that crossed the wire. */
    @SuppressWarnings("unchecked")
    static String describe(McpSchema.JSONRPCMessage message, boolean fromServer) {
        if (message instanceof McpSchema.JSONRPCRequest request) {
            Object name = request.params() instanceof Map<?, ?> p ? p.get("name") : request.params() instanceof McpSchema.CallToolRequest c ? c.name() : null;
            return request.method() + (name != null ? " " + name : "") + (fromServer ? " (a request from the server)" : "");
        }
        if (message instanceof McpSchema.JSONRPCResponse response) {
            if (response.error() != null) return "error " + response.error().code();
            if (fromServer && response.result() instanceof Map<?, ?> r && r.get("content") instanceof List<?> content) return "complete: " + ((Map<String, Object>) content.get(0)).get("text");
            return "the answer to the server's request";
        }
        return ((McpSchema.JSONRPCNotification) message).method();
    }

    /** What the client's transport carried, in the order it happened, and whether every request after the first carried the session id. */
    static final class WireLog {
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        final Set<Object> callIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        volatile boolean sawInitialize = false, sessionOnEvery = true;

        /** Wraps a transport so that each tools/call, each request from the server and each response is logged as it passes. */
        McpClientTransport around(McpClientTransport inner) {
            return new McpClientTransport() {
                @Override
                public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
                    return inner.connect(incoming -> handler.apply(incoming.doOnNext(message -> {
                        if (message instanceof McpSchema.JSONRPCRequest || (message instanceof McpSchema.JSONRPCResponse r && callIds.contains(r.id()))) lines.add("<- " + describe(message, true));
                    })));
                }

                @Override
                public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
                    if (message instanceof McpSchema.JSONRPCRequest request && request.method().equals("tools/call")) {
                        callIds.add(request.id());
                        lines.add("-> " + describe(message, false));
                    } else if (message instanceof McpSchema.JSONRPCResponse) {
                        lines.add("-> " + describe(message, false));
                    }
                    return inner.sendMessage(message);
                }

                @Override
                public Mono<Void> closeGracefully() {
                    return inner.closeGracefully();
                }

                @Override
                public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
                    return inner.unmarshalFrom(data, typeRef);
                }

                @Override
                public List<String> protocolVersions() {
                    return inner.protocolVersions();
                }

                @Override
                public void setExceptionHandler(Consumer<Throwable> handler) {
                    inner.setExceptionHandler(handler);
                }
            };
        }

        /** Looks at each HTTP request the transport is about to send: the first must be initialize, the others must carry a session id. */
        void check(java.net.http.HttpRequest.Builder builder, String method, java.net.URI endpoint, String body) {
            boolean hasSession = builder.build().headers().firstValue("mcp-session-id").isPresent();
            if (body != null && body.contains("\"method\":\"initialize\"")) sawInitialize = true;
            else if (!hasSession) sessionOnEvery = false;
        }
    }

    static McpSyncClient connect(String url, WireLog log, Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> person, Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> model) {
        HttpClientStreamableHttpTransport http = HttpClientStreamableHttpTransport.builder(url).endpoint("/mcp")
            .httpRequestCustomizer((builder, method, endpoint, body, context) -> log.check(builder, method, endpoint, body)).build();
        McpClient.SyncSpec spec = McpClient.sync(log.around(http)).clientInfo(new McpSchema.Implementation("host", "1.0.0"));
        if (person != null) spec.elicitation(person);
        if (model != null) spec.sampling(model);
        McpSyncClient client = spec.build();
        client.initialize();
        return client;
    }

    static String textOf(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) throws Exception {
        WireLog log = new WireLog();
        Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> person = request -> {
            System.out.println("the person is asked: " + request.message());
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true));
        };
        Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> model = request -> {
            System.out.println("the client's model is asked: " + ((McpSchema.TextContent) request.messages().get(0).content()).text());
            return new McpSchema.CreateMessageResult(McpSchema.Role.ASSISTANT, new McpSchema.TextContent("Checkout is faster."), "scripted", McpSchema.CreateMessageResult.StopReason.END_TURN);
        };
        try (Running server = serveOnLoopback()) {
            McpSyncClient client = connect(server.url(), log, person, model);
            Object[][] calls = {{"deploy", Map.of("service", "api", "env", "production")}, {"deploy", Map.of("service", "api", "env", "staging")}, {"release_notes", Map.of("service", "api")}};
            for (Object[] call : calls) {
                @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call[1];
                log.lines.clear();
                McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest((String) call[0], arguments));
                System.out.println(call[0] + (arguments.containsKey("env") ? " " + arguments.get("env") : "") + " -> " + textOf(result));
                synchronized (log.lines) {
                    log.lines.forEach(line -> System.out.println("   " + line));
                }
            }
            System.out.println("the first request was initialize: " + py(log.sawInitialize) + " and every later request carried a session id: " + py(log.sessionOnEvery));
            client.closeGracefully();
        }
        try (Running second = serveOnLoopback()) {
            McpSyncClient bare = connect(second.url(), new WireLog(), null, null);
            McpSchema.CallToolResult result = bare.callTool(new McpSchema.CallToolRequest("deploy", Map.of("service", "api", "env", "production")));
            System.out.println("a client that cannot be asked -> " + (Boolean.TRUE.equals(result.isError()) ? "a tool error" : "a result") + " that says so: " + py(textOf(result).contains("cannot be asked")));
            bare.closeGracefully();
        }
        System.exit(0);
    }
}
