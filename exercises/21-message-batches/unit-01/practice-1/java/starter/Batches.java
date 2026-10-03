import java.util.List;
import java.util.Map;

/** Build, split and read Message Batches. See ../../statement.md for the contract. Requests and results are JSON-like maps. */
final class Batches {
    private Batches() {}

    static List<Map<String, Object>> buildRequests(List<Map<String, Object>> items) {
        // TODO: turn [{id, params}] into [{custom_id, params}], refusing what a batch refuses.
        return null;
    }

    static List<List<Map<String, Object>>> splitBatches(List<Map<String, Object>> requests, int maxRequests, long maxBytes) {
        // TODO: cut the requests, in order, into batches that respect both limits.
        return null;
    }

    static List<List<Map<String, Object>>> splitBatches(List<Map<String, Object>> requests) {
        return splitBatches(requests, 100_000, 256L * 1024 * 1024);
    }

    static Map<String, Object> collect(List<Map<String, Object>> requests, List<String> resultLines) {
        // TODO: match the result lines to the requests by custom_id and sort out what to retry and what to fix.
        return null;
    }
}
