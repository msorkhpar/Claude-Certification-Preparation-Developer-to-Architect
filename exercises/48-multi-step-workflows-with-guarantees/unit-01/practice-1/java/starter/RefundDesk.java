import java.util.Map;
import java.util.function.Function;

/** A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md. Results are JSON-like maps. */
final class RefundDesk {
    private final Map<String, Function<Map<String, Object>, Map<String, Object>>> backend;
    private final int limitCents;

    RefundDesk(Map<String, Function<Map<String, Object>, Map<String, Object>>> backend) {
        this(backend, 10000);
    }

    RefundDesk(Map<String, Function<Map<String, Object>, Map<String, Object>>> backend, int limitCents) {
        this.backend = backend;
        this.limitCents = limitCents;
        // TODO: keep what the desk has verified, looked up, refunded and blocked.
    }

    Map<String, Object> state() {
        // TODO: a snapshot of the desk's state, as described in the statement.
        return null;
    }

    Map<String, Object> call(String name, Map<String, Object> args) {
        // TODO: check the prerequisites in code, then call the backend, and return {content, is_error, blocked}.
        return null;
    }

    Map<String, Object> handoff(String reason) {
        // TODO: the structured hand-off a person needs to take the case over.
        return null;
    }
}
