import static harness.Show.py;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
 *
 * <p>The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
 * answer. The refund service, the orders and the limits are illustrative.
 */
public final class ErrorFlow {
    /** A failure a tool reports about itself: its kind, a message the model can use, an optional wait the service asked for and an explanation for the customer. */
    static final class ToolError extends RuntimeException {
        final String kind;
        final Integer retryAfterMs;
        final String explanation;

        ToolError(String kind, String message, Integer retryAfterMs, String explanation) {
            super(message);
            this.kind = kind;
            this.retryAfterMs = retryAfterMs;
            this.explanation = explanation;
        }

        ToolError(String kind, String message) {
            this(kind, message, null, null);
        }
    }

    static final Map<String, String> RETRYABLE = Map.of("transient", "yes", "validation", "no", "permission", "no", "business", "no", "outcome_unknown", "no", "internal", "no");
    static final Map<String, String> ACTION = Map.of("transient", "retry later", "validation", "repair the input", "permission", "escalate", "business", "explain to the customer",
        "outcome_unknown", "check the state first", "internal", "escalate");

    /** What the loop hands the model: an error flag, the kind of failure, the text, the attempts made, and whether a success was empty. */
    record Result(boolean isError, String kind, Object content, int attempts, boolean empty) {}

    /** The result of a run and the waits it made between attempts. */
    record Run(Result result, List<Integer> waits) {}

    /** A tool: it takes the call's arguments and returns a value or throws. */
    interface ToolFn {
        Object call(Map<String, Object> args) throws Exception;
    }

    /** What a run may be told: an idempotency key, and whether the call only reads. */
    record Options(String key, boolean readOnly) {
        static final Options NONE = new Options(null, false);
    }

    static Result failure(String kind, String message, int attempts, String explanation) {
        String text = kind + " error (retryable: " + RETRYABLE.get(kind) + "): " + message;
        if (explanation != null && !explanation.isEmpty()) text += " Tell the customer: " + explanation;
        return new Result(true, kind, text, attempts, false);
    }

    static boolean isEmpty(Object value) {
        return value == null || "".equals(value) || (value instanceof List<?> l && l.isEmpty());
    }

    /** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
    static Run run(ToolFn tool, Map<String, Object> args, Options options, int maxRetries, int baseMs) {
        List<Integer> waits = new ArrayList<>();
        int attempts = 0;
        while (true) {
            attempts++;
            Map<String, Object> call = new HashMap<>(args);
            if (options.key() != null) call.put("idempotency_key", options.key());
            try {
                Object value = tool.call(call);
                return new Run(new Result(false, null, value, attempts, isEmpty(value)), waits);
            } catch (ToolError error) {
                String kind = error.kind;
                if (kind.equals("timeout")) {
                    if (options.key() == null && !options.readOnly()) {
                        return new Run(failure("outcome_unknown", error.getMessage() + " The call may have taken effect: check the current state before trying again.", attempts, null), waits);
                    }
                    kind = "transient";
                }
                if (!kind.equals("transient")) return new Run(failure(kind, error.getMessage(), attempts, error.explanation), waits);
                if (attempts > maxRetries) return new Run(failure("transient", error.getMessage() + " Gave up after " + attempts + " attempts.", attempts, null), waits);
                waits.add(error.retryAfterMs != null ? error.retryAfterMs : baseMs * (1 << (attempts - 1)));
            } catch (Exception error) {
                return new Run(failure("internal", "unexpected failure in the tool: " + error.getMessage(), attempts, null), waits);
            }
        }
    }

    static Run run(ToolFn tool, Map<String, Object> args, Options options) {
        return run(tool, args, options, 2, 100);
    }

    static Run run(ToolFn tool, Map<String, Object> args) {
        return run(tool, args, Options.NONE);
    }

    static String nextStep(Result result) {
        if (!result.isError()) return result.empty() ? "accept the empty result" : "continue";
        return ACTION.get(result.kind());
    }

    /** A tool that does what the script says, one entry per call; the last entry repeats. */
    static final class Scripted implements ToolFn {
        final Object[] steps;
        int n = 0;
        final List<String> seen = new ArrayList<>();

        Scripted(Object... steps) {
            this.steps = steps;
        }

        @Override
        public Object call(Map<String, Object> args) throws Exception {
            seen.add((String) args.get("idempotency_key"));
            Object step = steps[Math.min(n, steps.length - 1)];
            n++;
            if (step instanceof Exception e) throw e;
            return step;
        }
    }

    record Scenario(String title, Scripted tool, Options options) {}

    static List<Scenario> scenarios() {
        return List.of(
            new Scenario("get_order order=A-7", new Scripted(new ToolError("transient", "The order service is busy."), new ToolError("transient", "The order service is busy."), "order A-7: 2 items"), Options.NONE),
            new Scenario("process_refund amount=-5", new Scripted(new ToolError("validation", "amount must be a positive whole number, for example 40")), Options.NONE),
            new Scenario("process_refund amount=900", new Scripted(new ToolError("business", "Refunds above 500 need a person.", null, "A colleague will contact you about this refund.")), Options.NONE),
            new Scenario("process_refund amount=40, no key", new Scripted(new ToolError("timeout", "No answer from the refund service.")), Options.NONE),
            new Scenario("process_refund amount=40, key refund-A-7-1", new Scripted(new ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), new Options("refund-A-7-1", false)),
            new Scenario("list_orders customer=C-9", new Scripted(List.of()), new Options(null, true)),
            new Scenario("process_refund amount=40, tool bug", new Scripted(new RuntimeException("the currency table is missing")), Options.NONE));
    }

    public static void main(String[] args) {
        int number = 0;
        for (Scenario s : scenarios()) {
            number++;
            Run r = run(s.tool(), Map.of("order", "A-7"), s.options());
            System.out.println(number + ". " + s.title());
            System.out.println("   attempts " + r.result().attempts() + ", waits " + py(r.waits()) + ", keys sent " + py(s.tool().seen));
            System.out.println("   tool_result is_error=" + (r.result().isError() ? "true" : "false") + ": " + py(r.result().content()));
            System.out.println("   next: " + nextStep(r.result()));
        }
    }
}
