import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */
final class Distribute {
    private Distribute() {}

    /** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
    private static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object value) {
        return value == null ? List.of() : (List<String>) value;
    }

    private static boolean shares(Map<String, Object> tool, Set<String> tags) {
        for (String t : list(tool.get("tags"))) if (tags.contains(t)) return true;
        return false;
    }

    static Map<String, List<String>> assignTools(Map<String, Map<String, Object>> roles, List<Map<String, Object>> catalog) {
        return assignTools(roles, catalog, 5);
    }

    /** Give each role the tools of its specialisation, plus what it is explicitly granted, and refuse a set that is too large. */
    static Map<String, List<String>> assignTools(Map<String, Map<String, Object>> roles, List<Map<String, Object>> catalog, int budget) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> tool : catalog) {
            String name = (String) tool.get("name");
            if (byName.containsKey(name)) throw new IllegalArgumentException("duplicate tool name: " + name);
            byName.put(name, tool);
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> role : roles.entrySet()) {
            Set<String> tags = new HashSet<>(list(role.getValue().get("specialisation")));
            if (tags.isEmpty()) throw new IllegalArgumentException("role " + role.getKey() + " has no specialisation");
            List<String> names = new ArrayList<>();
            for (Map<String, Object> tool : catalog) if (shares(tool, tags) && !Boolean.TRUE.equals(tool.get("irreversible"))) names.add((String) tool.get("name"));
            for (String extra : list(role.getValue().get("extra"))) {
                Map<String, Object> tool = byName.get(extra);
                if (tool == null) throw new IllegalArgumentException("role " + role.getKey() + ": unknown tool " + extra);
                if (!(shares(tool, tags) || Boolean.TRUE.equals(tool.get("scoped")) || Boolean.TRUE.equals(tool.get("irreversible")))) {
                    throw new IllegalArgumentException("role " + role.getKey() + ": " + extra + " is outside the specialisation and is not a scoped cross-role tool");
                }
                if (!names.contains(extra)) names.add(extra);
            }
            if (names.size() > budget) throw new IllegalArgumentException("role " + role.getKey() + " has " + names.size() + " tools, more than the budget of " + budget);
            java.util.Collections.sort(names);
            result.put(role.getKey(), names);
        }
        return result;
    }

    static Map<String, Object> planTurn(String model, String need, List<String> tools) {
        return planTurn(model, need, tools, null, false);
    }

    static Map<String, Object> planTurn(String model, String need, List<String> tools, String forced) {
        return planTurn(model, need, tools, forced, false);
    }

    /** The tool_choice and tool list of one request, with the portable fallback where forcing is not accepted. */
    static Map<String, Object> planTurn(String model, String need, List<String> tools, String forced, boolean manualThinking) {
        if (!List.of("free", "none", "any", "named").contains(need)) throw new IllegalArgumentException("unknown need: " + need);
        if (need.equals("named") && (forced == null || !tools.contains(forced))) throw new IllegalArgumentException("a named choice needs a tool from the list");
        boolean rejects = NO_FORCING.contains(model) || manualThinking;
        if (need.equals("free")) return map("tool_choice", map("type", "auto"), "tools", new ArrayList<>(tools), "strict", false, "verify_call", false);
        if (need.equals("none")) return map("tool_choice", map("type", "none"), "tools", new ArrayList<>(tools), "strict", false, "verify_call", false);
        if (!rejects) {
            Map<String, Object> choice = need.equals("any") ? map("type", "any") : map("type", "tool", "name", forced);
            return map("tool_choice", choice, "tools", new ArrayList<>(tools), "strict", false, "verify_call", false);
        }
        return map("tool_choice", map("type", "auto"), "tools", need.equals("any") ? new ArrayList<>(tools) : new ArrayList<>(List.of(forced)), "strict", true, "verify_call", true);
    }

    /** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
    static String cacheImpact(Map<String, Object> previous, Map<String, Object> next) {
        if (previous == null) return "none";
        if (!java.util.Objects.equals(previous.get("tools"), next.get("tools"))) return "all";
        if (!java.util.Objects.equals(previous.get("tool_choice"), next.get("tool_choice"))) return "messages";
        return "none";
    }

    static String checkTurn(List<Map<String, Object>> blocks, String need) {
        return checkTurn(blocks, need, null);
    }

    /** Compare a reply with the call that was required: ok, missed_call or wrong_tool. */
    static String checkTurn(List<Map<String, Object>> blocks, String need, String forced) {
        if (!need.equals("any") && !need.equals("named")) return "ok";
        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map<String, Object> b : blocks) if ("tool_use".equals(b.get("type"))) calls.add(b);
        if (calls.isEmpty()) return "missed_call";
        if (need.equals("named") && !calls.get(0).get("name").equals(forced)) return "wrong_tool";
        return "ok";
    }

    private static Map<String, Object> answer(boolean allowed, String code, String message, boolean escalate) {
        return map("allowed", allowed, "code", code, "message", message, "escalate", escalate);
    }

    /** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> authorize(Map<String, Object> call, Map<String, Object> policy, Collection<String> approvals) {
        Map<String, Map<String, Object>> tools = (Map<String, Map<String, Object>>) policy.getOrDefault("tools", Map.of());
        Map<String, Object> rule = tools.get((String) call.get("tool"));
        if (rule == null) return answer(false, "unknown_tool", call.get("tool") + " is not an allowed tool", false);
        Integer cap = (Integer) rule.get("cap");
        Object amount = call.get("amount");
        if (cap != null && !(amount instanceof Integer n && n > 0)) return answer(false, "bad_amount", "the amount must be a positive whole number", false);
        Object verified = call.get("verified_customer");
        if (verified == null || !verified.equals(call.get("customer"))) return answer(false, "not_owner", "the call is not for the verified customer", false);
        if (cap != null && (Integer) amount > cap) return answer(false, "over_cap", amount + " is above the limit of " + cap + ": send this to a person", true);
        if (Boolean.TRUE.equals(rule.get("irreversible")) && !approvals.contains(call.get("id"))) return answer(false, "needs_approval", "this call cannot be undone: a person must approve it first", true);
        return answer(true, "ok", "allowed", false);
    }
}
