import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An injection-resistant tool gate. See ../../statement.md. */
final class Gate {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** The names of the injection signals found in the text, in the fixed order override, role-tag, exfiltrate, reveal. */
    static List<String> screen(String text) {
        return new ArrayList<>();
    }

    /** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
    static Map<String, Object> wrapUntrusted(String toolUseId, String source, String content) {
        return map("type", "tool_result", "tool_use_id", toolUseId, "content", content);
    }

    /** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
    static String redact(String text) {
        return text;
    }

    private final String root;
    private final List<String> allowedHosts;
    private final List<String> allowedEmailDomains;

    Gate(String root, List<String> allowedHosts, List<String> allowedEmailDomains) {
        this.root = root;
        this.allowedHosts = allowedHosts;
        this.allowedEmailDomains = allowedEmailDomains;
    }

    boolean isTainted() {
        return false;
    }

    List<Map<String, Object>> audit() {
        return new ArrayList<>();
    }

    void markUntrusted(String source) {}

    Map<String, Object> decide(String actor, String tool, Map<String, Object> args) {
        return map("decision", "allow", "reason", "ok");
    }

    List<Map<String, Object>> alerts() {
        return new ArrayList<>();
    }

    /** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
    static Map<String, Object> hookResponse(Map<String, Object> decision) {
        return map("exit_code", 0, "stdout", "", "stderr", "");
    }
}
