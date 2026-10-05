import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An injection-resistant tool gate. See ../../statement.md. */
final class Gate {
    private static final System.Logger LOG = System.getLogger(Gate.class.getName());
    private static final Set<String> TOOLS = Set.of("read_file", "write_file", "bash", "fetch", "send_email");
    private static final Map<String, Pattern> SIGNALS = new LinkedHashMap<>();
    private static final String[] SHELL_TRICKS = {";", "&", "|", ">", "<", "`", "$(", "\n"};

    static {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
        SIGNALS.put("override", Pattern.compile("\\b(ignore|disregard|forget)\\b.{0,40}\\b(previous|prior|above|earlier|system)\\b.{0,20}\\b(instructions?|prompts?|rules)\\b", flags));
        SIGNALS.put("role-tag", Pattern.compile("<\\s*/?\\s*(system|assistant|tool_result|instructions?)\\s*>", flags));
        SIGNALS.put("exfiltrate", Pattern.compile("\\b(send|email|forward|post|upload)\\b.{0,60}\\b(to|at)\\b.{0,40}[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+", flags));
        SIGNALS.put("reveal", Pattern.compile("\\b(reveal|print|show|repeat)\\b.{0,40}\\b(system prompt|password|secret|api key)\\b", flags));
    }

    /** The names of the injection signals found in the text, in the fixed order override, role-tag, exfiltrate, reveal. */
    static List<String> screen(String text) {
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, Pattern> e : SIGNALS.entrySet()) if (e.getValue().matcher(text).find()) found.add(e.getKey());
        return found;
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
    static Map<String, Object> wrapUntrusted(String toolUseId, String source, String content) {
        LOG.log(System.Logger.Level.DEBUG, "wrapUntrusted input: {0}", content);
        // TODO 1 of 6 (unlocks m1 and e1): wrap untrusted text as a tool_result block.
        // Receives the tool use id, where the text came from and the text. Returns a tool_result map. Clean text becomes one line of
        // compact JSON (Json.stringify) with the keys source, trust ("untrusted") and content, in that order. When `screen` finds a
        // signal, return the error block from the statement instead and leave the text out of it.
        // Example: wrapUntrusted("t1", "web page", "hi")
        //   -> {type=tool_result, tool_use_id=t1, content={"source":"web page","trust":"untrusted","content":"hi"}}
        return map("type", "tool_result", "tool_use_id", toolUseId, "content", content);
    }

