import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */
final class Distribute {
    private static final System.Logger LOG = System.getLogger(Distribute.class.getName());
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
            // TODO 1 of 8 (finish this to pass m1, e2): the tools of a role. Receives the catalog and the role's tags.
            //   Return the names, in catalog order, of the tools that share a tag with the role and are not irreversible
            //   (an irreversible tool is only given by an explicit grant). Example: tags {billing}, catalog
            //   [lookup(billing), refund(billing, irreversible)] -> [lookup].
            names.addAll(byName.keySet());
            for (String extra : list(role.getValue().get("extra"))) {
                Map<String, Object> tool = byName.get(extra);
                if (tool == null) throw new IllegalArgumentException("role " + role.getKey() + ": unknown tool " + extra);
                if (!(shares(tool, tags) || Boolean.TRUE.equals(tool.get("scoped")) || Boolean.TRUE.equals(tool.get("irreversible")))) {
                    throw new IllegalArgumentException("role " + role.getKey() + ": " + extra + " is outside the specialisation and is not a scoped cross-role tool");
                }
                if (!names.contains(extra)) names.add(extra);
            }
            // TODO 2 of 8 (finish this to pass e1): the budget. When the role would hold more tools than the budget,
            //   refuse with an error that names the role, the count and the budget. Example: 6 tools, budget 5 -> raises;
            //   exactly 5 is fine.
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
        // TODO 3 of 8 (finish this to pass e3): the choice for `any` and `named`. Receives the model, the need, the
        //   tools, the forced name and `rejects`. When the model accepts forcing, return tool_choice {type: any} or {type:
        //   tool, name}, the tools, strict false, verify_call false. When it rejects forcing, return tool_choice auto, the
        //   tools (all for any, only the named one for named), strict true and verify_call true. Example: sonnet, named,
        //   forced lookup -> auto, [lookup], strict, verify.
        return map("tool_choice", map("type", "auto"), "tools", new ArrayList<>(tools), "strict", false, "verify_call", false);
    }

    /** What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed). */
    static String cacheImpact(Map<String, Object> previous, Map<String, Object> next) {
        if (previous == null) return "none";
        // TODO 4 of 8 (finish this to pass e4): the cost in prompt caching. Receives the previous request and the new
        //   one (the first request has no previous). Return all when the tools differ, otherwise messages when the
        //   tool_choice differs, otherwise none. Example: same tools, auto then {type: any} -> messages.
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
        // TODO 5 of 8 (finish this to pass e5): the check of a named call. When the need is named and the first tool
        //   call is not the forced tool, return wrong_tool. Example: need named, forced lookup, first call refund ->
        //   wrong_tool.
        return "ok";
    }

    private static Map<String, Object> answer(boolean allowed, String code, String message, boolean escalate) {
        return map("allowed", allowed, "code", code, "message", message, "escalate", escalate);
    }

    /** Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> authorize(Map<String, Object> call, Map<String, Object> policy, Collection<String> approvals) {
        LOG.log(System.Logger.Level.DEBUG, "authorize input: {0}", call);
        Map<String, Map<String, Object>> tools = (Map<String, Map<String, Object>>) policy.getOrDefault("tools", Map.of());
        // TODO 6 of 8 (finish this to pass e6): the policy lookup. Find the rule of the called tool in the policy; when
        //   the policy does not name the tool, return the answer (allowed false, code unknown_tool, a message that names
        //   the tool). Example: tool delete_all, policy without it -> unknown_tool.
        Map<String, Object> rule = tools.getOrDefault((String) call.get("tool"), Map.of());
        Integer cap = (Integer) rule.get("cap");
        Object amount = call.get("amount");
        // TODO 7 of 8 (finish this to pass e6): the amount and the owner. When the tool has a cap and the amount is
        //   missing, a boolean, a decimal or not above zero, return bad_amount. When there is no verified customer or the
        //   call's customer is another, return not_owner. Example: amount 0 -> bad_amount; customer C2 while C1 is
        //   verified -> not_owner.
        // TODO 8 of 8 (finish this to pass e7): the cap and the approval. When the amount is above the cap, return
        //   over_cap with escalate true (an amount equal to the cap is allowed). When the tool is irreversible and the
        //   call's id is not among the approvals, return needs_approval with escalate true. Example: cap 500, amount 501
        //   -> over_cap, even when approved.
        return answer(true, "ok", "allowed", false);
    }
}
