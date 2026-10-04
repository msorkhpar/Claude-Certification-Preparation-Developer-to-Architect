import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private record Outcome(String result, String reason) {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> runPlan(List<Map<String, Object>> plan, Map<String, Agent> agents, Map<String, String> store, int attempts, int breakerThreshold) {
        Map<String, String> done = new LinkedHashMap<>();
        Map<String, String> failed = new LinkedHashMap<>();
        Map<String, String> skipped = new LinkedHashMap<>();
        List<String> degraded = new ArrayList<>();
        List<String> resumed = new ArrayList<>();
        Map<String, Integer> calls = new LinkedHashMap<>();
        Map<String, Integer> consecutive = new HashMap<>();

        java.util.function.Function<Object[], Outcome> attempt = args -> {
            String agentName = (String) args[0];
            String key = (String) args[1];
            Map<String, String> inputs = (Map<String, String>) args[2];
            String taskId = (String) args[3];
            for (int n = 1; n <= attempts; n++) {
                if (consecutive.getOrDefault(agentName, 0) >= breakerThreshold) return new Outcome(null, "circuit open");
                calls.merge(taskId, 1, Integer::sum);
                try {
                    String result = agents.get(agentName).call(key, inputs);
                    consecutive.put(agentName, 0);
                    return new Outcome(result, null);
                } catch (Transient e) {
                    consecutive.merge(agentName, 1, Integer::sum);
                } catch (Fatal e) {
                    consecutive.merge(agentName, 1, Integer::sum);
                    return new Outcome(null, "fatal: " + e.getMessage());
                }
            }
            return new Outcome(null, "retries exhausted");
        };

        for (Map<String, Object> task : plan) {
            String tid = (String) task.get("id");
            calls.put(tid, 0);
            if (store.containsKey(tid)) {
                done.put(tid, store.get(tid));
                resumed.add(tid);
                continue;
            }
            List<String> needs = task.get("needs") == null ? List.of() : (List<String>) task.get("needs");
            String missing = null;
            for (String n : needs) {
                if (!done.containsKey(n)) {
                    missing = n;
                    break;
                }
            }
            if (missing != null) {
                skipped.put(tid, "dependency failed: " + missing);
                continue;
            }
            Map<String, String> inputs = new LinkedHashMap<>();
            for (String n : needs) inputs.put(n, done.get(n));
            Outcome outcome = attempt.apply(new Object[] {task.get("agent"), task.get("key"), inputs, tid});
            if (outcome.result() == null && task.get("fallback") != null) {
                Outcome second = attempt.apply(new Object[] {task.get("fallback"), task.get("key") + ":fallback", inputs, tid});
                if (second.result() != null) {
                    done.put(tid, second.result());
                    degraded.add(tid);
                    continue;
                }
            }
            if (outcome.result() == null) {
                failed.put(tid, outcome.reason());
                continue;
            }
            done.put(tid, outcome.result());
            store.put(tid, outcome.result());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("done", done);
        out.put("failed", failed);
        out.put("skipped", skipped);
        out.put("degraded", degraded);
        out.put("attempts", calls);
        out.put("resumed", resumed);
        return out;
    }
}
