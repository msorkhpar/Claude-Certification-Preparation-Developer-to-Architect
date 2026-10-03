import java.util.List;
import java.util.Map;

/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */
final class CachePlan {
    private CachePlan() {}

    static List<Map<String, Object>> planRequest(List<Map<String, Object>> blocks, int minTokens) {
        // TODO: return the blocks in cache-friendly order, each as a map {"id": ..., "cache": null, "5m" or "1h"}.
        return null;
    }
}
