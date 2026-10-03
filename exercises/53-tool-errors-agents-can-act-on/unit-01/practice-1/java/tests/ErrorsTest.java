import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ErrorsTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> args() {
        return map("order", "A-7", "amount", 40);
    }

    private static Map<String, Object> policy(int maxRetries, Object... more) {
        Map<String, Object> p = map("max_retries", maxRetries, "base_delay_ms", 100);
        for (int i = 0; i < more.length; i += 2) p.put((String) more[i], more[i + 1]);
        return p;
    }

    /** A scripted tool: each call takes the next script entry (a RuntimeException is thrown, anything else is returned); every call is kept. */
    private static final class Tool implements Function<Map<String, Object>, Object> {
        final List<Object> script = new ArrayList<>();
        final List<Map<String, Object>> calls = new ArrayList<>();

        Tool(Object... steps) {
            for (Object s : steps) script.add(s);
        }

        public Object apply(Map<String, Object> a) {
            calls.add(new LinkedHashMap<>(a));
            Object step = script.size() > 1 ? script.remove(0) : script.get(0);
            if (step instanceof RuntimeException e) throw e;
            return step;
        }
    }

    private record Run(Map<String, Object> result, List<Integer> waits) {}

    private static Run run(Tool tool, Map<String, Object> policy, Map<String, Object> args) {
        List<Integer> waits = new ArrayList<>();
        Map<String, Object> result = Errors.runTool(tool, args, policy, waits::add);
        assertNotNull(result, "runTool returned nothing");
        return new Run(result, waits);
    }

    private static Run run(Tool tool) {
        return run(tool, policy(2), args());
    }

    private static Errors.ToolError err(String kind, String message) {
        return new Errors.ToolError(kind, message);
    }

    @Test
    void m1_aFailedCallBecomesAStructuredErrorWithACategoryARetryFlagAndAnErrorFlag() {
        Map<String, Object> error = Errors.makeError("transient", "The billing service timed out after 5 s.");
        assertNotNull(error);
        assertEquals(map("is_error", true, "category", "transient", "retryable", true, "message", "The billing service timed out after 5 s.", "attempts", 1), error);
        assertEquals(map("type", "tool_result", "tool_use_id", "toolu_1", "content", "transient error (retryable: yes): The billing service timed out after 5 s.", "is_error", true), Errors.toToolResult("toolu_1", error));
        Map<String, Object> business = Errors.makeError("business", "Refunds above 500 need a person.", "A colleague will contact you about this refund.");
        assertEquals(false, business.get("retryable"));
        assertEquals("A colleague will contact you about this refund.", business.get("explanation"));
        assertEquals("business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.", Errors.toToolResult("toolu_2", business).get("content"));
        for (String kind : List.of("validation", "permission", "outcome_unknown", "internal")) assertEquals(false, Errors.makeError(kind, "Specific text.").get("retryable"));
        assertEquals(map("type", "tool_result", "tool_use_id", "toolu_3", "content", "refund R-1 created", "is_error", false), Errors.toToolResult("toolu_3", map("ok", true, "content", "refund R-1 created")));
    }

    @Test
    void e1_aGenericMessageOrAnUnknownCategoryIsRefused() {
        int refused = 0;
        for (String message : List.of("Operation failed", "", "   ", "Failed.", "Error", "Something went wrong")) {
            try {
                Errors.makeError("transient", message);
            } catch (IllegalArgumentException expected) {
                refused++;
            }
        }
        assertEquals(6, refused);
        refused = 0;
        for (String kind : List.of("oops", "timeout", "", "Transient")) {
            try {
                Errors.makeError(kind, "A specific message that says what to change.");
            } catch (IllegalArgumentException expected) {
                refused++;
            }
        }
        assertEquals(4, refused);
    }

    @Test
    void e2_onlyTransientFailuresAreRetriedWithGrowingWaitsAndTheOtherKindsReturnAtOnce() {
        Tool tool = new Tool(err("transient", "Billing is unavailable."), err("transient", "Billing is unavailable."), "refund R-1 created");
        Run r = run(tool);
        assertEquals(true, r.result().get("ok"));
        assertEquals("refund R-1 created", r.result().get("content"));
        assertEquals(3, r.result().get("attempts"));
        assertEquals(List.of(100, 200), r.waits());
        assertEquals(3, tool.calls.size());
        String[][] cases = {{"validation", "amount must be a positive whole number", null}, {"permission", "this key may not issue refunds", null}, {"business", "Refunds above 500 need a person.", "A colleague will contact you."}};
        for (String[] c : cases) {
            tool = new Tool(new Errors.ToolError(c[0], c[1], null, c[2]), "never reached");
            r = run(tool);
            assertEquals(true, r.result().get("is_error"));
            assertEquals(c[0], r.result().get("category"));
            assertEquals(false, r.result().get("retryable"));
            assertEquals(c[1], r.result().get("message"));
            assertEquals(c[2], r.result().get("explanation"));
            assertEquals(1, r.result().get("attempts"));
            assertEquals(args(), r.result().get("attempted"));
            assertTrue(r.waits().isEmpty());
            assertEquals(1, tool.calls.size());
        }
    }

    @Test
    void e3_theRetriesAreBoundedAndAWaitTheServiceAsksForIsHonoured() {
        Tool tool = new Tool(err("transient", "Billing is unavailable."));
        Run r = run(tool);
        assertEquals(3, tool.calls.size());
        assertEquals(List.of(100, 200), r.waits());
        assertEquals(true, r.result().get("is_error"));
        assertEquals("transient", r.result().get("category"));
        assertEquals(true, r.result().get("retryable"));
        assertEquals(3, r.result().get("attempts"));
        assertTrue(((String) r.result().get("message")).contains("Gave up after 3 attempts"));
        assertEquals(args(), r.result().get("attempted"));
        tool = new Tool(new Errors.ToolError("transient", "Rate limited.", 1500, null), err("transient", "Rate limited."), "ok");
        r = run(tool);
        assertEquals(List.of(1500, 200), r.waits());
        assertEquals(true, r.result().get("ok"));
        assertEquals(3, r.result().get("attempts"));
        tool = new Tool(err("transient", "Billing is unavailable."));
        r = run(tool, policy(0), args());
        assertEquals(1, tool.calls.size());
        assertTrue(r.waits().isEmpty());
        assertEquals(1, r.result().get("attempts"));
        assertEquals("transient", r.result().get("category"));
    }

    @Test
    void e4_aValidEmptyResultIsASuccessAndNotAnError() {
        for (Object empty : new Object[] {List.of(), "", Map.of(), null}) {
            Run r = run(new Tool(empty));
            assertEquals(true, r.result().get("ok"));
            assertEquals(true, r.result().get("empty"));
            assertEquals(1, r.result().get("attempts"));
            assertTrue(r.waits().isEmpty());
            assertEquals("accept_empty", Errors.nextAction(r.result()));
        }
        Map<String, Object> block = Errors.toToolResult("toolu_1", map("ok", true, "content", ""));
        assertEquals(false, block.get("is_error"));
        assertEquals("", block.get("content"));
        Run full = run(new Tool("3 orders"));
        assertEquals(false, full.result().get("empty"));
        assertEquals("continue", Errors.nextAction(full.result()));
    }

    @Test
    void e5_aTimeoutOnAWriteIsAnUnknownOutcomeAndIsRetriedOnlyWhenRepeatingItIsSafe() {
        Tool tool = new Tool(err("timeout", "No answer from the refund service."), "refund R-1 created");
        Run r = run(tool);
        assertEquals(1, tool.calls.size());
        assertTrue(r.waits().isEmpty());
        assertEquals(true, r.result().get("is_error"));
        assertEquals("outcome_unknown", r.result().get("category"));
        assertEquals(false, r.result().get("retryable"));
        String message = (String) r.result().get("message");
        assertTrue(message.contains("No answer from the refund service.") && message.contains("check the current state"));
        assertEquals(args(), r.result().get("attempted"));
        tool = new Tool(err("timeout", "No answer."), "3 orders");
        r = run(tool, policy(2, "read_only", true), args());
        assertEquals(true, r.result().get("ok"));
        assertEquals(2, r.result().get("attempts"));
        assertEquals(List.of(100), r.waits());
        tool = new Tool(err("timeout", "No answer."), err("timeout", "No answer."), "refund R-1 created");
        Map<String, Object> mine = args();
        r = run(tool, policy(2, "idempotency_key", "k-1"), mine);
        assertEquals(true, r.result().get("ok"));
        assertEquals(3, r.result().get("attempts"));
        assertEquals(List.of(100, 200), r.waits());
        Map<String, Object> keyed = args();
        keyed.put("idempotency_key", "k-1");
        assertEquals(List.of(keyed, keyed, keyed), tool.calls);
        assertEquals(args(), mine, "the caller's arguments must not be changed");
    }

    @Test
    void e6_theNextActionFollowsTheCategory() {
        Map<String, String> expected = Map.of("transient", "retry_later", "validation", "repair_input", "permission", "escalate", "business", "explain", "outcome_unknown", "verify_first", "internal", "escalate");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            Map<String, Object> error = Errors.makeError(e.getKey(), "A specific message.");
            assertNotNull(error);
            assertEquals(e.getValue(), Errors.nextAction(error));
        }
        assertEquals("continue", Errors.nextAction(map("ok", true, "content", "x", "empty", false)));
        assertEquals("accept_empty", Errors.nextAction(map("ok", true, "content", List.of(), "empty", true)));
    }

    @Test
    void e7_anUnexpectedExceptionBecomesAnInternalErrorAndTheRunGoesOn() {
        for (RuntimeException boom : List.of(new IllegalStateException("boom"), new NullPointerException("missing"))) {
            Tool tool = new Tool(boom, "never reached");
            Run r = run(tool);
            assertEquals(true, r.result().get("is_error"));
            assertEquals("internal", r.result().get("category"));
            assertEquals(false, r.result().get("retryable"));
            assertEquals(1, r.result().get("attempts"));
            assertTrue(r.waits().isEmpty());
            assertEquals(1, tool.calls.size());
        }
        Run r = run(new Tool(new IllegalStateException("boom")));
        assertTrue(((String) r.result().get("message")).contains("boom"));
        assertEquals(args(), r.result().get("attempted"));
    }
}
