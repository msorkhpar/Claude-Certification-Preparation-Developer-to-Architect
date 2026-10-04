import static harness.Show.py;

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
 * What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.
 *
 * <p>The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
 * `/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
 * indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work:
 * the front matter is YAML, read with Jackson.
 */
public final class SkillModel {
    static final List<String> LEVELS = List.of("enterprise", "personal", "project"); // the order in which a skill name is resolved: the first level wins
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern FRONTMATTER = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL);

    /** The front matter of a command or skill file, and its body. */
    record Parsed(Map<String, Object> meta, String body) {}

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

    /** (front matter mapping, body) of a command or skill file. */
    @SuppressWarnings("unchecked")
    static Parsed parse(String text) throws IOException {
        Matcher m = FRONTMATTER.matcher(text);
        if (!m.matches()) return new Parsed(Map.of(), text);
        Map<String, Object> meta = YAML.readValue(m.group(1), LinkedHashMap.class);
        return new Parsed(meta == null ? Map.of() : meta, m.group(2));
    }

    /** The slash command a file creates: a skill's `name`, else its folder; a command's file name. */
    static String commandName(String path, Map<String, Object> meta) {
        String[] parts = path.split("/");
        if (parts[parts.length - 1].equals("SKILL.md")) {
            Object name = meta.get("name");
            return name == null || String.valueOf(name).isEmpty() ? parts[parts.length - 2] : String.valueOf(name);
        }
        String file = parts[parts.length - 1];
        return file.substring(0, file.length() - 3);
    }

    /** Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}. */
    static String winner(Map<String, String> candidates) {
        for (String level : LEVELS) if (candidates.containsKey(level)) return candidates.get(level);
        return null;
    }

    /** Who can start it, and whether its description is always in context. */
    static Map<String, Boolean> invocation(Map<String, Object> meta) {
        boolean manual = Boolean.TRUE.equals(meta.get("disable-model-invocation"));
        boolean hidden = Boolean.FALSE.equals(meta.get("user-invocable"));
        Map<String, Boolean> out = new LinkedHashMap<>();
        out.put("you", !hidden);
        out.put("claude", !manual);
        out.put("description_in_context", !manual);
        return out;
    }

    private static List<String> names(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof String s) {
            Matcher m = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(s);
            while (m.find()) out.add(m.group());
        } else if (value instanceof List<?> l) {
            for (Object o : l) out.add(String.valueOf(o));
        }
        return out;
    }

    /** True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command. */
    static boolean preApproved(Map<String, Object> meta, String tool, String command) {
        for (String item : names(meta.get("allowed-tools"))) {
            int open = item.indexOf('(');
            String name = open < 0 ? item : item.substring(0, open);
            if (!name.equals(tool)) continue;
            String pattern = open < 0 ? "" : item.substring(open + 1);
            while (pattern.endsWith(")")) pattern = pattern.substring(0, pattern.length() - 1);
            if (pattern.isEmpty() || (pattern.endsWith(" *") && (command.equals(pattern.substring(0, pattern.length() - 2)) || command.startsWith(pattern.substring(0, pattern.length() - 1)))) || pattern.equals(command)) return true;
        }
        return false;
    }

    static boolean preApproved(Map<String, Object> meta, String tool) {
        return preApproved(meta, tool, "");
    }

    /** True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/**)` leaves the tool in place. */
    static boolean removed(Map<String, Object> meta, String tool) {
        return names(meta.get("disallowed-tools")).stream().filter(n -> !n.contains("(")).anyMatch(n -> n.equals(tool));
    }

    static String toolStatus(Map<String, Object> meta, String tool, String command) {
        if (removed(meta, tool)) return "removed";
        return preApproved(meta, tool, command) ? "pre-approved" : "permission settings decide";
    }

    static String toolStatus(Map<String, Object> meta, String tool) {
        return toolStatus(meta, tool, "");
    }

    /** Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input. */
    static String render(String body, String raw, List<String> names) {
        List<String> args = raw.isEmpty() ? List.of() : shlexSplit(raw);
        Map<String, String> named = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(names.size(), args.size()); i++) named.put(names.get(i), args.get(i));
        String alternatives = "ARGUMENTS\\[\\d+\\]|ARGUMENTS|\\d+" + names.stream().map(n -> "|" + Pattern.quote(n)).collect(Collectors.joining());
        Matcher m = Pattern.compile("\\$(" + alternatives + ")").matcher(body);
        Pattern indexed = Pattern.compile("ARGUMENTS\\[(\\d+)\\]|(\\d+)");
        boolean used = false;
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            used = true;
            String token = m.group(1);
            String value;
            if (token.equals("ARGUMENTS")) value = raw;
            else {
                Matcher index = indexed.matcher(token);
                if (index.matches()) {
                    int i = Integer.parseInt(index.group(1) != null ? index.group(1) : index.group(2));
                    value = i < args.size() ? args.get(i) : m.group(0); // an indexed placeholder with no argument stays as written
                } else value = named.getOrDefault(token, ""); // a named placeholder with no argument is empty
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        String result = out.toString();
        if (!raw.isEmpty() && !used) result = result.replaceFirst("\\n*$", "") + "\nARGUMENTS: " + raw + "\n";
        return result;
    }

    static String render(String body, String raw) {
        return render(body, raw, List.of());
    }

    /** The subagent a forked skill runs in, or null when it runs in the conversation. The subagent does not see the conversation. */
    static String forkAgent(Map<String, Object> meta) {
        if (!"fork".equals(meta.get("context"))) return null;
        Object agent = meta.get("agent");
        return agent == null || String.valueOf(agent).isEmpty() ? "general-purpose" : String.valueOf(agent);
    }

    static final String SKILL = """
        ---
        name: release-tag
        description: Tag a release and push the tag. Use when the user asks to cut a release.
        disable-model-invocation: true
        argument-hint: "[version]"
        arguments: [version]
        allowed-tools: Bash(git tag *) Bash(git push origin *)
        disallowed-tools: Edit
        ---
        Create the tag $version and push it.
        """;

    public static void main(String[] args) throws IOException {
        Parsed skill = parse(SKILL);
        System.out.println("name: " + commandName(".claude/skills/release-tag/SKILL.md", skill.meta()) + " | legacy file: " + commandName(".claude/commands/standup.md", Map.of()));
        System.out.println("winner of three same-name skills: " + winner(Map.of("project", "p/SKILL.md", "personal", "u/SKILL.md")));
        System.out.println("who can start it: " + py(invocation(skill.meta())));
        String[][] calls = {{"Bash", "git tag v1.2.0"}, {"Bash", "git push --force"}, {"Bash", "rm -rf build"}, {"Edit", ""}, {"Read", ""}};
        for (String[] c : calls) System.out.println(c[0] + " " + py(c[1]) + ": " + toolStatus(skill.meta(), c[0], c[1]));
        System.out.println("bare Bash allowed: " + py(preApproved(Map.of("allowed-tools", "Bash"), "Bash", "rm -rf build")) + " | scoped disallow removes Edit: " + py(removed(Map.of("disallowed-tools", "Edit(src/**)"), "Edit")));
        System.out.println("render: " + render(skill.body(), "v1.2.0", List.of("version")).strip());
        System.out.println("render, no placeholder: " + py(render("Review the change.\n", "123")));
        System.out.println("quoted: " + render("first=$0 second=$1", "\"hello world\" second"));
        System.out.println("fork: " + py2(forkAgent(Map.of("context", "fork"))) + " " + py2(forkAgent(Map.of("context", "fork", "agent", "Explore"))) + " " + py2(forkAgent(Map.of())));
    }

    /** Python's str() of a string or None. */
    private static String py2(String s) {
        return s == null ? "None" : s;
    }
}
