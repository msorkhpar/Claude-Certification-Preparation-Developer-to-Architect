import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RawClientTest {
    private static final String KEY = "sk-test-0123456789abcdef";
    private static final List<Map<String, Object>> MSGS = list("[{\"role\":\"user\",\"content\":\"Capital of France?\"}]");
    private static final String OK_BODY = """
        {"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5",
         "content":[{"type":"text","text":"Paris."}],"stop_reason":"end_turn","stop_sequence":null,
         "usage":{"input_tokens":9,"output_tokens":3}}""";

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(String json) {
        return (List<Map<String, Object>>) Json.parse(json);
    }

    private static Response reply(int status, String body, Map<String, String> headers) {
        return new Response(status, headers, body);
    }

    private static Response reply(int status, String body) {
        return reply(status, body, Map.of());
    }

    /** The exception the call threw, or null when it did not throw. */
    private static RuntimeException errorOf(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }

    @Test
    void m1_requestHasMethodUrlThreeHeadersAndJsonBody() {
        Request req = RawClient.buildRequest(KEY, "claude-sonnet-5-5", MSGS, 64, "Be brief.");
        assertEquals("POST", req.method());
        assertEquals("https://api.anthropic.com/v1/messages", req.url());
        assertEquals(Map.of("x-api-key", KEY, "anthropic-version", "2023-06-01", "content-type", "application/json"), req.headers());
        assertEquals(Json.parse("""
            {"model":"claude-sonnet-5-5","max_tokens":64,"messages":[{"role":"user","content":"Capital of France?"}],"system":"Be brief."}"""),
            Json.parse(req.body()));
    }

    @Test
    void e1_blankOrAbsentSystemIsLeftOutOfTheBody() {
        for (String system : new String[] {null, "", "   "}) {
            Map<?, ?> body = (Map<?, ?>) Json.parse(RawClient.buildRequest(KEY, "m", MSGS, 8, system).body());
            assertFalse(body.containsKey("system"), String.valueOf(system));
        }
        Map<?, ?> withSystem = (Map<?, ?>) Json.parse(RawClient.buildRequest(KEY, "m", MSGS, 8, "x").body());
        assertEquals("x", withSystem.get("system"));
    }

    @Test
    void e2_badInputIsRefusedBeforeAnythingIsSent() {
        List<Request> calls = new ArrayList<>();
        Transport transport = r -> {
            calls.add(r);
            return reply(200, OK_BODY);
        };
        RuntimeException a = errorOf(() -> RawClient.sendMessages(transport, KEY, "m", MSGS, 0, null));
        RuntimeException b = errorOf(() -> RawClient.sendMessages(transport, KEY, "m", MSGS, -5, null));
        RuntimeException c = errorOf(() -> RawClient.sendMessages(transport, KEY, "m", List.of(), 8, null));
        RuntimeException d = errorOf(() -> RawClient.sendMessages(transport, "  ", "m", MSGS, 8, null));
        for (RuntimeException e : new RuntimeException[] {a, b, c, d}) {
            assertInstanceOf(IllegalArgumentException.class, e);
        }
        assertEquals(0, calls.size());
    }

    @Test
    void e3_successReturnsTheMessageAndTextJoinsTextBlocksOnly() {
        List<Request> seen = new ArrayList<>();
        Map<String, Object> message = RawClient.sendMessages(r -> {
            seen.add(r);
            return reply(200, OK_BODY);
        }, KEY, "claude-sonnet-5-5", MSGS, 64, null);
        assertTrue("end_turn".equals(message.get("stop_reason")) && seen.size() == 1 && seen.get(0).method().equals("POST"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mixed = (Map<String, Object>) Json.parse("""
            {"content":[{"type":"text","text":"Let me "},{"type":"tool_use","id":"t","name":"x","input":{}},{"type":"text","text":"check."}]}""");
        assertEquals("Let me check.", RawClient.textOf(mixed));
        assertEquals("", RawClient.textOf(Map.of("content", List.of())));
    }

    @Test
    void e4_anErrorReplyBecomesAnApiErrorWithTheHeaderRequestId() {
        String body = """
            {"type":"error","error":{"type":"rate_limit_error","message":"Rate limited"},"request_id":"req_from_body"}""";
        RuntimeException e = errorOf(() -> RawClient.sendMessages(r -> reply(429, body, Map.of("request-id", "req_from_header")), KEY, "m", MSGS, 8, null));
        ApiError err = assertInstanceOf(ApiError.class, e);
        assertEquals(429, err.status());
        assertEquals("rate_limit_error", err.errorType());
        assertEquals("Rate limited", err.detail());
        assertEquals("req_from_header", err.requestId());
        String overloaded = """
            {"type":"error","error":{"type":"overloaded_error","message":"Overloaded"},"request_id":"req_from_body"}""";
        RuntimeException e2 = errorOf(() -> RawClient.sendMessages(r -> reply(529, overloaded), KEY, "m", MSGS, 8, null));
        ApiError err2 = assertInstanceOf(ApiError.class, e2);
        assertEquals("req_from_body", err2.requestId());
        assertEquals("overloaded_error", err2.errorType());
    }

    @Test
    void e5_aReplyThatIsNotJsonStillGivesAnApiError() {
        String html = "<html><body><h1>502 Bad Gateway</h1></body></html>";
        RuntimeException e = errorOf(() -> RawClient.sendMessages(r -> reply(502, html), KEY, "m", MSGS, 8, null));
        ApiError err = assertInstanceOf(ApiError.class, e);
        assertEquals(502, err.status());
        assertEquals("unknown", err.errorType());
        assertTrue(err.detail().contains("502 Bad Gateway"));
        assertNull(err.requestId());
    }

    @Test
    void e6_theApiKeyNeverAppearsInAnError() {
        String body = "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key: " + KEY + "\"}}";
        RuntimeException e = errorOf(() -> RawClient.sendMessages(r -> reply(401, body), KEY, "m", MSGS, 8, null));
        ApiError err = assertInstanceOf(ApiError.class, e);
        assertFalse(err.getMessage().contains(KEY) || err.detail().contains(KEY));
        assertTrue(err.detail().contains("[redacted]"));
    }
}
