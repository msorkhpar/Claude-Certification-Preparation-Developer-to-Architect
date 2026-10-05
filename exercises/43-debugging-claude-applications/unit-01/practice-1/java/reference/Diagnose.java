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

    private static String[] rateLimit(Map<String, Object> event) {
        if (event.get("headers") instanceof Map<?, ?> h && h.containsKey("retry-after")) return new String[] {"rate_limit", "service", "wait_retry_after"};
        if ("enforced_spend_limit_reached".equals(event.get("error_code"))) return new String[] {"spend_cap", "account", "wait_for_reset"};
        return new String[] {"rate_limit", "service", "retry_backoff"};
    }

    private static String[] byStatus(int status) {
        if (HTTP.containsKey(status)) return HTTP.get(status);
        return status >= 500 ? HTTP.get(500) : HTTP.get(400);
    }
    private static String[] http(Map<String, Object> event) {
        int status = event.get("status") instanceof Number n ? n.intValue() : 0;
        if (status == 429) return rateLimit(event);
        if (status == 400 && String.valueOf(event.getOrDefault("message", "")).toLowerCase().contains("spend limit")) return new String[] {"spend_limit", "account", "raise_limit"};
        return byStatus(status);
    }

    private static boolean hasJsonObject(String text) {
        int start = text.indexOf('{'), end = text.lastIndexOf('}');
        if (start < 0 || end < start) return false;
        try {
            return Json.parse(text.substring(start, end + 1)) instanceof Map<?, ?>;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String[] emptyOrigin(List<String> lastBlocks) {
        int at = lastBlocks.indexOf("tool_result");
        if (at >= 0 && lastBlocks.subList(at, lastBlocks.size()).contains("text")) return new String[] {"empty_response", "integration", "remove_text_after_tool_result"};
        return new String[] {"empty_response", "model", "add_continue_prompt"};
    }

    private static String[] toolFailure(Map<String, Object> event, List<String> tools) {
        Object kind = event.get("kind");
        if ("tool_call".equals(kind) && tools != null && !tools.contains(event.get("name"))) return new String[] {"unknown_tool", "model", "return_error_result"};
        if ("tool_result".equals(kind) && event.get("exception") instanceof String s && !s.isEmpty()) return new String[] {"tool_exception", "integration", "fix_tool_code"};
        return null;
    }

    private static boolean recoveredAfter(List<Map<String, Object>> trace, int i) {
        for (Map<String, Object> e : trace.subList(i + 1, trace.size())) {
            if ("response".equals(e.get("kind")) && e.get("status") instanceof Number n && n.intValue() == 200 && "end_turn".equals(e.get("stop_reason"))
                && e.get("content") instanceof List<?> c && !c.isEmpty()) return true;
        }
        return false;
    }

    /** The triple for a successful response that still failed by its stop reason, or null. */
    private static String[] stopFailure(String reason) {
        return reason == null ? null : STOP.get(reason);
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
