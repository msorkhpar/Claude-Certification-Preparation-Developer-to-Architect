import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md. Requests and results are JSON-like maps. */
final class Mrtr {
    private static final System.Logger LOG = System.getLogger(Mrtr.class.getName());
    private Mrtr() {}

    static final String VERSION = "2026-07-28";
    static final String META_VERSION = "io.modelcontextprotocol/protocolVersion";
    static final String META_CAPS = "io.modelcontextprotocol/clientCapabilities";
    static final int TTL_SECONDS = 300;

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static final List<Map<String, Object>> TOOLS = List.of(
            map("name", "deploy", "description", "Deploy a service to an environment. Production needs a person's confirmation.",
                    "inputSchema", map("type", "object", "properties", map("service", map("type", "string"), "env", map("type", "string", "enum", List.of("staging", "production"))), "required", List.of("service", "env"))),
            map("name", "status", "description", "Report whether a service is running.",
                    "inputSchema", map("type", "object", "properties", map("service", map("type", "string")), "required", List.of("service"))));

    /** JSON with the keys of every object in sorted order. */
    private static String canonical(Object v) {
        if (v instanceof Map<?, ?> m) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : new TreeMap<String, Object>((Map<String, Object>) m).entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(Json.stringify(e.getKey())).append(':').append(canonical(e.getValue()));
            }
            return out.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) out.append(i > 0 ? "," : "").append(canonical(l.get(i)));
            return out.append(']').toString();
        }
        return Json.stringify(v);
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] hmac(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Given: a requestState for payload: its JSON, then a HMAC-SHA256 signature of it, both in base64url, joined by a dot. */
    static String mintState(String secret, Map<String, Object> payload) {
        String body = b64(canonical(payload).getBytes(StandardCharsets.UTF_8));
        return body + "." + b64(hmac(secret, body));
    }

    /** Given: the payload of a requestState, or null when it is not one this secret signed (tampered, truncated, foreign, garbage). */
    @SuppressWarnings("unchecked")
    static Map<String, Object> readState(String secret, String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) return null;
            if (!MessageDigest.isEqual(b64(hmac(secret, parts[0])).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) return null;
            Object payload = Json.parse(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
            return payload instanceof Map<?, ?> ? (Map<String, Object>) payload : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Given: a fingerprint of the call's arguments, the same for the same arguments in any key order. */
    static String argsDigest(Map<String, Object> arguments) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(arguments).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> error(int code, String message, Object data) {
        Map<String, Object> err = map("code", code, "message", message);
        if (data != null) err.put("data", data);
        return map("error", err);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> meta(Map<String, Object> request) {
        return request.get("_meta") instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
    }

    private static Map<String, Object> metaError(Map<String, Object> request) {
        if (!VERSION.equals(meta(request).get(META_VERSION))) return error(-32022, "Unsupported protocol version", map("supported", List.of(VERSION)));
        return null;
    }

    /**
     * TODO 1 of 8 (unlocks e1): the result of tools/list.
     * Receives nothing and returns the map from the statement: resultType "complete", the TOOLS sorted by name, ttlMs 300000, cacheScope "public".
     * Example: toolListing().get("tools") starts with the tool named "deploy".
     */
    private static Map<String, Object> toolListing() {
        return map();
    }

    static Map<String, Object> listTools(Map<String, Object> request) {
        Map<String, Object> bad = metaError(request);
        if (bad != null) return bad;
        return toolListing();
    }

    private static Map<String, Object> complete(String text, boolean isError) {
        return map("resultType", "complete", "content", List.of(map("type", "text", "text", text)), "isError", isError);
    }

    private static Map<String, Object> confirmRequest(String service) {
        return map("confirm", map("method", "elicitation/create", "params", map("mode", "form", "message", "Deploy " + service + " to production?", "requestedSchema",
                map("type", "object", "properties", map("confirm", map("type", "boolean", "title", "Confirm the deployment")), "required", List.of("confirm")))));
    }

    private static Map<String, Object> notesRequest(String service) {
        return map("notes", map("method", "sampling/createMessage", "params", map("messages",
                List.of(map("role", "user", "content", map("type", "text", "text", "Write one sentence of release notes for " + service + "."))), "maxTokens", 100)));
    }

    /**
     * TODO 2 of 8 (unlocks e7): the payload of a requestState.
     * Receives the tool name, the arguments, the user, the time in seconds and the step. Returns the map from the statement: v, tool, digest, sub, exp, step.
     * Example: newState("deploy", Map.of("service", "api"), "alice", 1000, "confirm").get("exp") -> 1300
     */
    private static Map<String, Object> newState(Object name, Map<String, Object> arguments, String principal, long now, String step) {
        return map();
    }

    /**
     * TODO 3 of 8 (unlocks e6): the protocol error for a call that cannot be served, in the order of the statement.
     * Receives the tool name and the arguments. Returns error(-32602, ..., null) for an unknown tool, a missing or blank service, or (for deploy) an env
     * that is not staging or production; null when the call is valid.
     * Example: validate("deploy", Map.of("service", "api", "env", "dev")) -> error(-32602, "Invalid params: env must be staging or production", null)
     */
    private static Map<String, Object> validate(Object name, Map<String, Object> arguments) {
        return null;
    }

    /**
     * TODO 4 of 8 (unlocks e2): can the client be asked for a confirmation?
     * Receives the client capabilities map. Returns true when "elicitation" is present and is empty or holds "form"; false for none or only "url".
     * Example: canElicit(Map.of("elicitation", Map.of())) -> true, canElicit(Map.of("elicitation", Map.of("url", Map.of()))) -> false
     */
    private static boolean canElicit(Map<String, Object> caps) {
        return false;
    }

    /**
     * TODO 5 of 8 (unlocks e4 and e5): the error for a requestState that cannot be used.
     * Receives the secret, the token, the user, the tool name, the arguments and the time. Returns error(-32602, ..., null) with "Invalid requestState"
     * (readState gives null), "Expired requestState" (now after exp) or "requestState does not match this request" (sub, tool or digest differ), checked in
     * that order; null when the state is good.
     * Example: a state minted for "alice" and read for "bob" -> error(-32602, "requestState does not match this request", null)
     */
    private static Map<String, Object> stateError(String secret, String token, String principal, Object name, Map<String, Object> arguments, long now) {
        return null;
    }

    /**
     * TODO 6 of 8 (unlocks m1 and e3): is this an answer to the confirmation question?
     * Receives inputResponses.confirm (anything, or null). Returns true when it is a map whose "action" is accept, decline or cancel.
     * Example: confirmUsable(Map.of("action", "decline")) -> true, confirmUsable("yes") -> false
     */
    private static boolean confirmUsable(Object answer) {
        return false;
    }

    /**
     * TODO 7 of 8 (unlocks e3): did the person accept?
     * Receives a usable answer. Returns true only for action "accept" with content.confirm exactly true.
     * Example: confirmed(Map.of("action", "accept", "content", Map.of("confirm", false))) -> false
     */
    private static boolean confirmed(Map<?, ?> answer) {
        return false;
    }

    /**
     * TODO 8 of 8 (unlocks m1): the release notes the client wrote.
     * Receives the inputResponses map. Returns answers.notes.content.text when that is a string; null for anything else.
     * Example: notesText(Map.of("notes", Map.of("content", Map.of("type", "text", "text", "Faster.")))) -> "Faster."
     */
    private static String notesText(Map<String, Object> answers) {
        return null;
    }

    private static Map<String, Object> ask(Map<String, Object> requests, String step, String secret, String name, Map<String, Object> arguments, String principal, long now) {
        Map<String, Object> state = newState(name, arguments, principal, now, step);
        return map("resultType", "input_required", "inputRequests", requests, "requestState", mintState(secret, state));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> callTool(Map<String, Object> request, String secret, String principal, long now) {
        LOG.log(System.Logger.Level.DEBUG, "callTool input: {0}", request);
        Map<String, Object> bad = metaError(request);
        if (bad != null) return bad;
        Object name = request.get("name");
        Map<String, Object> arguments = request.get("arguments") instanceof Map<?, ?> a ? (Map<String, Object>) a : new LinkedHashMap<>();
        Map<String, Object> invalid = validate(name, arguments);
        if (invalid != null) return invalid;
        String s = (String) arguments.get("service");
        if (name.equals("status")) return complete(s + ": running", false);
        if (arguments.get("env").equals("staging")) return complete("Deployed " + s + " to staging", false);
        Object capsObject = meta(request).get(META_CAPS);
        Map<String, Object> caps = capsObject instanceof Map<?, ?> c ? (Map<String, Object>) c : new LinkedHashMap<>();
        if (!canElicit(caps)) return complete("Deploying to production needs confirmation, and this client cannot be asked.", true);
        String step = "confirm";
        Object token = request.get("requestState");
        if (token != null) {
            Map<String, Object> problem = stateError(secret, String.valueOf(token), principal, name, arguments, now);
            if (problem != null) return problem;
            Map<String, Object> state = readState(secret, String.valueOf(token));
            step = state == null ? "" : String.valueOf(state.get("step"));
        }
        Map<String, Object> answers = token != null && request.get("inputResponses") instanceof Map<?, ?> r ? (Map<String, Object>) r : new LinkedHashMap<>();
        if (step.equals("confirm")) {
            Object answer = answers.get("confirm");
            if (!confirmUsable(answer)) return ask(confirmRequest(s), "confirm", secret, "deploy", arguments, principal, now);
            if (!confirmed((Map<?, ?>) answer)) return complete("Deployment cancelled", false);
            if (!caps.containsKey("sampling")) return complete("Deployed " + s + " to production", false);
            return ask(notesRequest(s), "notes", secret, "deploy", arguments, principal, now);
        }
        String text = notesText(answers);
        if (text == null) return ask(notesRequest(s), "notes", secret, "deploy", arguments, principal, now);
        return complete("Deployed " + s + " to production. Release notes: " + text, false);
    }
}
