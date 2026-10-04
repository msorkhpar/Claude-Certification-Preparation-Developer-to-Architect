import static harness.Show.py;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * <p>Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network. The JSON is read with Jackson.
 */
public final class CiGate {
    static final List<String> SEVERITIES = List.of("low", "medium", "high");
    private static final ObjectMapper JSON = new ObjectMapper();

    static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static final JsonNode REVIEW_SCHEMA = json("""
        {"type": "object",
         "properties": {"findings": {"type": "array", "items": {"type": "object", "properties": {
           "file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string", "enum": ["bug", "security", "style", "other"]},
           "severity": {"type": "string", "enum": ["low", "medium", "high"]}, "issue": {"type": "string"}, "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}},
           "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], "additionalProperties": false}}},
         "required": ["findings"], "additionalProperties": false}""");

    /** What a team decides about review findings: the lowest severity to post, the categories to drop and the severities that fail the job. */
    record Policy(String minSeverity, List<String> disabledCategories, List<String> failOn) {}

    /** One review comment to post. */
    record Comment(String file, int line, String severity, String body) {}

    /** The decision: the exit status, the comments to post and what went wrong. */
    record Decision(int exit, List<Comment> comments, List<String> problems) {}

    /** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
    static List<String> buildCommand(String prompt, JsonNode schema, int maxTurns, List<String> tools, String contextFile) {
        return List.of("claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", schema.toString(),
            "--max-turns", String.valueOf(maxTurns), "--allowedTools", String.join(",", tools));
    }

    static List<String> buildCommand(String prompt, JsonNode schema) {
        return buildCommand(prompt, schema, 8, List.of("Read", "Grep", "Glob", "Bash(git diff *)"), "CLAUDE.md");
    }

    private static String stripQuotes(String s) {
        int from = 0, to = s.length();
        while (from < to && (s.charAt(from) == '"' || s.charAt(from) == '\'')) from++;
        while (to > from && (s.charAt(to - 1) == '"' || s.charAt(to - 1) == '\'')) to--;
        return s.substring(from, to);
    }

    private static boolean has(String regex, String text) {
        return Pattern.compile(regex).matcher(text).find();
    }

    /** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
    static List<String> lintCommand(String text) {
        text = text.replaceAll("\\\\\\n\\s*", " ");
        List<String> found = new ArrayList<>();
        if (!has("\\sclaude\\b", " " + text)) return List.of("no-claude-command");
        if (!has("\\s(-p|--print)\\b", text)) found.add("no-print");
        if (!has("--output-format[ =]json\\b", text)) found.add("no-json");
        if (!has("--json-schema\\b", text)) found.add("no-schema");
        Matcher turns = Pattern.compile("--max-turns[ =](\\d+)").matcher(text);
        if (!turns.find() || Integer.parseInt(turns.group(1)) > 20) found.add("no-turn-limit");
        Matcher allowed = Pattern.compile("--allowed[Tt]ools[ =](\"[^\"]*\"|'[^']*'|\\S+)").matcher(text);
        boolean given = allowed.find();
        List<String> names = new ArrayList<>();
        if (given) {
            Matcher n = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(stripQuotes(allowed.group(1).strip()));
            while (n.find()) names.add(n.group());
        }
        if (!given) found.add("no-tool-list");
        else if (names.stream().anyMatch(n -> List.of("Bash", "Edit", "Write", "MultiEdit", "NotebookEdit").contains(n))) found.add("wide-tools");
        if (!text.contains("--bare")) found.add("no-bare");
        else if (!has("--append-system-prompt(-file)?\\b", text)) found.add("bare-without-context");
        return found;
    }

    private static boolean isType(JsonNode value, String kind) {
        return switch (kind) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            default -> throw new IllegalArgumentException(kind);
        };
    }

    /** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
    static List<String> schemaCheck(JsonNode value, JsonNode schema, String path) {
        List<String> wants = new ArrayList<>();
        JsonNode want = schema.get("type");
        if (want != null) {
            if (want.isArray()) want.forEach(w -> wants.add(w.asText()));
            else wants.add(want.asText());
        }
        boolean ok = wants.isEmpty() || wants.stream().anyMatch(k -> isType(value, k));
        if (!ok) return List.of(path + ": expected " + String.join(" or ", wants));
        List<String> errors = new ArrayList<>();
        JsonNode enumeration = schema.get("enum");
        if (enumeration != null) {
            boolean found = false;
            for (JsonNode e : enumeration) found |= e.equals(value);
            if (!found) errors.add(path + ": " + py(value) + " is not one of " + py(enumeration));
        }
        if (value.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null) for (JsonNode k : required) if (!value.has(k.asText())) errors.add(path + "." + k.asText() + ": is required");
            JsonNode extra = schema.get("additionalProperties");
            JsonNode properties = schema.has("properties") ? schema.get("properties") : JSON.createObjectNode();
            if (extra != null && extra.isBoolean() && !extra.asBoolean()) {
                for (var it = value.fieldNames(); it.hasNext();) {
                    String k = it.next();
                    if (!properties.has(k)) errors.add(path + "." + k + ": is not allowed");
                }
            }
            for (var it = properties.fields(); it.hasNext();) {
                var sub = it.next();
                if (value.has(sub.getKey())) errors.addAll(schemaCheck(value.get(sub.getKey()), sub.getValue(), path + "." + sub.getKey()));
            }
        }
        if (value.isArray() && schema.has("items")) {
            for (int i = 0; i < value.size(); i++) errors.addAll(schemaCheck(value.get(i), schema.get("items"), path + "[" + i + "]"));
        }
        return errors;
    }

    static List<String> schemaCheck(JsonNode value, JsonNode schema) {
        return schemaCheck(value, schema, "$");
    }

    private static String text(JsonNode n) {
        return n.asText();
    }

    /** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
    static Decision gate(String stdout, int exitCode, JsonNode schema, Policy policy) {
        List<String> problems = new ArrayList<>();
        if (exitCode != 0) problems.add("claude exited with status " + exitCode);
        JsonNode envelope;
        try {
            envelope = JSON.readTree(stdout);
        } catch (Exception e) {
            envelope = null;
        }
        if (envelope == null || !envelope.isObject()) {
            problems.add("the output is not a JSON object");
            return new Decision(1, List.of(), problems);
        }
        JsonNode subtype = envelope.get("subtype");
        if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual() || !subtype.asText().equals("success")) {
            problems.add("the run ended with " + (subtype == null || subtype.isNull() ? "None" : subtype.asText()));
        }
        JsonNode output = envelope.get("structured_output");
        if (output == null || output.isNull()) problems.add("the result has no structured_output");
        else for (String e : schemaCheck(output, schema)) problems.add("schema " + e);
        if (!problems.isEmpty()) return new Decision(1, List.of(), problems);
        int floor = SEVERITIES.indexOf(policy.minSeverity());
        List<Comment> comments = new ArrayList<>();
        for (JsonNode f : output.get("findings")) {
            if (policy.disabledCategories().contains(text(f.get("category"))) || SEVERITIES.indexOf(text(f.get("severity"))) < floor) continue;
            comments.add(new Comment(text(f.get("file")), f.get("line").asInt(), text(f.get("severity")), text(f.get("issue")) + " Suggested fix: " + text(f.get("suggested_fix"))));
        }
        boolean blocked = comments.stream().anyMatch(c -> policy.failOn().contains(c.severity()));
        return new Decision(blocked ? 1 : 0, comments, List.of());
    }

    static String envelope(Map<String, Object> over) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "result");
        body.put("subtype", "success");
        body.put("is_error", false);
        body.put("result", "done");
        body.put("num_turns", 3);
        body.put("total_cost_usd", 0.05);
        body.put("session_id", "s-1");
        body.putAll(over);
        try {
            return JSON.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String envelope() {
        return envelope(Map.of());
    }

    /** Python's shlex.quote for one word. */
    static String quote(String s) {
        if (s.isEmpty()) return "''";
        return s.matches("[\\w@%+=:,./-]+") ? s : "'" + s.replace("'", "'\"'\"'") + "'";
    }

