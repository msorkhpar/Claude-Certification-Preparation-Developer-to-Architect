import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
 *
 * <p>The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
 * runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
 * design, not an Anthropic interface.
 */
public final class SupportDesk {
    private static final System.Logger LOG = System.getLogger(SupportDesk.class.getName());
    static final int LIMIT = 10_000; // cents: a refund above it is a person's decision
    static final int STALL = 3; // the same call this many times in a row is no progress

    static final Map<String, String[]> CUSTOMERS = new LinkedHashMap<>();
    static final Map<String, Object[]> ORDERS = new LinkedHashMap<>();

    static {
        CUSTOMERS.put("C1", new String[] {"Ana Silva", "ana@example.com"});
        CUSTOMERS.put("C2", new String[] {"Ana Silva", "ana.s@example.com"});
        CUSTOMERS.put("C3", new String[] {"Ben Ortiz", "ben@example.com"});
        ORDERS.put("O1", new Object[] {"C3", 5000});
        ORDERS.put("O2", new Object[] {"C3", 30000});
        ORDERS.put("O3", new Object[] {"C1", 4000});
    }

    record Call(String tool, Map<String, Object> args) {}

    record Result(boolean ok, String code, boolean retryable, String message, String content) {}

    record Escalation(String trigger, String reason, String customer, boolean verified, List<String> orders, List<String> refunds, List<String> refused) {}

    /** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
    static final class Desk {
        String customer;
        final List<String> checked = new ArrayList<>(), refunds = new ArrayList<>(), refused = new ArrayList<>(), backend = new ArrayList<>(), recent = new ArrayList<>();
        Escalation escalation;
        String lastCode;
        int retries;
        final java.util.Set<String> flaky = new java.util.HashSet<>(List.of("O2")); // the first lookup of this order fails with a transient fault

        Result fail(String code, String message, boolean retryable) {
            lastCode = code;
            if (!code.equals("transient") && !code.equals("not_found")) refused.add(code);
            return new Result(false, code, retryable, message, null);
        }

        Result fail(String code, String message) {
            return fail(code, message, false);
        }

        /** The record a person reads, built from the desk's own state and not from the model's account. */
        Result escalate(String trigger, String reason) {
            escalation = new Escalation(trigger, reason, customer, customer != null, checked.stream().sorted().toList(), List.copyOf(refunds), List.copyOf(refused));
            return new Result(true, null, false, null, "handed over");
        }

        Result call(String tool, Map<String, Object> args) {
            lastCode = null;
            recent.add(tool + new TreeMap<>(args));
            if (recent.size() > STALL) recent.remove(0);
            if (tool.equals("escalate_to_human")) return escalate((String) args.get("trigger"), (String) args.get("reason")); // the way to a person never waits for a prerequisite
            if (recent.size() == STALL && recent.stream().distinct().count() == 1) return escalate("stalled", tool + " repeated " + STALL + " times without progress");
            if (tool.equals("get_customer")) {
                List<String> found = new ArrayList<>();
                CUSTOMERS.forEach((id, c) -> {
                    if (args.get("query").equals(c[0]) || args.get("query").equals(c[1])) found.add(id);
                });
                if (found.size() > 1) return fail("ambiguous_match", found.size() + " customers match. Ask for the e-mail address. Do not pick one.");
                if (found.isEmpty()) return fail("not_found", "No customer matches. Ask for the e-mail address.");
                customer = found.get(0);
                return new Result(true, null, false, null, "customer_id=" + found.get(0));
            }
            if (customer == null) return fail("identity_required", "Identify the customer with get_customer before this action.");
            String id = (String) args.get("order_id");
            Object[] order = ORDERS.get(id);
            if (tool.equals("lookup_order")) {
                backend.add(tool);
                if (flaky.remove(id)) return fail("transient", "The order service timed out. Retry.", true);
                if (order == null) return fail("not_found", "No such order.");
                if (!order[0].equals(customer)) return fail("order_not_owned", "That order does not belong to the verified customer.");
                checked.add(id);
                return new Result(true, null, false, null, "total_cents=" + order[1]);
            }
            if (tool.equals("process_refund")) {
                if (!checked.contains(id)) return fail("order_not_checked", "Look up the order before refunding it.");
                int amount = (Integer) args.get("amount_cents");
                if (amount > LIMIT) return fail("needs_human", "A refund above the limit is decided by a person. Escalate.");
                backend.add(tool);
                refunds.add(id + ":" + amount);
                return new Result(true, null, false, null, "refund_id=R" + refunds.size());
            }
            return fail("unknown_tool", "No tool named " + tool + ".");
        }
    }

    /** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
    static Map.Entry<Desk, String> run(List<Call> script) {
        Desk desk = new Desk();
        for (Call c : script) {
            Result result = desk.call(c.tool(), c.args());
            if (!result.ok() && result.retryable()) {
                desk.retries++;
                desk.call(c.tool(), c.args());
            }
        }
        String outcome = desk.escalation != null ? "escalated" : "ambiguous_match".equals(desk.lastCode) ? "asked" : "resolved";
        return Map.entry(desk, outcome);
    }

    static String join(List<String> items) {
        return items.isEmpty() ? "-" : String.join(",", items);
    }

    static Call call(String tool, Object... kv) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) args.put((String) kv[i], kv[i + 1]);
        return new Call(tool, args);
    }

    static final Call BEN = call("get_customer", "query", "ben@example.com");

    static final Map<String, List<Call>> INCIDENTS = new LinkedHashMap<>();

    static {
        INCIDENTS.put("skips identity", List.of(call("lookup_order", "order_id", "O1"), BEN, call("lookup_order", "order_id", "O1"), call("process_refund", "order_id", "O1", "amount_cents", 2000)));
        INCIDENTS.put("transient fault", List.of(BEN, call("lookup_order", "order_id", "O2"), call("process_refund", "order_id", "O2", "amount_cents", 8000)));
        INCIDENTS.put("over the limit", List.of(BEN, call("lookup_order", "order_id", "O2"), call("process_refund", "order_id", "O2", "amount_cents", 25000),
            call("escalate_to_human", "trigger", "needs_human", "reason", "refund of 250.00 asked")));
        INCIDENTS.put("someone else's order", List.of(call("get_customer", "query", "ana@example.com"), call("lookup_order", "order_id", "O1")));
        INCIDENTS.put("two customers match", List.of(call("get_customer", "query", "Ana Silva")));
        INCIDENTS.put("asks for a person", List.of(call("escalate_to_human", "trigger", "customer_request", "reason", "customer asked for a person")));
        INCIDENTS.put("no progress", List.of(BEN, call("lookup_order", "order_id", "O9"), call("lookup_order", "order_id", "O9"), call("lookup_order", "order_id", "O9")));
    }

    public static void main(String[] args) {
        INCIDENTS.forEach((name, script) -> {
            var run = run(script);
            Desk desk = run.getKey();
            System.out.println(String.format("%-21s outcome=%-9s refused=%s retries=%d refunds=%s backend=%s", name, run.getValue(), join(desk.refused), desk.retries, join(desk.refunds), join(desk.backend)));
            if (desk.escalation != null) {
                Escalation e = desk.escalation;
                System.out.println("  handoff: trigger=" + e.trigger() + " verified=" + (e.verified() ? "yes" : "no") + " customer=" + (e.customer() == null ? "-" : e.customer())
                    + " orders=" + join(e.orders()) + " refunds=" + join(e.refunds()) + " refused=" + join(e.refused()));
            }
        });
    }
}
