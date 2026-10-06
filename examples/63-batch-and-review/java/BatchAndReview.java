import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * <p>Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the methods below make visible. Nothing here calls a model.
 */
public final class BatchAndReview {
    private static final System.Logger LOG = System.getLogger(BatchAndReview.class.getName());
    private static final Pattern CUSTOM_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    record Matched(String customId, String kind) {}

    record Entry(String customId, Map<String, Object> params) {}

    record Pairing(List<Matched> matched, List<String> unrequested) {}

    /** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
    static int worstCaseWait(int intervalHours, int windowHours, int handlingHours) {
        return intervalHours + windowHours + handlingHours;
    }

    /** One entry of a batch request, refused for the same reasons the API refuses it. */
    static Entry batchEntry(String customId, Map<String, Object> params) {
        if (!CUSTOM_ID.matcher(customId).matches()) throw new IllegalArgumentException("custom_id '" + customId + "' must be 1 to 64 letters, digits, hyphens or underscores");
        if (Boolean.TRUE.equals(params.get("stream"))) throw new IllegalArgumentException("stream is not supported in a batch");
        if (params.containsKey("speed")) throw new IllegalArgumentException("speed is not supported in a batch");
        if (Integer.valueOf(0).equals(params.get("max_tokens"))) throw new IllegalArgumentException("a max_tokens of 0 is not supported in a batch");
        return new Entry(customId, params);
    }

    /** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
    static Pairing matchResults(List<String> requests, List<Matched> results) {
        Map<String, String> byId = new HashMap<>();
        for (Matched r : results) byId.put(r.customId(), r.kind());
        Set<String> asked = new HashSet<>(requests);
        List<Matched> matched = new ArrayList<>();
        for (String r : requests) matched.add(new Matched(r, byId.getOrDefault(r, "missing")));
        List<String> unrequested = new ArrayList<>();
        for (Matched r : results) if (!asked.contains(r.customId())) unrequested.add(r.customId());
        return new Pairing(matched, unrequested);
    }

    /** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
    static String reviewRequest(String code, String reasoning, boolean independent) {
        List<String> parts = new ArrayList<>(List.of("Review this code for defects.", "<code>" + code + "</code>"));
        if (!independent) parts.add("<your_earlier_reasoning>" + reasoning + "</your_earlier_reasoning>");
        return String.join("\n", parts);
    }

    public static void main(String[] args) {
        for (int interval : new int[] {4, 6}) System.out.println("worst-case wait with a " + interval + " hour interval: " + worstCaseWait(interval, 24, 2) + " hours (SLA 30 hours)");
        for (String customId : new String[] {"invoice-0042", "invoice 0042"}) {
            try {
                batchEntry(customId, Map.of("max_tokens", 1024));
                System.out.println("custom_id " + customId + ": accepted");
            } catch (IllegalArgumentException e) {
                System.out.println("custom_id " + customId + ": refused, " + e.getMessage());
            }
        }
        for (Map<String, Object> params : List.<Map<String, Object>>of(Map.of("stream", true), Map.of("speed", "fast"), Map.of("max_tokens", 0))) {
            try {
                batchEntry("a1", params);
            } catch (IllegalArgumentException e) {
                System.out.println("refused: " + e.getMessage());
            }
        }
        Pairing p = matchResults(List.of("a1", "a2", "a3"), List.of(new Matched("a2", "expired"), new Matched("z9", "succeeded"), new Matched("a1", "succeeded")));
        List<String> pairs = new ArrayList<>();
        for (Matched m : p.matched()) pairs.add(m.customId() + "=" + m.kind());
        System.out.println("matched: " + String.join(", ", pairs) + "; unrequested: " + String.join(", ", p.unrequested()));
        String code = "total = price * qty";
        String reasoning = "qty is always positive, so no check";
        for (boolean independent : new boolean[] {false, true}) {
            String request = reviewRequest(code, reasoning, independent);
            System.out.println("independent=" + (independent ? "yes" : "no") + ": carries the reasoning: " + (request.contains(reasoning) ? "yes" : "no"));
        }
    }
}
