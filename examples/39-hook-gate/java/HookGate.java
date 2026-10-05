import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A PreToolUse hook that blocks destructive commands, and a linter for skill and subagent files.
 *
 * <p>Claude Code starts a hook as a process, writes one JSON event to its standard input and reads the answer from its exit code, its
 * standard output and its standard error. Exit code 2 blocks the call and the standard error is the reason; exit code 0 with a JSON
 * `permissionDecision` answers in a structured way; exit code 0 with nothing printed gives no opinion. This file is such a hook (run it
 * with --hook) and a demonstration (run it plain). The event and output shapes are those of the Claude Code hooks reference, read on
 * 2026-10-03. The command check normalises what a prefix rule such as `Bash(git push *)` would miss: another form of the same command.
 * The front matter of the skill and subagent files is YAML, read with Jackson.
 */
public final class HookGate {
    private static final System.Logger LOG = System.getLogger(HookGate.class.getName());
    static final List<String> PROTECTED = List.of(".env", "package-lock.json", ".git/");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    /** What the hook process answers: the exit code, standard output and standard error. */
    record Answer(int code, String out, String err) {}

    /** The words of a command line as a POSIX shell would split them (quotes and backslashes); a bad quote is an error. */
    static List<String> shlexSplit(String s) {
        List<String> words = new ArrayList<>();
        StringBuilder word = null;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (Character.isWhitespace(c)) {
                if (word != null) { words.add(word.toString()); word = null; }
                continue;
            }
            if (word == null) word = new StringBuilder();
            if (c == '\\') {
                if (i >= s.length()) throw new IllegalArgumentException("No escaped character");
                word.append(s.charAt(i++));
            } else if (c == '\'') {
                int end = s.indexOf('\'', i);
                if (end < 0) throw new IllegalArgumentException("No closing quotation");
                word.append(s, i, end);
                i = end + 1;
            } else if (c == '"') {
                while (true) {
                    if (i >= s.length()) throw new IllegalArgumentException("No closing quotation");
                    char d = s.charAt(i++);
                    if (d == '"') break;
                    if (d == '\\' && i < s.length() && "\"\\$`\n".indexOf(s.charAt(i)) >= 0) d = s.charAt(i++);
                    word.append(d);
                }
            } else {
                word.append(c);
            }
        }
        if (word != null) words.add(word.toString());
        return words;
    }

    private static String gitSubcommand(List<String> args) {
        int i = 0;
        while (i < args.size()) {
            String a = args.get(i);
            if (List.of("-C", "-c", "--git-dir", "--work-tree").contains(a)) i += 2;
            else if (a.startsWith("-")) i += 1;
            else return a;
        }
        return null;
    }

    /** The reason a command is refused, or null. Looks through compound commands, `sh -c`, env assignments, paths and git options. */
    static String dangerous(String command) {
        for (String part : command.split("&&|\\|\\||;|\\||&|\n")) {
            List<String> words;
            try {
                words = shlexSplit(part);
            } catch (IllegalArgumentException e) {
                return "a command that cannot be parsed is not run unreviewed";
            }
            int skip = 0;
            while (skip < words.size() && words.get(skip).matches("\\w+=\\S*")) skip++;
            words = words.subList(skip, words.size());
            if (words.isEmpty()) continue;
            String program = words.get(0).substring(words.get(0).lastIndexOf('/') + 1);
            List<String> args = words.subList(1, words.size());
            int dashC = args.indexOf("-c");
            if (List.of("sh", "bash", "zsh").contains(program) && dashC >= 0 && dashC + 1 < args.size()) {
                String inner = dangerous(args.get(dashC + 1));
                if (inner != null) return inner;
            }
            if (program.equals("git") && "push".equals(gitSubcommand(args))) return "nothing is pushed from an agent";
            String shortFlags = args.stream().filter(a -> a.startsWith("-") && !a.startsWith("--")).map(a -> a.substring(1)).collect(Collectors.joining());
            boolean recursive = shortFlags.contains("r") || shortFlags.contains("R") || args.contains("--recursive");
            boolean force = shortFlags.contains("f") || args.contains("--force");
            if (program.equals("rm") && recursive && force) return "a recursive forced delete is not run from an agent";
        }
        return null;
    }

    /** The answer for one PreToolUse event. */
    @SuppressWarnings("unchecked")
    static Answer preToolUse(Map<String, Object> event) throws IOException {
        String tool = (String) event.get("tool_name");
        Map<String, Object> toolInput = (Map<String, Object>) event.getOrDefault("tool_input", Map.of());
        if (tool.equals("Bash")) {
            String reason = dangerous((String) toolInput.getOrDefault("command", ""));
            if (reason != null) {
                Map<String, Object> specific = new LinkedHashMap<>();
                specific.put("hookEventName", "PreToolUse");
                specific.put("permissionDecision", "deny");
                specific.put("permissionDecisionReason", reason);
                return new Answer(0, JSON.writeValueAsString(Map.of("hookSpecificOutput", specific)), "");
            }
        }
        if (List.of("Edit", "Write", "MultiEdit").contains(tool)) {
            String path = ((String) toolInput.getOrDefault("file_path", "")).replace("\\", "/");
            for (String pattern : PROTECTED) {
                if (path.contains(pattern)) return new Answer(2, "", "Blocked: " + path + " matches protected pattern '" + pattern + "'");
            }
        }
        return new Answer(0, "", "");
    }

    // --- linting the files that configure Claude Code -------------------------------------------------------------------

    private static final Pattern SIDE_EFFECTS = Pattern.compile("\\b(deploy|publish|delete|commit|push|merge)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRONTMATTER = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL);

    /** The front matter of a Markdown file that starts with a --- block; an empty map when it has none. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> frontmatter(String text) throws IOException {
        Matcher m = FRONTMATTER.matcher(text);
        if (!m.matches()) return Map.of();
        Map<String, Object> fm = YAML.readValue(m.group(1), LinkedHashMap.class);
        return fm == null ? Map.of() : fm;
    }

    private static String text(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static List<String> lintSkill(String text) throws IOException {
        Map<String, Object> fm = frontmatter(text);
        List<String> findings = new ArrayList<>();
        String desc = text(fm.get("description"));
        if (desc.isEmpty()) findings.add("description is missing: Claude uses it to decide when to load the skill");
        if (desc.length() + text(fm.get("when_to_use")).length() > 1536) findings.add("description and when_to_use together exceed 1,536 characters and are cut");
        if (SIDE_EFFECTS.matcher(text(fm.getOrDefault("name", "")) + " " + desc).find() && !Boolean.TRUE.equals(fm.get("disable-model-invocation"))) {
            findings.add("a skill with side effects should set disable-model-invocation: true");
        }
        Object allowed = fm.get("allowed-tools");
        if (allowed != null && !text(allowed).isEmpty() && List.of(text(allowed).replace(",", " ").trim().split("\\s+")).contains("Bash")) {
            findings.add("allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead");
        }
        return findings;
    }

    static List<String> lintAgent(String text) throws IOException {
        Map<String, Object> fm = frontmatter(text);
        List<String> findings = new ArrayList<>();
        for (String k : List.of("name", "description")) if (text(fm.get(k)).isEmpty()) findings.add(k + " is required");
        if (!fm.containsKey("tools")) findings.add("tools is omitted: the subagent inherits every tool");
        if (fm.get("memory") != null && !List.of("user", "project", "local").contains(fm.get("memory"))) findings.add("memory must be user, project or local");
        if ("bypassPermissions".equals(fm.get("permissionMode"))) findings.add("permissionMode bypassPermissions skips every prompt in this subagent");
        return findings;
    }

    static final String SKILL = "---\nname: deploy\ndescription: Deploy the service to production\nallowed-tools: Bash\n---\nRun the release script.\n";
    static final String AGENT = "---\nname: reviewer\ndescription: Reviews a diff for bugs\nmemory: team\n---\nYou review code.\n";

    /** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v instanceof List<?> l) return l.stream().map(HookGate::py).collect(Collectors.joining(", ", "[", "]"));
        String s = String.valueOf(v);
        String q = s.contains("'") && !s.contains("\"") ? "\"" : "'";
        return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q;
    }

    @SuppressWarnings("unchecked")
    static void demo() throws IOException {
        List<Object[]> events = List.of(
            new Object[] {"Bash", "command", "git push origin main"}, new Object[] {"Bash", "command", "git -C . push origin main"},
            new Object[] {"Bash", "command", "bash -c 'rm -rf build'"}, new Object[] {"Bash", "command", "ls && git status"},
            new Object[] {"Bash", "command", "rm build/old.txt"}, new Object[] {"Edit", "file_path", "/work/app/.env"},
            new Object[] {"Edit", "file_path", "/work/app/main.py"});
        for (Object[] e : events) {
            Map<String, Object> event = Map.of("hook_event_name", "PreToolUse", "tool_name", e[0], "tool_input", Map.of((String) e[1], e[2]));
            Answer a = preToolUse(event);
            String shown;
            if (!a.out().isEmpty()) {
                Map<String, Object> specific = (Map<String, Object>) JSON.readValue(a.out(), Map.class).get("hookSpecificOutput");
                shown = "deny (exit 0, JSON): " + specific.get("permissionDecisionReason");
            } else {
                shown = a.code() == 2 ? "block (exit 2): " + a.err() : "no decision (exit 0)";
            }
            System.out.println(e[0] + " " + py(e[2]) + " -> " + shown);
        }
        System.out.println("skill findings: " + py(lintSkill(SKILL)));
        System.out.println("agent findings: " + py(lintAgent(AGENT)));
    }

    public static void main(String[] args) throws Exception {
        if (List.of(args).contains("--hook")) {
            Answer a = preToolUse(JSON.readValue(System.in, LinkedHashMap.class));
            System.out.print(a.out());
            System.err.print(a.err());
            System.out.flush();
            System.err.flush();
            System.exit(a.code());
        }
        demo();
    }
}
