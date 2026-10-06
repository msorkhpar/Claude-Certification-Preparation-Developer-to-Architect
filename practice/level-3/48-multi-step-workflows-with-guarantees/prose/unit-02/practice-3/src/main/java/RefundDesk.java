import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md. Results are JSON-like maps. */
final class RefundDesk {
    private static final System.Logger LOG = System.getLogger(RefundDesk.class.getName());
    private static final Map<String, String> MESSAGES = Map.of(
        "identity_required", "Verify the customer's identity before this action.",
        "order_not_owned", "That order does not belong to the verified customer.",
        "order_not_checked", "Look up the order before refunding it.",
        "bad_amount", "The amount must be a positive whole number of cents.",
        "exceeds_order", "The amount is more than what is left to refund on the order.",
        "needs_human", "Refunds over the limit need a person.",
        "locked", "Too many failed identity checks; escalate to a person.");
    private static final List<String> TOOLS = List.of("verify_identity", "lookup_order", "process_refund", "escalate");

    private final Map<String, Function<Map<String, Object>, Map<String, Object>>> backend;
    private final int limitCents;
    private String customer;
    private boolean locked;
    private int failures;
    private final Map<String, Map<String, Object>> orders = new LinkedHashMap<>();
    private final List<Map<String, Object>> refunds = new ArrayList<>();
    private final List<Map<String, Object>> blocked = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();

    RefundDesk(Map<String, Function<Map<String, Object>, Map<String, Object>>> backend) {
        this(backend, 10000);
    }

    RefundDesk(Map<String, Function<Map<String, Object>, Map<String, Object>>> backend, int limitCents) {
        this.backend = backend;
        this.limitCents = limitCents;
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<Map<String, Object>> copies(List<Map<String, Object>> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : list) out.add(new LinkedHashMap<>(m));
        return out;
    }

    Map<String, Object> state() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : orders.entrySet()) snapshot.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        return map("customer", customer, "locked", locked, "failures", failures, "orders", snapshot, "refunds", copies(refunds), "blocked", copies(blocked), "backend_calls", new ArrayList<>(calls));
    }

    private Map<String, Object> block(String tool, String code, String message) {
        blocked.add(map("tool", tool, "code", code));
        return map("content", "BLOCKED " + code + ": " + (message != null ? message : MESSAGES.get(code)), "is_error", true, "blocked", code);
    }

    private Map<String, Object> ok(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : result.entrySet()) sb.append(sb.length() == 0 ? "" : "; ").append(e.getKey()).append('=').append(e.getValue());
        return map("content", sb.toString(), "is_error", false, "blocked", null);
    }

    /** The backend's result, or the error result when it threw (result is then null and `failure` is set). */
    private Map<String, Object> failure;

    private Map<String, Object> run(String name, Map<String, Object> args) {
        calls.add(name);
        failure = null;
        try {
            return backend.get(name).apply(args);
        } catch (RuntimeException error) { // a backend failure is a result for the model and changes nothing here
            failure = map("content", String.valueOf(error.getMessage()), "is_error", true, "blocked", null);
            return null;
        }
    }

    Map<String, Object> call(String name, Map<String, Object> args) {
        LOG.log(System.Logger.Level.DEBUG, "call input: {0}", args);
        // TODO 7 of 8 (finish this to pass e6): the first check of call. When the tool name is not one of TOOLS, return
        //   the refusal `unknown_tool` with the message "Unknown tool: NAME". Example: call("delete_everything", {}) ->
        //   BLOCKED unknown_tool: Unknown tool: delete_everything.
        if (name.equals("escalate")) {
            Map<String, Object> result = run(name, args);
            return result == null ? failure : ok(result);
        }
        if (locked) return block(name, "locked", null);
        if (name.equals("verify_identity")) {
            Map<String, Object> result = run(name, args);
            if (result == null) return failure;
            // TODO 1 of 8 (finish this to pass m1, e5): what a `verify_identity` result does to the desk. Receives the
            //   backend's result map. A result with `verified` equal to `yes` and a `customer_id` sets the verified
            //   customer and resets the failure count to 0; any other result clears the verified customer, adds one
            //   failure and locks the desk at three. Example: three results of {verified: no} lock the desk.
            return ok(result);
        }
        // TODO 2 of 8 (finish this to pass e1): the prerequisite check. When no customer is verified, return the refusal
        //   `identity_required` for this call: block(name, "identity_required") builds and records it. Example:
        //   process_refund before verify_identity -> BLOCKED identity_required, and the backend is never called.
        if (name.equals("lookup_order")) {
            Map<String, Object> result = run(name, args);
            if (result == null) return failure;
            // TODO 3 of 8 (finish this to pass e2): the ownership check on a looked-up order. When the order's
            //   customer_id is not the verified customer, return the refusal `order_not_owned` before the order is
            //   remembered. Example: customer C1 looks up an order of C2 -> BLOCKED order_not_owned, and state()["orders"]
            //   stays empty.
            orders.put((String) result.get("order_id"), new LinkedHashMap<>(result));
            return ok(result);
        }
        Map<String, Object> order = orders.get(args.get("order_id"));
        if (order == null) return block(name, "order_not_checked", null);
        // TODO 4 of 8 (finish this to pass e3): the amount check. Receives the amount from the call. When it is not a
        //   whole number above zero (a boolean, a decimal, a string, a missing value or 0), return the refusal
        //   `bad_amount`. Example: amount_cents 0 or 12.5 or True -> BLOCKED bad_amount.
        if (!(args.get("amount_cents") instanceof Integer amount)) return block(name, "bad_amount", null);
        int refunded = (Integer) order.get("refunded_cents");
        // TODO 5 of 8 (finish this to pass e3): the check against the order. When the amount is more than the order's
        //   total_cents minus its refunded_cents, return the refusal `exceeds_order`. Example: 3000 refunded on a 5000
        //   order, then 2001 -> BLOCKED exceeds_order.
        // TODO 6 of 8 (finish this to pass e4): the authority limit. When the amount is above the desk's limit, return
        //   the refusal `needs_human` and do not call the backend. Example: limit 10000, amount 12000 on a 50000 order ->
        //   BLOCKED needs_human.
        Map<String, Object> result = run(name, args);
        if (result == null) return failure;
        order.put("refunded_cents", refunded + amount);
        refunds.add(map("order_id", order.get("order_id"), "amount_cents", amount, "refund_id", result.get("refund_id")));
        return ok(result);
    }

    Map<String, Object> handoff(String reason) {
        String last = blocked.isEmpty() ? null : (String) blocked.get(blocked.size() - 1).get("code");
        // TODO 8 of 8 (finish this to pass e4, e5): the recommended action of the hand-off. `last` is the code of the
        //   most recent refusal or none. Choose verify_identity_manually when the desk is locked, otherwise review_refund
        //   when last is needs_human, otherwise review_case. Example: locked desk -> verify_identity_manually.
        String action = "review_case";
        return map("customer_id", customer, "identity_verified", customer != null, "reason", reason, "orders_checked", new ArrayList<>(orders.keySet()),
            "refunds_done", copies(refunds), "blocked", copies(blocked), "recommended_action", action);
    }
}
