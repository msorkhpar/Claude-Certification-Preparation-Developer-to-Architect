import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md. Requests and results are JSON-like maps. */
final class Mrtr {
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

    static Map<String, Object> listTools(Map<String, Object> request) {
        // TODO: the tools sorted by name, as a complete result with ttlMs and cacheScope; a request of another protocol version is a protocol error.
        return null;
    }

    static Map<String, Object> callTool(Map<String, Object> request, String secret, String principal, long now) {
        // TODO: run the tool; when the server needs the client's input, return an input_required result with a signed requestState. No state is kept here.
        return null;
    }
}
