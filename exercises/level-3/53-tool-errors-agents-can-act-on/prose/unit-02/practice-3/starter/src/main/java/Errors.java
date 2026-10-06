import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */
final class Errors {
    private static final System.Logger LOG = System.getLogger(Errors.class.getName());
    private Errors() {}

    private static final Map<String, Boolean> KINDS = Map.of("transient", true, "validation", false, "permission", false, "business", false, "outcome_unknown", false, "internal", false);
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
        // TODO 2 of 9 (finish this to pass e1): the refusals of make_error. Refuse with an error when the kind is not
        //   one of KINDS, and when the message, trimmed, lower-cased and without final periods, is in GENERIC. Example:
        //   make_error("transient", "Operation failed.") -> raises.
        Map<String, Object> error = new LinkedHashMap<>();
        // TODO 1 of 9 (finish this to pass m1): the structured error. Receives the kind, the message and the attempts.
        //   Build the map: is_error true, category the kind, retryable the flag KINDS holds for that kind, message
        //   trimmed, attempts. Example: make_error("transient", "Service busy, retry later") -> is_error true, retryable
        //   true.
        error.put("is_error", false);
        error.put("category", kind);
        error.put("message", message);
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
        // TODO 5 of 9 (finish this to pass e4): the empty check. Return true for none, an empty text, an empty list and
        //   an empty map, and false for anything else (0 and false are real values). Example: [] -> true, 0 -> false.
        return false;
    }

    /** Call a tool, recover locally from what is safe to recover from, and return a result or a structured error. */
    static Map<String, Object> runTool(Function<Map<String, Object>, Object> tool, Map<String, Object> args, Map<String, Object> policy, IntConsumer sleep) {
        LOG.log(System.Logger.Level.DEBUG, "runTool input: {0}", args);
        String key = (String) policy.get("idempotency_key");
        int maxRetries = policy.containsKey("max_retries") ? (Integer) policy.get("max_retries") : 2;
        int base = policy.containsKey("base_delay_ms") ? (Integer) policy.get("base_delay_ms") : 100;
        boolean safeToRepeat = Boolean.TRUE.equals(policy.get("read_only")) || (key != null && !key.isEmpty());
        int attempts = 0;
        while (true) {
            attempts++;
            Map<String, Object> callArgs = new LinkedHashMap<>(args);
            // TODO 7 of 9 (finish this to pass e5): the idempotency key. When the policy holds a key, add it to
            //   `call_args`, the copy of the arguments that this attempt sends (never to the caller's own map). Example:
            //   key "k-1" -> every attempt receives idempotency_key "k-1".
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
                // TODO 6 of 9 (finish this to pass e5): the timeout. When the kind is timeout: if the call is not safe
                //   to repeat, return make_error("outcome_unknown", the message plus "The call may have taken effect:
                //   check the current state before trying again.", attempts, a copy of the arguments); otherwise treat it
                //   as transient. Example: timeout on a write with no key -> outcome_unknown.
                if (kind.equals("timeout")) {
                    kind = "transient";
                }
                // TODO 3 of 9 (finish this to pass e2): the kinds that are not retried. When the kind is not transient,
                //   return the structured error at once (message and explanation of the tool error, the attempts so far, a
                //   copy of the arguments). Example: a validation error on the first attempt -> make_error(validation,
                //   ..., attempts=1) and no sleep.
                if (attempts > maxRetries) return makeError("transient", error.getMessage() + " Gave up after " + attempts + " attempts.", null, attempts, new LinkedHashMap<>(args));
                // TODO 4 of 9 (finish this to pass e2, e3): the wait before the next attempt, in milliseconds. Receives
                //   the tool error's retry-after value (or none), the base delay and the attempt number from 1. Sleep the
                //   value the service asked for when there is one, otherwise base times 2 to the power of attempts - 1.
                //   Example: no retry-after, base 100 -> waits 100, 200, 400.
                sleep.accept(base);
            } catch (RuntimeException error) {
                // TODO 9 of 9 (finish this to pass e7): the unexpected exception. When the tool raises something that is not
                //   a ToolError, return make_error("internal", "unexpected failure in the tool: " + its message, attempts, a
                //   copy of the arguments) instead of letting it end the run. Example: a tool that raises ValueError("boom")
                //   -> category internal.
                return makeError("transient", "unexpected failure in the tool: " + error.getMessage(), null, attempts, new LinkedHashMap<>(args));
            }
        }
    }

    /** What the loop does next with a result. */
    static String nextAction(Map<String, Object> result) {
        // TODO 8 of 9 (finish this to pass e6): the next action of the loop. Receives a result. For an error return the
        //   action ACTIONS holds for its category; for a success return accept_empty when it is empty, otherwise continue.
        //   Example: a permission error -> escalate.
        return "continue";
    }
}
