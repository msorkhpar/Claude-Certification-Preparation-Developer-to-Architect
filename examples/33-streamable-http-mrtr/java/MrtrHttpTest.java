import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class MrtrHttpTest {
    private static McpSchema.CallToolResult deploy(McpSyncClient client, String env) {
        return client.callTool(new McpSchema.CallToolRequest("deploy", Map.of("service", "api", "env", env)));
    }

    private static Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> answer(McpSchema.ElicitResult.Action action, boolean confirm) {
        return request -> new McpSchema.ElicitResult(action, action == McpSchema.ElicitResult.Action.ACCEPT ? Map.of("confirm", confirm) : null);
    }

    @Test
    void aStagingDeployAsksNobody() throws Exception {
        try (MrtrHttp.Running server = MrtrHttp.serveOnLoopback()) {
            McpSyncClient client = MrtrHttp.connect(server.url(), new MrtrHttp.WireLog(), null, null);
            assertEquals("Deployed api to staging", MrtrHttp.textOf(deploy(client, "staging")));
            client.closeGracefully();
        }
    }

    @Test
    void aProductionDeployFollowsThePersonsAnswer() throws Exception {
        Object[][] cases = {
            {McpSchema.ElicitResult.Action.ACCEPT, true, "Deployed api to production"},
            {McpSchema.ElicitResult.Action.ACCEPT, false, "Deployment cancelled"},
            {McpSchema.ElicitResult.Action.DECLINE, false, "Deployment cancelled"}};
        try (MrtrHttp.Running server = MrtrHttp.serveOnLoopback()) {
            for (Object[] c : cases) {
                McpSyncClient client = MrtrHttp.connect(server.url(), new MrtrHttp.WireLog(), answer((McpSchema.ElicitResult.Action) c[0], (Boolean) c[1]), null);
                assertEquals(c[2], MrtrHttp.textOf(deploy(client, "production")), c[0] + " " + c[1]);
                client.closeGracefully();
            }
        }
    }

    @Test
    void theReleaseNotesComeFromTheClientsModel() throws Exception {
        try (MrtrHttp.Running server = MrtrHttp.serveOnLoopback()) {
            McpSyncClient client = MrtrHttp.connect(server.url(), new MrtrHttp.WireLog(), null,
                request -> new McpSchema.CreateMessageResult(McpSchema.Role.ASSISTANT, new McpSchema.TextContent("Faster."), "scripted", McpSchema.CreateMessageResult.StopReason.END_TURN));
            assertEquals("api: Faster.", MrtrHttp.textOf(client.callTool(new McpSchema.CallToolRequest("release_notes", Map.of("service", "api")))));
            client.closeGracefully();
        }
    }

    @Test
    void aClientThatCannotBeAskedGetsAToolErrorAndNotAQuestion() throws Exception {
        try (MrtrHttp.Running server = MrtrHttp.serveOnLoopback()) {
            McpSyncClient client = MrtrHttp.connect(server.url(), new MrtrHttp.WireLog(), null, null);
            McpSchema.CallToolResult result = deploy(client, "production");
            assertTrue(Boolean.TRUE.equals(result.isError()) && MrtrHttp.textOf(result).contains("cannot be asked"));
            client.closeGracefully();
        }
    }

    @Test
    void theWireLogShowsTheServerAskingInTheMiddleOfTheCall() throws Exception {
        try (MrtrHttp.Running server = MrtrHttp.serveOnLoopback()) {
            MrtrHttp.WireLog log = new MrtrHttp.WireLog();
            McpSyncClient client = MrtrHttp.connect(server.url(), log, answer(McpSchema.ElicitResult.Action.ACCEPT, true), null);
            deploy(client, "production");
            assertEquals(java.util.List.of("-> tools/call deploy", "<- elicitation/create (a request from the server)", "-> the answer to the server's request", "<- complete: Deployed api to production"), log.lines);
            assertTrue(log.sawInitialize && log.sessionOnEvery);
            client.closeGracefully();
        }
    }
}
