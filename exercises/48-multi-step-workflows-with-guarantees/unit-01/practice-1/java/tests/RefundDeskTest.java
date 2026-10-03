import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RefundDeskTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Map<String, Object>> orders() {
        Map<String, Map<String, Object>> o = new LinkedHashMap<>();
        o.put("O1", map("order_id", "O1", "customer_id", "C1", "total_cents", 5000, "refunded_cents", 0));
        o.put("O2", map("order_id", "O2", "customer_id", "C2", "total_cents", 3000, "refunded_cents", 0));
        o.put("O9", map("order_id", "O9", "customer_id", "C1", "total_cents", 50000, "refunded_cents", 0));
        return o;
    }

    /** A scripted backend that keeps a log of every call it receives. */
    private static final class Backend extends LinkedHashMap<String, Function<Map<String, Object>, Map<String, Object>>> {
        final List<String> log = new ArrayList<>();
        int refunds = 0;

        Backend(boolean breakRefund) {
            Map<String, Map<String, Object>> table = orders();
            put("verify_identity", a -> {
                log.add("verify_identity");
                return "1234".equals(a.get("code")) ? map("verified", "yes", "customer_id", "C1") : map("verified", "no");
            });
            put("lookup_order", a -> {
                log.add("lookup_order");
                return new LinkedHashMap<>(table.get((String) a.get("order_id")));
            });
            put("process_refund", a -> {
                log.add("process_refund");
                if (breakRefund) throw new IllegalStateException("payment service offline");
                refunds++;
                return map("refund_id", "R" + refunds, "amount_cents", a.get("amount_cents"));
            });
            put("escalate", a -> {
                log.add("escalate");
                return map("ticket_id", "T1");
            });
        }
    }

    private static final class Made {
        final RefundDesk desk;
        final Backend backend;

        Made(boolean breakRefund, int limit) {
            backend = new Backend(breakRefund);
            desk = new RefundDesk(backend, limit);
        }
    }

    private static Made desk() {
        return new Made(false, 10000);
    }

    private static Map<String, Object> res(RefundDesk d, String name, Object... args) {
        Map<String, Object> out = d.call(name, map(args));
        assertNotNull(out, "call returned null");
        return out;
    }

    private static Made verified(boolean breakRefund, int limit) {
        Made m = new Made(breakRefund, limit);
        res(m.desk, "verify_identity", "code", "1234");
        return m;
    }

    private static Map<String, Object> blockedResult(String content, String code) {
        return map("content", content, "is_error", true, "blocked", code);
    }

    private static Map<String, Object> state(RefundDesk d) {
        Map<String, Object> s = d.state();
        assertNotNull(s, "state returned null");
        return s;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> order(Map<String, Object> state, String id) {
        return ((Map<String, Map<String, Object>>) state.get("orders")).get(id);
    }

    @Test
    void m1_aVerifiedCustomerCanLookUpAnOrderAndBeRefundedWithinTheLimit() {
        Made m = desk();
        assertEquals(map("content", "verified=yes; customer_id=C1", "is_error", false, "blocked", null), res(m.desk, "verify_identity", "code", "1234"));
        assertEquals("order_id=O1; customer_id=C1; total_cents=5000; refunded_cents=0", res(m.desk, "lookup_order", "order_id", "O1").get("content"));
        assertEquals(map("content", "refund_id=R1; amount_cents=2000", "is_error", false, "blocked", null), res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 2000));
        Map<String, Object> s = state(m.desk);
        assertEquals(Arrays.asList("C1", false, 0, List.of()), Arrays.asList(s.get("customer"), s.get("locked"), s.get("failures"), s.get("blocked")));
        assertEquals(List.of(map("order_id", "O1", "amount_cents", 2000, "refund_id", "R1")), s.get("refunds"));
        assertEquals(2000, order(s, "O1").get("refunded_cents"));
        assertEquals(List.of("verify_identity", "lookup_order", "process_refund"), m.backend.log);
    }

    @Test
    void e1_aRefundBeforeIdentityIsVerifiedIsBlockedInCodeAndNeverReachesTheBackend() {
        Made m = desk();
        assertEquals(blockedResult("BLOCKED identity_required: Verify the customer's identity before this action.", "identity_required"), res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 500));
        assertEquals(List.of(), m.backend.log);
        assertEquals(List.of(map("tool", "process_refund", "code", "identity_required")), state(m.desk).get("blocked"));
        res(m.desk, "verify_identity", "code", "1234");
        res(m.desk, "lookup_order", "order_id", "O1");
        assertNull(res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 500).get("blocked"));
        assertEquals("process_refund", m.backend.log.get(m.backend.log.size() - 1));
    }

    @Test
    void e2_anOrderThatBelongsToSomeoneElseIsNeitherShownNorRefundable() {
        Made m = desk();
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id", "O1").get("blocked"));
        assertEquals(List.of(), m.backend.log);
        res(m.desk, "verify_identity", "code", "1234");
        Map<String, Object> other = res(m.desk, "lookup_order", "order_id", "O2");
        assertEquals(blockedResult("BLOCKED order_not_owned: That order does not belong to the verified customer.", "order_not_owned"), other);
        assertFalse(((String) other.get("content")).contains("C2"));
        assertNull(order(state(m.desk), "O2"));
        assertEquals("order_not_checked", res(m.desk, "process_refund", "order_id", "O2", "amount_cents", 100).get("blocked"));
        assertFalse(m.backend.log.contains("process_refund"));
    }

    @Test
    void e3_aRefundIsCheckedAgainstTheOrderItsAmountAndWhatIsLeft() {
        Made m = verified(false, 10000);
        assertEquals("order_not_checked", res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 100).get("blocked"));
        res(m.desk, "lookup_order", "order_id", "O1");
        for (Object bad : new Object[] {0, -5, 12.5, "100", true, null}) {
            assertEquals("bad_amount", res(m.desk, "process_refund", "order_id", "O1", "amount_cents", bad).get("blocked"), String.valueOf(bad));
        }
        assertEquals("exceeds_order", res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 5001).get("blocked"));
        assertNull(res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 3000).get("blocked"));
        Map<String, Object> out = res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 2500);
        assertEquals(Arrays.asList("exceeds_order", "BLOCKED exceeds_order: The amount is more than what is left to refund on the order."), Arrays.asList(out.get("blocked"), out.get("content")));
        assertNull(res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 2000).get("blocked"));
        assertEquals(2, m.backend.log.stream().filter(c -> c.equals("process_refund")).count());
    }

    @Test
    void e4_aRefundOverTheLimitIsNotExecutedAndBecomesAStructuredHandOff() {
        Made m = verified(false, 10000);
        res(m.desk, "lookup_order", "order_id", "O9");
        Map<String, Object> out = res(m.desk, "process_refund", "order_id", "O9", "amount_cents", 25000);
        assertEquals(Arrays.asList("needs_human", true, "BLOCKED needs_human: Refunds over the limit need a person."), Arrays.asList(out.get("blocked"), out.get("is_error"), out.get("content")));
        assertFalse(m.backend.log.contains("process_refund"));
        assertNull(res(m.desk, "process_refund", "order_id", "O9", "amount_cents", 10000).get("blocked"));
        assertEquals("needs_human", res(m.desk, "process_refund", "order_id", "O9", "amount_cents", 10001).get("blocked"));
        Map<String, Object> handoff = m.desk.handoff("Customer asks for a 250.00 refund");
        assertNotNull(handoff, "handoff returned null");
        assertEquals(map("customer_id", "C1", "identity_verified", true, "reason", "Customer asks for a 250.00 refund", "orders_checked", List.of("O9"),
            "refunds_done", List.of(map("order_id", "O9", "amount_cents", 10000, "refund_id", "R1")),
            "blocked", List.of(map("tool", "process_refund", "code", "needs_human"), map("tool", "process_refund", "code", "needs_human")), "recommended_action", "review_refund"), handoff);
    }

    @Test
    void e5_aFailedCheckDoesNotUnlockAnythingAndThreeInARowLockTheDesk() {
        Made m = desk();
        assertEquals("verified=no", res(m.desk, "verify_identity", "code", "0000").get("content"));
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id", "O1").get("blocked"));
        res(m.desk, "verify_identity", "code", "1234");
        res(m.desk, "verify_identity", "code", "0000");
        assertNull(state(m.desk).get("customer"));
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id", "O1").get("blocked"));
        res(m.desk, "verify_identity", "code", "0000");
        res(m.desk, "verify_identity", "code", "0000");
        Map<String, Object> s = state(m.desk);
        assertEquals(Arrays.asList(true, 3), Arrays.asList(s.get("locked"), s.get("failures")));
        assertEquals(blockedResult("BLOCKED locked: Too many failed identity checks; escalate to a person.", "locked"), res(m.desk, "verify_identity", "code", "1234"));
        assertEquals("ticket_id=T1", res(m.desk, "escalate", "reason", "locked out").get("content"));
        Map<String, Object> handoff = m.desk.handoff("identity could not be verified");
        assertNotNull(handoff, "handoff returned null");
        assertEquals("verify_identity_manually", handoff.get("recommended_action"));
    }

    @Test
    void e6_unknownToolsAndBackendErrorsAreReportedAndTheHandOffListsEveryBlock() {
        Made m = verified(true, 10000);
        res(m.desk, "lookup_order", "order_id", "O1");
        assertEquals(blockedResult("BLOCKED unknown_tool: Unknown tool: delete_everything", "unknown_tool"), res(m.desk, "delete_everything"));
        assertEquals(map("content", "payment service offline", "is_error", true, "blocked", null), res(m.desk, "process_refund", "order_id", "O1", "amount_cents", 500));
        Map<String, Object> s = state(m.desk);
        assertEquals(List.of(), s.get("refunds"));
        assertEquals(0, order(s, "O1").get("refunded_cents"));
        Map<String, Object> handoff = m.desk.handoff("the payment service is down");
        assertNotNull(handoff, "handoff returned null");
        assertEquals(List.of(map("tool", "delete_everything", "code", "unknown_tool")), handoff.get("blocked"));
        assertEquals(Arrays.asList(List.of(), "review_case", List.of("O1")), Arrays.asList(handoff.get("refunds_done"), handoff.get("recommended_action"), handoff.get("orders_checked")));
    }
}