    static String join(List<String> words) {
        return words.stream().map(CiGate::quote).collect(Collectors.joining(" "));
    }

    static Map<String, Object> finding(String file, int line, String category, String severity, String issue, String fix, String pattern) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("file", file);
        f.put("line", line);
        f.put("category", category);
        f.put("severity", severity);
        f.put("issue", issue);
        f.put("suggested_fix", fix);
        f.put("detected_pattern", pattern);
        return f;
    }

    public static void main(String[] args) {
        List<String> command = buildCommand("Review the diff on standard input.", json("{\"type\": \"object\", \"properties\": {\"findings\": {\"type\": \"array\"}}}"), 8, List.of("Read", "Grep"), "CLAUDE.md");
        System.out.println("command: " + String.join(" ", command.subList(0, 5)) + " ... " + py(command.subList(command.size() - 4, command.size())));
        System.out.println("lint, the good step: " + py(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'")));
        System.out.println("lint, a careless step: " + py(lintCommand("claude 'Review the change' --allowedTools Bash,Edit")));
        Policy policy = new Policy("medium", List.of("style"), List.of("high"));
        List<Map<String, Object>> findings = List.of(
            finding("api.py", 12, "bug", "high", "Unchecked None.", "Return early.", "missing-none-check"),
            finding("api.py", 40, "style", "high", "Long line.", "Wrap it.", "line-length"),
            finding("ui.py", 3, "bug", "low", "Odd name.", "Rename.", "naming"));
        Map<String, String[]> runs = new LinkedHashMap<>();
        runs.put("a finding that blocks", new String[] {envelope(Map.of("structured_output", Map.of("findings", findings))), "0"});
        runs.put("no findings", new String[] {envelope(Map.of("structured_output", Map.of("findings", List.of()))), "0"});
        runs.put("turn limit", new String[] {envelope(Map.of("subtype", "error_max_turns", "is_error", true)), "1"});
        runs.put("success without output", new String[] {envelope(), "0"});
        runs.put("not JSON", new String[] {"Error: no key", "1"});
        runs.put("wrong shape", new String[] {envelope(Map.of("structured_output", Map.of("findings", List.of(Map.of("file", "a.py"))))), "0"});
        runs.forEach((label, run) -> {
            Decision d = gate(run[0], Integer.parseInt(run[1]), REVIEW_SCHEMA, policy);
            System.out.println(label + ": exit " + d.exit() + ", " + d.comments().size() + " comment(s)" + (d.problems().isEmpty() ? "" : ", " + d.problems().get(0)));
        });
    }
}
