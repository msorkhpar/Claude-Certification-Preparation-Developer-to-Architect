import java.util.List;
import java.util.Map;

/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */
final class ReliableAgents {
    private ReliableAgents() {}

    /** A failure worth retrying: a timeout, a rate limit, a lost response. */
    static class Transient extends RuntimeException {
        Transient(String message) {
            super(message);
        }
    }

    /** A failure that retrying cannot fix. */
    static class Fatal extends RuntimeException {
        Fatal(String message) {
            super(message);
        }
    }

    interface Agent {
        String call(String key, Map<String, String> inputs);
    }

    static Map<String, Object> runPlan(List<Map<String, Object>> plan, Map<String, Agent> agents, Map<String, String> store, int attempts, int breakerThreshold) {
        // TODO: run the tasks in order and return a map with done, failed, skipped, degraded, attempts and resumed.
        return null;
    }
}
