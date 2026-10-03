import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */
final class Errors {
    private Errors() {}

    private static final Map<String, Boolean> KINDS = Map.of("transient", true, "validation", false, "permission", false, "business", false, "outcome_unknown", false, "internal", true);
    private static final Set<String> GENERIC = Set.of("", "error", "failed", "failure", "operation failed", "something went wrong", "unknown error");
    private static final Map<String, String> ACTIONS = Map.of("transient", "retry_later", "validation", "repair_input", "permission", "escalate", "business", "explain", "outcome_unknown", "verify_first", "internal", "escalate");

    /** What a tool throws. kind is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown). */
    static final class ToolError extends RuntimeException {
        final String kind;
        final Integer retryAfterMs;
        final String explanation;

        ToolError(String kind, String message) {
            this(kind, message, null, null);
        }

        ToolError(String kind, String message, Integer retryAfterMs, String explanation) {
            super(message);
            this.kind = kind;
            this.retryAfterMs = retryAfterMs;
            this.explanation = explanation;
        }
    }

    /** The structured result of a failed call. */
    static Map<String, Object> makeError(String kind, String message, String explanation, int attempts, Map<String, Object> attempted) {
        if (!KINDS.containsKey(kind)) throw new IllegalArgumentException("unknown error kind: " + kind);
        String plain = message == null ? "" : message.trim().toLowerCase().replaceAll("\\.+$", "");
        if (GENERIC.contains(plain)) throw new IllegalArgumentException("an error message must say what went wrong and what to do");
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("is_error", true);
        error.put("category", kind);
        error.put("retryable", KINDS.get(kind));
        error.put("message", message.trim());
        error.put("attempts", attempts);
        if (explanation != null && !explanation.isEmpty()) error.put("explanation", explanation);
        if (attempted != null) error.put("attempted", attempted);
        return error;
    }

    static Map<String, Object> makeError(String kind, String message) {
        return makeError(kind, message, null, 1, null);
    }

    static Map<String, Object> makeError(String kind, String message, String explanation) {
        return makeError(kind, message, explanation, 1, null);
    }

    /** The tool_result block for the API: an error carries is_error true and its category, retry flag and message as text. */
    static Map<String, Object> toToolResult(String toolUseId, Map<String, Object> result) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "tool_result");
        block.put("tool_use_id", toolUseId);
        if (Boolean.TRUE.equals(result.get("is_error"))) {
            String text = result.get("category") + " error (retryable: " + (Boolean.TRUE.equals(result.get("retryable")) ? "yes" : "no") + "): " + result.get("message");
            if (result.get("explanation") != null) text += " Tell the customer: " + result.get("explanation");
            block.put("content", text);
            block.put("is_error", true);
        } else {
            Object content = result.get("content");
            block.put("content", content == null ? "" : String.valueOf(content));
            block.put("is_error", false);
        }
        return block;
    }

    private static boolean isEmpty(Object value) {
        return value == null || "".equals(value) || (value instanceof List<?> l && l.isEmpty()) || (value instanceof Map<?, ?> m && m.isEmpty());
    }

    /** Call a tool, recover locally from what is safe to recover from, and return a result or a structured error. */
    static Map<String, Object> runTool(Function<Map<String, Object>, Object> tool, Map<String, Object> args, Map<String, Object> policy, IntConsumer sleep) {
        String key = (String) policy.get("idempotency_key");
        int maxRetries = policy.containsKey("max_retries") ? (Integer) policy.get("max_retries") : 2;
        int base = policy.containsKey("base_delay_ms") ? (Integer) policy.get("base_delay_ms") : 100;
        boolean safeToRepeat = Boolean.TRUE.equals(policy.get("read_only")) || (key != null && !key.isEmpty());
        int attempts = 0;
        while (true) {
            attempts++;
            Map<String, Object> callArgs = new LinkedHashMap<>(args);
            if (key != null && !key.isEmpty()) callArgs.put("idempotency_key", key);
            try {
                Object value = tool.apply(callArgs);
                Map<String, Object> ok = new LinkedHashMap<>();
                ok.put("ok", true);
                ok.put("content", value);
                ok.put("empty", isEmpty(value));
                ok.put("attempts", attempts);
                return ok;
            } catch (ToolError error) {
                String kind = error.kind;
                if (kind.equals("timeout")) {
                    if (!safeToRepeat) return makeError("outcome_unknown", error.getMessage() + " The call may have taken effect: check the current state before trying again.", null, attempts, new LinkedHashMap<>(args));
                    kind = "transient";
                }
                if (!kind.equals("transient")) return makeError(kind, error.getMessage(), error.explanation, attempts, new LinkedHashMap<>(args));
                if (attempts > maxRetries) return makeError("transient", error.getMessage() + " Gave up after " + attempts + " attempts.", null, attempts, new LinkedHashMap<>(args));
                sleep.accept(error.retryAfterMs != null ? error.retryAfterMs : base * (1 << (attempts - 1)));
            } catch (RuntimeException error) { // a bug in the tool must not end the run
                return makeError("internal", "unexpected failure in the tool: " + error.getMessage(), null, attempts, new LinkedHashMap<>(args));
            }
        }
    }

    /** What the loop does next with a result. */
    static String nextAction(Map<String, Object> result) {
        if (!Boolean.TRUE.equals(result.get("is_error"))) return Boolean.TRUE.equals(result.get("empty")) ? "accept_empty" : "continue";
        return ACTIONS.get((String) result.get("category"));
    }
}
