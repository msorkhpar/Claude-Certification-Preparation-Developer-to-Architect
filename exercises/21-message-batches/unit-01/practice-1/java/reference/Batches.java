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
        if (!(id instanceof String s) || !CUSTOM_ID.matcher(s).matches()) {
            throw new BatchError("custom_id", id + " is not 1 to 64 letters, digits, hyphens or underscores");
        }
        if (!seen.add(s)) throw new BatchError("custom_id", s + " is used twice");
    }

    private static void checkParams(Map<String, Object> params) {
        Object maxTokens = params.get("max_tokens");
        if (maxTokens == null || ((Number) maxTokens).longValue() < 1) throw new BatchError("params.max_tokens", "must be at least 1 inside a batch");
        if (Boolean.TRUE.equals(params.get("stream"))) throw new BatchError("params.stream", "batch results are a file, not a stream");
        if (params.containsKey("speed")) throw new BatchError("params.speed", "fast mode is not available in a batch");
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
        return count > 0 && (count >= maxRequests || used + bytes > maxBytes);
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
        String cid = (String) record.get("custom_id");
        if (!wanted.contains(cid)) unknown.add(cid);
        else byId.putIfAbsent(cid, castMap(record.get("result")));
    }

    private static boolean needsFix(String errorType) {
        return errorType.equals("invalid_request_error");
    }

    private static void addUsage(Map<String, Object> usage, Map<String, Object> used) {
        for (String key : USAGE_KEYS) {
            long add = used.get(key) == null ? 0 : ((Number) used.get(key)).longValue();
            usage.put(key, ((Number) usage.get(key)).longValue() + add);
        }
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
