import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Build, split and read Message Batches. See ../../statement.md for the contract. Requests and results are JSON-like maps. */
final class Batches {
    private static final System.Logger LOG = System.getLogger(Batches.class.getName());
    private Batches() {}

    private static final Pattern CUSTOM_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    static final int MAX_REQUESTS = 100_000;
    static final long MAX_BYTES = 256L * 1024 * 1024;
    private static final List<String> USAGE_KEYS = List.of("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens");

    private static void checkCustomId(Object id, Set<String> seen) {
        // TODO 1 of 6 (finish this to pass e1): refuse a bad or repeated custom id, and remember a good one.
        // Receives the id and the set `seen` of ids already used. Throws BatchError("custom_id", reason) when the id is not a String
        // matching CUSTOM_ID (1 to 64 letters, digits, hyphens or underscores) or is already in `seen`; otherwise adds it to `seen`.
        // Example: "has space" throws; passing "a" twice with the same set throws the second time
    }

    private static void checkParams(Map<String, Object> params) {
        // TODO 2 of 6 (finish this to pass e2): refuse the parameters a batch cannot take.
        // Receives a request body. Throws BatchError("params.max_tokens", reason) when max_tokens is missing or below 1,
        // BatchError("params.stream", reason) when stream is true, and BatchError("params.speed", reason) when speed is present at all.
        // Example: a body with max_tokens 0 throws with field "params.max_tokens"
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> buildRequests(List<Map<String, Object>> items) {
        LOG.log(System.Logger.Level.DEBUG, "buildRequests input: {0}", items);
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> requests = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Object id = item.get("id");
            Map<String, Object> params = (Map<String, Object>) item.get("params");
            checkCustomId(id, seen);
            checkParams(params);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("custom_id", id);
            request.put("params", params);
            requests.add(request);
        }
        return requests;
    }

    private static long size(Map<String, Object> request) {
        return Json.stringify(request).getBytes(StandardCharsets.UTF_8).length;
    }

    private static boolean mustStartNew(int count, long used, long bytes, int maxRequests, long maxBytes) {
        // TODO 3 of 6 (finish this to pass e3): must this request go into a new batch?
        // Receives the number of requests in the current batch, their total bytes `used`, the size of the next request and both limits.
        // Returns true when the current batch is not empty and adding the request would pass either limit (count already at maxRequests,
        // or used + bytes above maxBytes). Example: (3, 10, 5, 3, 1000) -> true, (0, 0, 5, 3, 1000) -> false
        return false;
    }

    static List<List<Map<String, Object>>> splitBatches(List<Map<String, Object>> requests, int maxRequests, long maxBytes) {
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        List<Map<String, Object>> current = new ArrayList<>();
        long used = 0;
        for (Map<String, Object> request : requests) {
            long bytes = size(request);
            if (bytes > maxBytes) throw new BatchError("size", request.get("custom_id") + " alone is larger than a batch may be");
            if (mustStartNew(current.size(), used, bytes, maxRequests, maxBytes)) {
                batches.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(request);
            used += bytes;
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    static List<List<Map<String, Object>>> splitBatches(List<Map<String, Object>> requests) {
        return splitBatches(requests, MAX_REQUESTS, MAX_BYTES);
    }

    private static void keepResult(List<String> wanted, Map<String, Map<String, Object>> byId, List<String> unknown, Map<String, Object> record) {
        // TODO 4 of 6 (finish this to pass m1 and e5): file one parsed result line by its custom id.
        // Receives the list `wanted` of request ids, the map `byId`, the list `unknown` and the parsed `record` (custom_id and result).
        // Adds the id to `unknown` when no request carries it; otherwise stores the record's result in `byId` unless the id is already
        // there (the first result wins); castMap turns the result into a map. Example: a record for "zzz" when wanted is ["a"] -> unknown
    }

    private static boolean needsFix(String errorType) {
        // TODO 5 of 6 (finish this to pass e4): does an errored result need a corrected request?
        // Receives the error type of an errored result. Returns true for "invalid_request_error" (it fails again until fixed), false for
        // any other type, which is worth sending again unchanged. Example: "overloaded_error" -> false
        return false;
    }

    private static void addUsage(Map<String, Object> usage, Map<String, Object> used) {
        // TODO 6 of 6 (finish this to pass e6): add one succeeded reply's usage to the totals.
        // Receives the map `usage` of totals (every key of USAGE_KEYS holds a Long, starting at 0L) and the reply's usage map, which may
        // lack a key. Adds each key of USAGE_KEYS from `used` to `usage`, a missing key counting as 0. Store Long values.
        // Example: adding {input_tokens: 5} raises usage input_tokens by 5 and leaves the other three alone
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> collect(List<Map<String, Object>> requests, List<String> resultLines) {
        List<String> wanted = new ArrayList<>();
        for (Map<String, Object> r : requests) wanted.add((String) r.get("custom_id"));
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        for (String line : resultLines) {
            if (line.isBlank()) continue;
            keepResult(wanted, byId, unknown, (Map<String, Object>) Json.parse(line));
        }
        List<Map<String, Object>> outcomes = new ArrayList<>();
        List<String> retry = new ArrayList<>();
        List<String> fix = new ArrayList<>();
        Map<String, Object> usage = new LinkedHashMap<>();
        for (String key : USAGE_KEYS) usage.put(key, 0L);
        for (String cid : wanted) {
            Map<String, Object> result = byId.get(cid);
            Map<String, Object> outcome = new LinkedHashMap<>();
            outcome.put("custom_id", cid);
            if (result == null) {
                outcome.put("status", "missing");
                retry.add(cid);
            } else if ("succeeded".equals(result.get("type"))) {
                Map<String, Object> message = (Map<String, Object>) result.get("message");
                StringBuilder text = new StringBuilder();
                for (Object block : (List<Object>) message.get("content")) {
                    Map<String, Object> b = (Map<String, Object>) block;
                    if ("text".equals(b.get("type"))) text.append(b.getOrDefault("text", ""));
                }
                Map<String, Object> used = (Map<String, Object>) message.get("usage");
                outcome.put("status", "succeeded");
                outcome.put("text", text.toString());
                outcome.put("usage", used);
                addUsage(usage, used);
            } else if ("errored".equals(result.get("type"))) {
                Map<String, Object> outer = (Map<String, Object>) result.get("error");
                String kind = (String) ((Map<String, Object>) outer.get("error")).get("type");
                outcome.put("status", "errored");
                outcome.put("error_type", kind);
                (needsFix(kind) ? fix : retry).add(cid);
            } else { // canceled or expired: the request never reached the model
                outcome.put("status", result.get("type"));
                retry.add(cid);
            }
            outcomes.add(outcome);
        }
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("outcomes", outcomes);
        done.put("retry", retry);
        done.put("fix", fix);
        done.put("unknown", unknown);
        done.put("usage", usage);
        return done;
    }
}
