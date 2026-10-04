package harness;

import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ScriptedHttpTest {
    private static MessageCreateParams params(String content) {
        return MessageCreateParams.builder().model(Model.of("claude-sonnet-5-5")).maxTokens(16).addUserMessage(content).build();
    }

    @Test
    void theSdkSendsToTheScriptAndTheTransportRecordsTheWire() {
        Scripted.Rig rig = Scripted.client(message(List.of(text("Paris."))));
        Message reply = rig.client().messages().create(params("Capital of France?"));
        assertEquals("Paris.", reply.content().get(0).asText().text());
        assertEquals(List.of("https://api.anthropic.com/v1/messages"), rig.http().urls);
        assertEquals("Capital of France?", rig.http().requests.get(0).at("/messages/0/content").asText());
        assertEquals("2023-06-01", rig.http().headers.get(0).get("anthropic-version"));
        assertEquals("placeholder", rig.http().headers.get(0).get("x-api-key"));
        assertEquals("java", rig.http().headers.get(0).get("x-stainless-lang"));
    }

    @Test
    void aFunctionEntryAnswersFromTheRequestBodyAndAnEmptyScriptIsA500() {
        Scripted.Rig rig = Scripted.client((java.util.function.Function<com.fasterxml.jackson.databind.JsonNode, Object>) body ->
            message(List.of(text("echo " + body.at("/max_tokens").asInt()))));
        assertEquals("echo 16", rig.client().messages().create(params("x")).content().get(0).asText().text());
        assertThrows(com.anthropic.errors.InternalServerException.class, () -> rig.client().messages().create(params("x")));
    }

    @Test
    void aScriptedStatusBecomesTheSdksTypedError() {
        Reply limited = Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "Rate limited"), "request_id", "req_x"), "request-id", "req_x", "retry-after", "7");
        Scripted.Rig rig = Scripted.client(limited);
        RateLimitException error = assertThrows(RateLimitException.class, () -> rig.client().messages().create(params("x")));
        assertEquals(429, error.statusCode());
    }

    @Test
    void retriesAreCountedInAHeaderAndTheBackOffIsRecordedNotSlept() {
        Reply overloaded = Reply.json(529, map("type", "error", "error", map("type", "overloaded_error", "message", "Overloaded")));
        Scripted.Rig rig = Scripted.clientRetrying(2, overloaded, overloaded, message(List.of(text("ok"))));
        rig.client().messages().create(params("x"));
        assertEquals(List.of("0", "1", "2"), rig.http().headers.stream().map(h -> h.get("x-stainless-retry-count")).toList());
        assertEquals(2, rig.http().sleeps.size());
    }

    @Test
    void anAsyncRigCountsRequestsInsideTheTransportAtTheSameTime() {
        Map<String, Object> ok = message(List.of(text("ok")));
        Scripted.AsyncRig rig = Scripted.asyncClient(Duration.ofMillis(50), 0, ok, ok, ok);
        List<CompletableFuture<Message>> calls = List.of(rig.client().messages().create(params("a")), rig.client().messages().create(params("b")), rig.client().messages().create(params("c")));
        calls.forEach(CompletableFuture::join);
        assertEquals(3, rig.http().maxInFlight());
    }

    @Test
    void aScriptedBodyThatIsNotAMessageIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> message(List.of(map("type", "no_such_block"))));
    }

    @Test
    void aHandWrittenRequestIsRecordedAsWrittenWithNoSdkHeaders() {
        ScriptedHttp http = Scripted.http(Reply.json(200, map("ok", true)));
        Reply reply = http.send("POST", "https://example.invalid/x", Map.of("Content-Type", "application/json"), map("a", 1));
        assertEquals(200, reply.status());
        assertTrue(reply.json().get("ok").asBoolean());
        assertEquals(Map.of("content-type", "application/json"), http.headers.get(0));
        assertEquals(1, http.requests.get(0).get("a").asInt());
        assertEquals(List.of("POST"), http.methods);
    }
}
