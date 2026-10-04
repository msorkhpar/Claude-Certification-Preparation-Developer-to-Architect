import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.batches.BatchCreateParams;
import com.anthropic.models.messages.batches.MessageBatch;
import com.anthropic.models.messages.batches.MessageBatchIndividualResponse;
import com.anthropic.models.messages.batches.MessageBatchResult;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Message Batch from submission to results, against a scripted server.
 *
 * <p>The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
 * captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
 * them. Waiting between polls is recorded, not slept. The Java SDK asks for `/results` directly, so the script has no
 * batch look-up before the results (the Python SDK makes one).
 */
public final class BatchRoundTrip {
    static final String MODEL = "claude-haiku-4-5-20251001";
    static final Map<String, String> TICKETS = new LinkedHashMap<>();

    static {
        TICKETS.put("t-1", "My parcel never arrived.");
        TICKETS.put("t-2", "How do I change my address?");
        TICKETS.put("t-3", "Charge me twice? Refund please.");
        TICKETS.put("t-4", "x".repeat(10));
    }

    static Map<String, Object> batch(String status, Map<String, Object> counts, String resultsUrl) {
        return map("id", "msgbatch_illustrative", "type", "message_batch", "processing_status", status, "request_counts", counts,
            "ended_at", status.equals("ended") ? "2026-10-02T10:40:00Z" : null, "created_at", "2026-10-02T10:00:00Z",
            "expires_at", "2026-10-03T10:00:00Z", "cancel_initiated_at", null, "results_url", resultsUrl);
    }

    static Map<String, Object> counts(int processing, int succeeded, int errored, int canceled, int expired) {
        return map("processing", processing, "succeeded", succeeded, "errored", errored, "canceled", canceled, "expired", expired);
    }

    static String resultLine(String customId, Map<String, Object> result) {
        try {
            return ObjectMappers.jsonMapper().writeValueAsString(map("custom_id", customId, "result", result));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Map<String, Object> succeeded(String label) {
        return map("type", "succeeded", "message", message(List.of(text(label)), "end_turn", MODEL, map("input_tokens", 30, "output_tokens", 3), null));
    }

    static final String RESULTS = String.join("\n",
        resultLine("t-3", succeeded("billing")),
        resultLine("t-1", succeeded("shipping")),
        resultLine("t-4", map("type", "errored", "error", map("type", "error", "error", map("type", "invalid_request_error", "message", "messages: at least one message is required")))),
        resultLine("t-2", map("type", "expired"))) + "\n";

    static final String RESULTS_URL = "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results";

    static List<Object> script() {
        return List.of(
            Reply.json(200, batch("in_progress", counts(4, 0, 0, 0, 0), null)),
            Reply.json(200, batch("in_progress", counts(2, 2, 0, 0, 0), null)),
            Reply.json(200, batch("ended", counts(0, 2, 1, 0, 1), RESULTS_URL)),
            Reply.text(200, "application/x-jsonl", RESULTS));
    }

    static BatchCreateParams requests() {
        BatchCreateParams.Builder builder = BatchCreateParams.builder();
        TICKETS.forEach((cid, body) -> builder.addRequest(BatchCreateParams.Request.builder().customId(cid)
            .params(BatchCreateParams.Request.Params.builder().model(MODEL).maxTokens(50).addUserMessage("Label this ticket: " + body).build()).build()));
        return builder.build();
    }

    /** What came back for one request: its kind (succeeded, errored, expired, canceled) and a detail. */
    record Outcome(String kind, String detail) {}

    static Outcome outcomeOf(MessageBatchResult result) {
        if (result.succeeded().isPresent()) return new Outcome("succeeded", result.succeeded().get().message().content().get(0).asText().text());
        if (result.errored().isPresent()) {
            JsonNode error = ObjectMappers.jsonMapper().valueToTree(result.errored().get().error());
            return new Outcome("errored", error.at("/error/type").asText());
        }
        return new Outcome(result.canceled().isPresent() ? "canceled" : "expired", "");
    }

    /** The results in the order they arrived. */
    static Map<String, Outcome> results(AnthropicClient client, String batchId) {
        Map<String, Outcome> outcomes = new LinkedHashMap<>();
        try (StreamResponse<MessageBatchIndividualResponse> stream = client.messages().batches().resultsStreaming(batchId)) {
            stream.stream().forEach(item -> outcomes.put(item.customId(), outcomeOf(item.result())));
        }
        return outcomes;
    }

    private static String counts(MessageBatch status) {
        var c = status.requestCounts();
        return "{'canceled': " + c.canceled() + ", 'errored': " + c.errored() + ", 'expired': " + c.expired() + ", 'processing': " + c.processing() + ", 'succeeded': " + c.succeeded() + "}";
    }

    public static void main(String[] args) {
        ScriptedHttp transport = Scripted.http(script().toArray());
        AnthropicClient client = Scripted.clientOn(transport, 0);
        MessageBatch created = client.messages().batches().create(requests());
        List<String> sentIds = new ArrayList<>();
        transport.requests.get(0).get("requests").forEach(r -> sentIds.add(r.get("custom_id").asText()));
        System.out.println("created: " + created.id() + " " + created.processingStatus().asString() + " | request ids sent: " + py(sentIds));
        List<Integer> waits = new ArrayList<>();
        MessageBatch status = created;
        while (!status.processingStatus().asString().equals("ended")) {
            status = client.messages().batches().retrieve(created.id());
            waits.add(60);
            System.out.println("poll: " + status.processingStatus().asString() + " " + counts(status));
        }
        System.out.println("waited between polls (recorded, not slept): " + waits + " seconds");
        Map<String, Outcome> outcomes = results(client, created.id());
        outcomes.forEach((cid, o) -> System.out.println("result: " + cid + " " + o.kind() + " " + o.detail()));
        System.out.println("in request order: [" + TICKETS.keySet().stream().map(cid -> "('" + cid + "', '" + outcomes.get(cid).kind() + "')").collect(Collectors.joining(", ")) + "]");
        List<String> fix = outcomes.entrySet().stream().filter(e -> e.getValue().kind().equals("errored") && e.getValue().detail().equals("invalid_request_error")).map(Map.Entry::getKey).toList();
        List<String> retry = outcomes.entrySet().stream().filter(e -> List.of("expired", "canceled").contains(e.getValue().kind())
            || (e.getValue().kind().equals("errored") && !fix.contains(e.getKey()))).map(Map.Entry::getKey).toList();
        System.out.println("fix before resubmitting: " + py(fix) + " | resubmit unchanged: " + py(retry));
    }
}
