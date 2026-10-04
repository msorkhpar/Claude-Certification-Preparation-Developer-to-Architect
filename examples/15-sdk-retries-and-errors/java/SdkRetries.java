import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the SDK retries on its own, and what it does not, against a scripted transport.
 *
 * <p>The failures are illustrative, hand-written replies shaped like the API's error bodies.
 * The SDK asks for a short exponential back-off between attempts (about 0.5 s, then about 1 s); the course's transport records
 * those waits instead of sleeping them.
 */
public final class SdkRetries {
    static final String MODEL = "claude-sonnet-5-5";

    static MessageCreateParams params() {
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).addUserMessage("Hi").build();
    }

    static Reply error(int status, String kind, String requestId) {
        return Reply.json(status, map("type", "error", "error", map("type", kind, "message", kind), "request_id", requestId), "request-id", requestId);
    }

    static final Reply OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529");
    static final Reply BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400");
    static final Reply SPEND_CAP = Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "monthly limit reached",
        "details", map("error_code", "enforced_spend_limit_reached")), "request_id", "req_illustrative_0429"), "request-id", "req_illustrative_0429");

    static AnthropicClient clientFor(ScriptedHttp transport, int maxRetries) {
        return Scripted.clientOn(transport, maxRetries);
    }

    static void attempt(String label, List<Object> script, int maxRetries) {
        ScriptedHttp transport = Scripted.http(script.toArray());
        String outcome;
        try {
            Message reply = clientFor(transport, maxRetries).messages().create(params());
            outcome = "ok '" + reply.content().get(0).asText().text() + "'";
        } catch (AnthropicServiceException err) {
            outcome = err.getClass().getSimpleName() + " " + err.statusCode() + " " + err.errorType().get().asString() + " request id " + err.headers().values("request-id").get(0);
        } catch (AnthropicIoException err) {
            outcome = err.getClass().getSimpleName();
        }
        List<String> counts = new ArrayList<>();
        transport.headers.forEach(h -> counts.add("'" + h.get("x-stainless-retry-count") + "'"));
        System.out.println(label + ": " + transport.requests.size() + " request(s), retry-count header [" + String.join(", ", counts) + "] -> " + outcome);
    }

    public static void main(String[] args) {
        attempt("529, 529, then 200, max_retries=2", List.of(OVERLOADED, OVERLOADED, message(List.of(text("Hello.")))), 2);
        attempt("529, 529, then 200, max_retries=0", List.of(OVERLOADED, OVERLOADED, message(List.of(text("Hello.")))), 0);
        attempt("400 is never retried, max_retries=2", List.of(BAD_REQUEST, message(List.of(text("Hello.")))), 2);
        attempt("spend-cap 429, max_retries=2", List.of(SPEND_CAP, SPEND_CAP, SPEND_CAP), 2);
        AnthropicIoException timeout = new AnthropicIoException("scripted", new SocketTimeoutException("scripted"));
        attempt("timeout twice, max_retries=1", List.of(timeout, timeout), 1);
    }
}
