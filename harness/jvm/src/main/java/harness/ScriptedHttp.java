package harness;

import com.anthropic.backends.AnthropicBackend;
import com.anthropic.core.RequestOptions;
import com.anthropic.core.http.Headers;
import com.anthropic.core.http.HttpClient;
import com.anthropic.core.http.HttpRequest;
import com.anthropic.core.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * A scripted model behind the Java SDK's own transport hook: the `HttpClient` interface of `ClientOptions`.
 * The reader's code calls the SDK unchanged; the request goes here, never to a socket.
 *
 * <p>Each call pops one script entry. An entry is a {@link Reply}, a JSON body (a map, list or JsonNode: answered with 200),
 * a Throwable (thrown as a failed connection or a timeout), or a {@code Function<JsonNode, Object>} of the request body
 * returning any of these. `requests` keeps every request body the SDK sent, `headers` the lower-cased request headers
 * (as the transport saw them, with the version and key headers the SDK's backend adds) and `urls` the URLs, in order.
 * It plays the part of the Python harness's ScriptedTransport and the TypeScript harness's scriptedFetch.
 */
public final class ScriptedHttp implements HttpClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Executor ASYNC = Executors.newVirtualThreadPerTaskExecutor();

    private final List<Object> script;
    private final AnthropicBackend backend;
    private final Duration delay;
    public final List<JsonNode> requests = Collections.synchronizedList(new ArrayList<>());
    public final List<Map<String, String>> headers = Collections.synchronizedList(new ArrayList<>());
    public final List<String> urls = Collections.synchronizedList(new ArrayList<>());
    public final List<String> methods = Collections.synchronizedList(new ArrayList<>());
    /** The back-off waits the SDK asked for between retries (recorded, never slept). */
    public final List<Duration> sleeps = Collections.synchronizedList(new ArrayList<>());
    private int inFlight;
    private int maxInFlight;

    public ScriptedHttp(AnthropicBackend backend, Duration delay, List<Object> script) {
        this.backend = backend;
        this.delay = delay;
        this.script = Collections.synchronizedList(new ArrayList<>(script));
    }

    public AnthropicBackend backend() {
        return backend;
    }

    /**
     * A hand-written request, sent as written: what the caller passes is exactly what is recorded (no SDK headers, no key),
     * and the next script entry answers. The raw counterpart of an SDK call.
     */
    public Reply send(String method, String url, Map<String, String> requestHeaders, Object jsonBody) {
        JsonNode body = JSON.valueToTree(jsonBody);
        Map<String, String> lower = new LinkedHashMap<>();
        requestHeaders.forEach((k, v) -> lower.put(k.toLowerCase(), v));
        synchronized (this) {
            requests.add(body);
            headers.add(lower);
            urls.add(url);
            methods.add(method);
        }
        Reply reply = (Reply) next(body, true);
        return reply;
    }

    /** Requests that were inside the transport at the same time, at most. */
    public synchronized int maxInFlight() {
        return maxInFlight;
    }

    /** The request as the transport would send it: version header from the backend, then the key. */
    private HttpRequest onTheWire(HttpRequest request) {
        return backend.authorizeRequest(backend.prepareRequest(request));
    }

    @Override
    public HttpResponse execute(HttpRequest request, RequestOptions requestOptions) {
        HttpRequest sent = onTheWire(request);
        JsonNode body = bodyOf(sent);
        synchronized (this) {
            requests.add(body);
            Map<String, String> wire = lowerCased(sent.headers());
            if (sent.body() != null && sent.body().contentType() != null) wire.putIfAbsent("content-type", sent.body().contentType()); // the transport sets it from the body
            headers.add(wire);
            urls.add(sent.url());
            methods.add(sent.method().name());
            inFlight++;
            maxInFlight = Math.max(maxInFlight, inFlight);
        }
        try {
            if (!delay.isZero()) Thread.sleep(delay.toMillis());
            return answer(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            synchronized (this) {
                inFlight--;
            }
        }
    }

    @Override
    public CompletableFuture<HttpResponse> executeAsync(HttpRequest request, RequestOptions requestOptions) {
        return CompletableFuture.supplyAsync(() -> execute(request, requestOptions), ASYNC);
    }

    @Override
    public void close() {}

    private HttpResponse answer(JsonNode body) {
        return response((Reply) next(body, true));
    }

    /** The next script entry as a Reply (a Throwable entry is thrown). */
    @SuppressWarnings("unchecked")
    private Object next(JsonNode body, boolean asReply) {
        if (script.isEmpty()) {
            return Reply.json(500, Map.of("type", "error", "error", Map.of("type", "api_error", "message", "scripted model ran out of replies")));
        }
        Object item = script.remove(0);
        if (item instanceof Function<?, ?> f) item = ((Function<JsonNode, Object>) f).apply(body);
        if (item instanceof RuntimeException e) throw e;
        if (item instanceof Throwable t) throw new RuntimeException(t);
        if (item instanceof Reply r) return r;
        return Reply.json(200, item);
    }

    private static HttpResponse response(Reply reply) {
        com.anthropic.core.http.Headers.Builder h = com.anthropic.core.http.Headers.builder();
        reply.headers().forEach(h::put);
        Headers headers = h.build();
        return new HttpResponse() {
            @Override
            public int statusCode() {
                return reply.status();
            }

            @Override
            public Headers headers() {
                return headers;
            }

            @Override
            public InputStream body() {
                return new ByteArrayInputStream(reply.body());
            }

            @Override
            public void close() {}
        };
    }

    private static JsonNode bodyOf(HttpRequest request) {
        if (request.body() == null) return JSON.createObjectNode();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            request.body().writeTo(out);
            return out.size() == 0 ? JSON.createObjectNode() : JSON.readTree(out.toByteArray());
        } catch (Exception e) {
            return JSON.createObjectNode();
        }
    }

    private static Map<String, String> lowerCased(Headers headers) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String name : headers.names()) out.put(name.toLowerCase(), String.join(", ", headers.values(name)));
        return out;
    }
}
