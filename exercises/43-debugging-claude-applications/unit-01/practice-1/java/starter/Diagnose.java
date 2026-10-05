import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Diagnose a failure from a trace. See ../../statement.md. */
final class Diagnose {
    private static final System.Logger LOG = System.getLogger(Diagnose.class.getName());
    private static final Map<Integer, String[]> HTTP = Map.of(
        400, new String[] {"invalid_request", "integration", "fix_request"},
        401, new String[] {"authentication", "account", "fix_credentials"},
        402, new String[] {"billing", "account", "fix_billing"},
        403, new String[] {"permission", "account", "fix_access"},
        404, new String[] {"not_found", "integration", "fix_request"},
        409, new String[] {"conflict", "integration", "resolve_then_retry"},
        413, new String[] {"request_too_large", "integration", "shrink_request"},
        500, new String[] {"server_error", "service", "retry_backoff"},
        504, new String[] {"timeout", "service", "stream_or_batch"},
        529, new String[] {"overloaded", "service", "retry_backoff"});
    private static final Map<String, String[]> STOP = Map.of(
        "max_tokens", new String[] {"truncated", "integration", "raise_max_tokens"},
        "model_context_window_exceeded", new String[] {"context_exceeded", "integration", "trim_context"},
        "refusal", new String[] {"refusal", "model", "fallback_model"},
        "pause_turn", new String[] {"paused", "integration", "continue_turn"});

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // TODO 1 of 7 (unlocks e1): the diagnosis of a 429.
    // Receives the error event. Returns {"rate_limit", "service", "wait_retry_after"} when its "headers" map has a "retry-after" key (this wins),
    // else {"spend_cap", "account", "wait_for_reset"} when "error_code" is "enforced_spend_limit_reached", else {"rate_limit", "service",
    // "retry_backoff"}. Example: a 429 with error_code enforced_spend_limit_reached and no headers -> the spend_cap triple
    private static String[] rateLimit(Map<String, Object> event) {
        return new String[] {"rate_limit", "service", "retry_backoff"};
    }

    // TODO 6 of 7 (unlocks e1 and m1): the triple for a status.
    // Receives the status number. Returns HTTP.get(status) for a documented status; any other status falls back by class: 500 and above
    // use the 500 row, everything else the 400 row. Example: byStatus(502) -> HTTP.get(500), byStatus(418) -> HTTP.get(400)
    private static String[] byStatus(int status) {
        return HTTP.get(400);
    }
    private static String[] http(Map<String, Object> event) {
        int status = event.get("status") instanceof Number n ? n.intValue() : 0;
        if (status == 429) return rateLimit(event);
        if (status == 400 && String.valueOf(event.getOrDefault("message", "")).toLowerCase().contains("spend limit")) return new String[] {"spend_limit", "account", "raise_limit"};
        return byStatus(status);
    }

    // TODO 2 of 7 (unlocks e4): does the text hold a JSON object?
    // Receives a text. Returns true when the span from the first '{' to the last '}' parses as JSON (use Json.parse) and is an object
    // (a Map, not a list or a number); false for no braces, a broken span or another JSON type.
    // Example: hasJsonObject("ok {\"a\": 1} done") -> true, hasJsonObject("see {nope}") -> false
    private static boolean hasJsonObject(String text) {
        return false;
    }

    // TODO 3 of 7 (unlocks e3): who is to blame for an empty end turn?
    // Receives the block types of the last user message, in order. Returns {"empty_response", "integration", "remove_text_after_tool_result"}
    // when a "text" block comes after a "tool_result" block, else {"empty_response", "model", "add_continue_prompt"}.
    // Example: ["tool_result", "text"] -> integration; ["text", "tool_result"] -> model
    private static String[] emptyOrigin(List<String> lastBlocks) {
        return new String[] {"empty_response", "model", "add_continue_prompt"};
    }

    // TODO 4 of 7 (unlocks e5): is this tool event a failure, and whose?
    // Receives an event and the tool names of the last request (null when unknown). Returns {"unknown_tool", "model", "return_error_result"}
    // for a "tool_call" whose name is not in tools, {"tool_exception", "integration", "fix_tool_code"} for a "tool_result" with a non-empty
    // "exception" string, and null otherwise (an is_error flag alone is not our failure).
    // Example: a tool_call named get_wether with tools [get_weather] -> the unknown_tool triple
    private static String[] toolFailure(Map<String, Object> event, List<String> tools) {
        return null;
    }

    // TODO 5 of 7 (unlocks e6): did a later response recover from the failure at index i?
    // Receives the trace and the index of the failure. Returns true when a later event is a response with status 200, stop_reason
    // "end_turn" and a non-empty content list; false otherwise. Example: a failure at 1 and a good end_turn at 5 -> true
    private static boolean recoveredAfter(List<Map<String, Object>> trace, int i) {
        return false;
    }

    // TODO 7 of 7 (unlocks e2): the triple for a response that succeeded but stopped for a bad reason.
    // Receives the stop_reason (maybe null). Returns STOP.get(reason) when the reason is in the STOP table, else null (end_turn, stop_sequence and tool_use are fine).
    // Example: stopFailure("refusal") -> {"refusal", "model", "fallback_model"}, stopFailure("end_turn") -> null
    private static String[] stopFailure(String reason) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String[] classify(Map<String, Object> event, List<String> tools, List<String> lastBlocks) {
        String kind = (String) event.get("kind");
        if (kind == null) return null;
        switch (kind) {
            case "error":
                return http(event);
            case "network_error":
                return new String[] {"network", "service", "retry_backoff"};
            case "response":
                if (event.get("status") instanceof Number n && n.intValue() == 200) {
                    String reason = (String) event.get("stop_reason");
                    String[] stopped = stopFailure(reason);
                    if (stopped != null) return stopped;
                    if ("end_turn".equals(reason) && !(event.get("content") instanceof List<?> c && !c.isEmpty())) return emptyOrigin(lastBlocks);
                }
                return null;
            case "tool_call":
            case "tool_result":
                return toolFailure(event, tools);
            case "parse":
                if (Boolean.FALSE.equals(event.get("ok"))) {
                    return hasJsonObject(String.valueOf(event.getOrDefault("text", ""))) ? new String[] {"parse_failure", "integration", "extract_json"}
                        : new String[] {"parse_failure", "model", "validate_and_retry"};
                }
                return null;
            default:
                return null;
        }
    }

    /** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> diagnose(List<Map<String, Object>> trace) {
        LOG.log(System.Logger.Level.DEBUG, "diagnose input: {0}", trace);
        List<String> tools = null, lastBlocks = List.of();
        for (int i = 0; i < trace.size(); i++) {
            Map<String, Object> event = trace.get(i);
            if ("request".equals(event.get("kind"))) {
                tools = event.get("tools") instanceof List<?> t ? (List<String>) t : null;
                lastBlocks = event.get("last_user_blocks") instanceof List<?> b ? (List<String>) b : List.of();
                continue;
            }
            String[] found = classify(event, tools, lastBlocks);
            if (found != null) {
                boolean recovered = recoveredAfter(trace, i);
                return map("index", i, "type", found[0], "origin", found[1], "recovery", found[2], "recovered", recovered);
            }
        }
        return map("index", -1, "type", "ok", "origin", "none", "recovery", "none", "recovered", false);
    }
}