    private static boolean luhn(String digits) {
        int total = 0;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(digits.length() - 1 - i) - '0';
            if (i % 2 == 1) d = d * 2 > 9 ? d * 2 - 9 : d * 2;
            total += d;
        }
        return total % 10 == 0;
    }

    /** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
    static String redact(String input) {
        String text = input.replaceAll("sk-ant-[A-Za-z0-9_-]{8,}", "[SECRET]");
        text = text.replaceAll("\\bAKIA[0-9A-Z]{16}\\b", "[SECRET]");
        text = text.replaceAll("Bearer [A-Za-z0-9._-]{16,}", "Bearer [SECRET]");
        text = text.replaceAll("[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+", "[EMAIL]");
        Matcher m = Pattern.compile("\\b(?:[0-9][ -]?){12,18}[0-9]\\b").matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            m.appendReplacement(out, Matcher.quoteReplacement(isCard(digits) ? "[CARD]" : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String resolve(String root, String path) {
        String p = path.replace('\\', '/');
        List<String> stack = new ArrayList<>();
        for (String part : (p.startsWith("/") ? p : root + "/" + p).split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            } else stack.add(part);
        }
        return "/" + String.join("/", stack);
    }

    private static boolean isSecret(String path) {
        // TODO 2 of 6 (unlocks e2): does this path name a secret file?
        // Receives a path and returns true for a file named `.env`, a file starting `.env.` unless it is `.env.example`, any file inside
        // a folder called `secrets`, and a name ending in `.pem` or `.key`; false otherwise. Backslashes count as `/`.
        // Example: isSecret("app/.env.local") -> true, isSecret(".env.example") -> false
        return false;
    }

    private static boolean commandAllowed(List<String> words) {
        // TODO 3 of 6 (finish this to pass e3): is the first word of a bash command on the allow-list?
        // Receives the command split into words. Returns true for `ls`, `cat`, `pytest`, and `git` followed by `status`, `diff` or
        // `log`; false for anything else, including an empty list. (The checks for sudo, rm, chaining and secret arguments are written.)
        // Example: commandAllowed(List.of("git", "log", "--oneline")) -> true, commandAllowed(List.of("git", "push")) -> false
        return false;
    }

    private static boolean isCard(String digits) {
        // TODO 4 of 6 (finish this to pass e6): is this run of digits a card number?
        // Receives the digits with spaces and hyphens already removed. Returns true when there are 13 to 19 of them and `luhn` accepts
        // them (a long number that fails the check is not a card). Example: isCard("4111111111111111") -> true, isCard("4111111111111112") -> false
        return false;
    }

    private static Map<String, Object> result(String decision, String reason) {
        return map("decision", decision, "reason", reason);
    }

    private static Map<String, Object> allow() {
        return result("allow", "ok");
    }

    private final String root;
    private final List<String> allowedHosts = new ArrayList<>();
    private final List<String> allowedEmailDomains = new ArrayList<>();
    private boolean tainted = false;
    private final List<Map<String, Object>> records = new ArrayList<>();

    Gate(String root, List<String> allowedHosts, List<String> allowedEmailDomains) {
        this.root = resolve("/", root);
        for (String h : allowedHosts) this.allowedHosts.add(h.toLowerCase(Locale.ROOT));
        for (String d : allowedEmailDomains) this.allowedEmailDomains.add(d.toLowerCase(Locale.ROOT));
    }

    boolean isTainted() {
        return tainted;
    }

    List<Map<String, Object>> audit() {
        return new ArrayList<>(records);
    }

    void markUntrusted(String source) {
        tainted = true;
    }

    private boolean inside(String path) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    Map<String, Object> decide(String actor, String tool, Map<String, Object> args) {
        Map<String, Object> r = choose(tool, args);
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : args.entrySet()) clean.put(e.getKey(), e.getValue() instanceof String s ? redact(s) : e.getValue());
        records.add(map("actor", actor, "tool", tool, "decision", r.get("decision"), "reason", r.get("reason"), "args", clean));
        return r;
    }

    private Map<String, Object> choose(String tool, Map<String, Object> args) {
        if (!TOOLS.contains(tool)) return result("deny", "unknown tool");
        if (tool.equals("read_file") || tool.equals("write_file")) {
            String path = resolve(root, String.valueOf(args.getOrDefault("path", "")));
            if (!inside(path)) return result("deny", "outside the project");
            if (isSecret(path)) return result("deny", "secret file");
            if (tool.equals("write_file")) {
                List<String> segments = Arrays.asList(path.split("/"));
                if (segments.contains(".git") || segments.contains(".claude")) return result("deny", "protected path");
                if (tainted) return result("ask", "untrusted content in this session");
            }
            return allow();
        }
        if (tool.equals("bash")) return bash(String.valueOf(args.getOrDefault("command", "")));
        if (tool.equals("fetch")) return fetch(String.valueOf(args.getOrDefault("url", "")));
        return email(args);
    }

    private Map<String, Object> bash(String command) {
        String trimmed = command.strip();
        List<String> words = trimmed.isEmpty() ? new ArrayList<>() : Arrays.asList(trimmed.split("\\s+"));
        for (String w : words) {
            String base = w.substring(w.lastIndexOf('/') + 1);
            if (base.equals("sudo") || base.equals("rm")) return result("deny", "dangerous command");
        }
        for (String t : SHELL_TRICKS) if (command.contains(t)) return result("deny", "chaining or redirection");
        if (!commandAllowed(words)) return result("deny", "command not allowed");
        for (String w : words.subList(1, words.size())) if (!w.startsWith("-") && isSecret(w)) return result("deny", "secret file");
        if (words.get(0).equals("pytest") && tainted) return result("ask", "untrusted content in this session");
        return allow();
    }

    private static final Pattern URL = Pattern.compile("([A-Za-z][A-Za-z0-9+.-]*)://([^/?#]*)([^?#]*)(\\?[^#]*)?(#.*)?", Pattern.DOTALL);

    private Map<String, Object> fetch(String url) {
        Matcher m = URL.matcher(url);
        if (!m.matches() || !m.group(1).toLowerCase(Locale.ROOT).equals("https")) return result("deny", "https only");
        if (m.group(2).contains("@")) return result("deny", "credentials in the URL");
        String host = m.group(2).toLowerCase(Locale.ROOT).replaceAll(":[0-9]+$", "");
        boolean listed = false;
        for (String h : allowedHosts) if (host.equals(h) || host.endsWith("." + h)) listed = true;
        if (!listed) return result("deny", "host not allowed");
        if (tainted && (m.group(4) != null || m.group(5) != null)) return result("ask", "data could leave in the URL");
        return allow();
    }

    private Map<String, Object> email(Map<String, Object> args) {
        String to = String.valueOf(args.getOrDefault("to", ""));
        String domain = to.contains("@") ? to.substring(to.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT) : "";
        String text = args.getOrDefault("subject", "") + "\n" + args.getOrDefault("body", "");
        return emailDecision(domain, text);
    }

    private Map<String, Object> emailDecision(String domain, String text) {
        // TODO 5 of 6 (finish this to pass e4 and e5): decide a send_email call.
        // Receives the recipient's lower-case domain and the subject and body joined by a newline. Returns result(decision, reason):
        // deny "recipient not allowed" when the domain is not in allowedEmailDomains; deny "sensitive data in the body" when redact(text)
        // differs from the text; deny "a person must send it" when the session is tainted (the `tainted` field); else allow().
        // Example: a clean mail to "example.com" in a tainted session -> {decision=deny, reason=a person must send it}
        return allow();
    }

    List<Map<String, Object>> alerts() {
        // TODO 6 of 6 (finish this to pass e7): actors with three or more denials.
        // Reads `records` (maps with "actor" and "decision"). Returns one map("actor", ..., "denials", ...) per actor with three or more
        // "deny" records (an "ask" is not a denial), with that actor's current count, ordered by when each reached three.
        // Example: alice denied 3 times, then bob 3 times, then alice once more -> [{actor=alice, denials=4}, {actor=bob, denials=3}]
        return new ArrayList<>();
    }

    /** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
    static Map<String, Object> hookResponse(Map<String, Object> decision) {
        if (decision.get("decision").equals("allow")) return map("exit_code", 0, "stdout", "", "stderr", "");
        Map<String, Object> out = map("hookSpecificOutput", map("hookEventName", "PreToolUse", "permissionDecision", decision.get("decision"), "permissionDecisionReason", decision.get("reason")));
        return map("exit_code", 0, "stdout", Json.stringify(out), "stderr", "");
    }
}
