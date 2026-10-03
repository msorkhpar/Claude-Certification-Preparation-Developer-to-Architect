import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */
final class Errors {
    private Errors() {}

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

    static Map<String, Object> makeError(String kind, String message, String explanation, int attempts, Map<String, Object> attempted) {
        // TODO: the structured result of a failed call.
        return null;
    }

    static Map<String, Object> makeError(String kind, String message) {
        return makeError(kind, message, null, 1, null);
    }

    static Map<String, Object> makeError(String kind, String message, String explanation) {
        return makeError(kind, message, explanation, 1, null);
    }

    static Map<String, Object> toToolResult(String toolUseId, Map<String, Object> result) {
        // TODO: the tool_result block for the API.
        return null;
    }

    static Map<String, Object> runTool(Function<Map<String, Object>, Object> tool, Map<String, Object> args, Map<String, Object> policy, IntConsumer sleep) {
        // TODO: call the tool, retry only what is safe to retry, return a result or a structured error.
        return null;
    }

    static String nextAction(Map<String, Object> result) {
        // TODO: what the loop does next with a result.
        return null;
    }
}
