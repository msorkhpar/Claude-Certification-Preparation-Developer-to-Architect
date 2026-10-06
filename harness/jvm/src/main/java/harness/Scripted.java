package harness;

import com.anthropic.backends.AnthropicBackend;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.AnthropicClientAsyncImpl;
import com.anthropic.client.AnthropicClientImpl;
import com.anthropic.core.ClientOptions;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.Sleeper;
import com.anthropic.models.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Builders for a real Anthropic Java SDK client wired to a {@link ScriptedHttp}, and for the illustrative response bodies a
 * script holds. Nothing here is a capture: a scripted body is validated against the SDK's own `Message` type before it is used,
 * so a scripted turn is a response the real API could return.
 */
public final class Scripted {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Scripted() {}

    /** The pieces of a scripted run: the SDK client and the transport that recorded what it sent (and the waits it asked for). */
    public record Rig(AnthropicClient client, ScriptedHttp http) {}

    /** The same for the SDK's asynchronous client. */
    public record AsyncRig(AnthropicClientAsync client, ScriptedHttp http) {}

    /** A sleeper that records the back-off the SDK asks for instead of waiting for it. */
    private static Sleeper recording(List<Duration> sleeps) {
        return new Sleeper() {
            @Override
            public void sleep(Duration duration) {
                sleeps.add(duration);
            }

            @Override
            public CompletableFuture<Void> sleepAsync(Duration duration) {
                sleeps.add(duration);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void close() {}
        };
    }

    private static ClientOptions options(ScriptedHttp http, int maxRetries) {
        AnthropicBackend backend = http.backend();
        ClientOptions.Builder builder = ClientOptions.builder().maxRetries(maxRetries).sleeper(recording(http.sleeps)).baseUrl(backend.baseUrl());
        backend.applyCredentials(http, builder);
        return builder.httpClient(http).build();
    }

    /** A transport that answers with the script, one entry per request, after `delay`. */
    public static ScriptedHttp http(Duration delay, Object... script) {
        return new ScriptedHttp(AnthropicBackend.builder().apiKey("placeholder").build(), delay, List.of(script));
    }

    public static ScriptedHttp http(Object... script) {
        return http(Duration.ZERO, script);
    }

    /** The SDK's own client, built on a transport you already hold. */
    public static AnthropicClient clientOn(ScriptedHttp http, int maxRetries) {
        return new AnthropicClientImpl(options(http, maxRetries));
    }

    public static AnthropicClientAsync asyncClientOn(ScriptedHttp http, int maxRetries) {
        return new AnthropicClientAsyncImpl(options(http, maxRetries));
    }

    /** (client, transport) with no SDK retries: the SDK's own client against the script. */
    public static Rig client(Object... script) {
        return clientRetrying(0, script);
    }

    public static Rig clientRetrying(int maxRetries, Object... script) {
        ScriptedHttp http = http(script);
        return new Rig(clientOn(http, maxRetries), http);
    }

    /** The asynchronous client; every request takes `delay` inside the transport. */
    public static AsyncRig asyncClient(Duration delay, int maxRetries, Object... script) {
        ScriptedHttp http = http(delay, script);
        return new AsyncRig(asyncClientOn(http, maxRetries), http);
    }

    // ---- bodies ----------------------------------------------------------------------------------------------------

    /** A Messages API response body with default usage (1 token in, 1 out), validated against the SDK's type. */
    public static Map<String, Object> message(List<? extends Map<String, Object>> content) {
        return message(content, "end_turn", "claude-sonnet-5-5", map("input_tokens", 1, "output_tokens", 1), null);
    }

    public static Map<String, Object> message(List<? extends Map<String, Object>> content, String stopReason) {
        return message(content, stopReason, "claude-sonnet-5-5", map("input_tokens", 1, "output_tokens", 1), null);
    }

    public static Map<String, Object> message(List<? extends Map<String, Object>> content, String stopReason, String model, Map<String, Object> usage, String stopSequence) {
        Map<String, Object> body = map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", model, "content", content,
            "stop_reason", stopReason, "stop_sequence", stopSequence, "usage", usage);
        try {
            ObjectMappers.jsonMapper().convertValue(body, Message.class).validate(); // a scripted turn must be a response the real API could return
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("not a Messages response: " + e.getMessage(), e);
        }
        return body;
    }

    public static Map<String, Object> text(String t) {
        return map("type", "text", "text", t);
    }

    public static Map<String, Object> toolUse(String id, String name, Map<String, Object> input) {
        return map("type", "tool_use", "id", id, "name", name, "input", input);
    }

    /** A map from alternating keys and values, in order. */
    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** JSON text as a tree. */
    public static JsonNode tree(String json) {
        try {
            return JSON.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
