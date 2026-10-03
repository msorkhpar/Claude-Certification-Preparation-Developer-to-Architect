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
    private Batches() {}

    private static final Pattern CUSTOM_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    static final int MAX_REQUESTS = 100_000;
    static final long MAX_BYTES = 256L * 1024 * 1024;
    private static final List<String> USAGE_KEYS = List.of("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens");

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> buildRequests(List<Map<String, Object>> items) {
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> requests = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Object id = item.get("id");
            Map<String, Object> params = (Map<String, Object>) item.get("params");
            if (!(id instanceof String s) || !CUSTOM_ID.matcher(s).matches()) {
                throw new BatchError("custom_id", id + " is not 1 to 64 letters, digits, hyphens or underscores");
            }
            if (!seen.add(s)) throw new BatchError("custom_id", s + " is used twice");
            Object maxTokens = params.get("max_tokens");
            if (maxTokens == null || ((Number) maxTokens).longValue() < 1) throw new BatchError("params.max_tokens", "must be at least 1 inside a batch");
            if (Boolean.TRUE.equals(params.get("stream"))) throw new BatchError("params.stream", "batch results are a file, not a stream");
            if (params.containsKey("speed")) throw new BatchError("params.speed", "fast mode is not available in a batch");
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("custom_id", s);
            request.put("params", params);
            requests.add(request);
        }
        return requests;
    }

    private static long size(Map<String, Object> request) {
        return Json.stringify(request).getBytes(StandardCharsets.UTF_8).length;
    }

    static List<List<Map<String, Object>>> splitBatches(List<Map<String, Object>> requests, int maxRequests, long maxBytes) {
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        List<Map<String, Object>> current = new ArrayList<>();
        long used = 0;
        for (Map<String, Object> request : requests) {
            long bytes = size(request);
            if (bytes > maxBytes) throw new BatchError("size", request.get("custom_id") + " alone is larger than a batch may be");
            if (!current.isEmpty() && (current.size() >= maxRequests || used + bytes > maxBytes)) {
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

    @SuppressWarnings("unchecked")
    static Map<String, Object> collect(List<Map<String, Object>> requests, List<String> resultLines) {
        List<String> wanted = new ArrayList<>();
        for (Map<String, Object> r : requests) wanted.add((String) r.get("custom_id"));
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();
        for (String line : resultLines) {
            if (line.isBlank()) continue;
            Map<String, Object> record = (Map<String, Object>) Json.parse(line);
            String cid = (String) record.get("custom_id");
            if (!wanted.contains(cid)) unknown.add(cid);
            else byId.putIfAbsent(cid, (Map<String, Object>) record.get("result"));
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
                for (String key : USAGE_KEYS) {
                    long add = used.get(key) == null ? 0 : ((Number) used.get(key)).longValue();
                    usage.put(key, ((Number) usage.get(key)).longValue() + add);
                }
            } else if ("errored".equals(result.get("type"))) {
                Map<String, Object> outer = (Map<String, Object>) result.get("error");
                String kind = (String) ((Map<String, Object>) outer.get("error")).get("type");
                outcome.put("status", "errored");
                outcome.put("error_type", kind);
                (kind.equals("invalid_request_error") ? fix : retry).add(cid);
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
