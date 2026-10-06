import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * One Messages call written by hand, then the same call through the SDK.
 *
 * <p>Both go through the same kind of scripted transport, so nothing leaves the container. The reply is an
 * illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
 * (`harness` is the course's stand-in: a scripted transport plugged into the SDK's own HttpClient hook.)
 */
public final class RawVsSdk {
    private static final System.Logger LOG = System.getLogger(RawVsSdk.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String URL = "https://api.anthropic.com/v1/messages";
    static final Map<String, Object> PAYLOAD = map("model", MODEL, "max_tokens", 64, "messages", List.of(map("role", "user", "content", "Capital of France?")));
    static final Map<String, Object> OK = message(List.of(text("Paris.")));
    static final Reply LIMITED = Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "Rate limited"), "request_id", "req_illustrative_0001"),
        "retry-after", "7", "request-id", "req_illustrative_0001");

    /** The HTTP request the SDK would build, written out: three headers and a JSON body. */
    static Reply rawCall(ScriptedHttp transport) {
        Map<String, String> headers = Map.of("x-api-key", "placeholder", "anthropic-version", "2023-06-01", "content-type", "application/json");
        return transport.send("POST", URL, headers, PAYLOAD);
    }

    static Message sdkCall(ScriptedHttp transport) {
        AnthropicClient client = Scripted.clientOn(transport, 0);
        return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(64).addUserMessage("Capital of France?").build());
    }

    private static String repr(String s) {
        return "'" + s + "'";
    }

    public static void main(String[] args) {
        ScriptedHttp raw = Scripted.http(OK, LIMITED);
        ScriptedHttp sdk = Scripted.http(OK, LIMITED);

        Reply reply = rawCall(raw);
        System.out.println("raw : " + raw.methods.get(0) + " " + raw.urls.get(0) + " -> " + reply.status() + " " + repr(reply.json().at("/content/0/text").asText()));
        Message message = sdkCall(sdk);
        System.out.println("sdk : POST " + sdk.urls.get(0) + " -> 200 " + repr(message.content().get(0).asText().text()));
        System.out.println("same URL: " + py(raw.urls.get(0).equals(sdk.urls.get(0))) + " | same body: " + py(raw.requests.get(0).equals(sdk.requests.get(0))));
        for (String name : List.of("anthropic-version", "content-type")) {
            System.out.println(name + ": raw " + raw.headers.get(0).get(name) + " | sdk " + sdk.headers.get(0).get(name));
        }
        TreeSet<String> onlySdk = new TreeSet<>(sdk.headers.get(0).keySet());
        onlySdk.removeAll(raw.headers.get(0).keySet());
        System.out.println("headers only the SDK adds: " + String.join(", ", onlySdk));

        Reply limited = rawCall(raw);
        JsonNode body = limited.json();
        System.out.println("raw 429 : " + limited.status() + " " + body.at("/error/type").asText() + " retry-after " + limited.headers().get("retry-after"));
        try {
            sdkCall(sdk);
        } catch (RateLimitException err) {
            System.out.println("sdk 429 : " + err.getClass().getSimpleName() + " " + err.statusCode() + " request id " + err.headers().values("request-id").get(0));
        }
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }
}
