import static harness.Show.py;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
 *
 * <p>A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
 * the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
 * expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
 * variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
 * The configuration is JSON, read with Jackson.
 */
public final class McpConfig {
    static final List<String> SCOPES = List.of("local", "project", "user"); // highest precedence first
    static final Set<String> COVERED = Set.of("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN");
    static final Set<String> REMOTE = Set.of("http", "sse", "ws");
    static final Pattern VAR = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\\}");
    static final Pattern SECRET_KEY = Pattern.compile("token|key|secret|authorization|password", Pattern.CASE_INSENSITIVE);
    static final int DESCRIPTION_LIMIT = 2048; // characters kept of a tool description and of a server's instructions
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The text after expansion and the names that were unset and had no default. */
    record Expanded(String text, List<String> missing) {}

    /** A server entry after expansion, and the warnings it raised. */
    record ExpandedServer(Map<String, Object> entry, List<String> warnings) {}

    /** A server's winning scope and entry. */
    record Source(String scope, Map<String, Object> entry) {}

    /** The servers in use, and the warnings about conflicts. */
    record Resolved(Map<String, Source> servers, List<String> warnings) {}

    static Map<String, Object> parse(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = JSON.readValue(json, LinkedHashMap.class);
            return m;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Expand ${VAR} and ${VAR:-default}. Returns the text and the names that were unset and had no default. */
    static Expanded expand(String text, Map<String, String> env, boolean remote) {
        List<String> missing = new ArrayList<>();
        Matcher m = VAR.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1), dflt = m.group(2);
            String value;
            if (remote && COVERED.contains(name)) value = ""; // never sent toward a remote server, whether or not it is set, and a default is ignored
            else if (env.containsKey(name)) value = env.get(name);
            else if (dflt != null) value = dflt;
            else {
                missing.add(name);
                value = m.group(0); // the unexpanded text is used as written
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return new Expanded(out.toString(), missing);
    }

    static Expanded expand(String text, Map<String, String> env) {
        return expand(text, env, false);
    }

    private static void warn(List<String> warnings, List<String> missing) {
        for (String n : missing) warnings.add(n + " is not set");
    }

    /** Expand the five fields where expansion applies. Returns the entry and the warnings. */
    @SuppressWarnings("unchecked")
    static ExpandedServer expandServer(Map<String, Object> entry, Map<String, String> env) {
        boolean remote = entry.get("type") != null && REMOTE.contains(entry.get("type"));
        List<String> warnings = new ArrayList<>();
        Map<String, Object> out = new LinkedHashMap<>(entry);
        for (String field : List.of("command", "url")) {
            if (!out.containsKey(field)) continue;
            Expanded e = expand((String) out.get(field), env, remote);
            out.put(field, e.text());
            warn(warnings, e.missing());
        }
        if (out.containsKey("args")) {
            List<String> args = new ArrayList<>();
            for (Object arg : (List<Object>) entry.get("args")) {
                Expanded e = expand((String) arg, env, remote);
                args.add(e.text());
                warn(warnings, e.missing());
            }
            out.put("args", args);
        }
        for (String field : List.of("env", "headers")) {
            if (!out.containsKey(field)) continue;
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, Object> kv : ((Map<String, Object>) entry.get(field)).entrySet()) {
                Expanded e = expand((String) kv.getValue(), env, remote);
                values.put(kv.getKey(), e.text());
                warn(warnings, e.missing());
            }
            out.put(field, values);
        }
        return new ExpandedServer(out, warnings);
    }

    /** scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole. */
    static Resolved resolveServers(Map<String, Map<String, Map<String, Object>>> scopes) {
        Map<String, Source> servers = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String scope : SCOPES) {
            for (Map.Entry<String, Map<String, Object>> e : scopes.getOrDefault(scope, Map.of()).entrySet()) {
                String name = e.getKey();
                if (!servers.containsKey(name)) servers.put(name, new Source(scope, e.getValue()));
                else if (!servers.get(name).entry().equals(e.getValue())) {
                    warnings.add(name + " is defined in " + servers.get(name).scope() + " and " + scope + " with different settings: " + servers.get(name).scope() + " wins");
                }
            }
        }
        return new Resolved(servers, warnings);
    }

    /** Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced. */
    @SuppressWarnings("unchecked")
    static List<String> lint(Map<String, Object> config) {
        List<String> findings = new ArrayList<>();
        Map<String, Object> servers = (Map<String, Object>) config.getOrDefault("mcpServers", Map.of());
        for (Map.Entry<String, Object> s : servers.entrySet()) {
            String name = s.getKey();
            Map<String, Object> entry = (Map<String, Object>) s.getValue();
            String kind = (String) entry.getOrDefault("type", "stdio");
            if (REMOTE.contains(kind) && !entry.containsKey("url")) findings.add(name + ": a " + kind + " server needs a url");
            if (kind.equals("stdio") && !entry.containsKey("command")) findings.add(name + ": a stdio server needs a command");
            for (String field : List.of("env", "headers")) {
                for (Map.Entry<String, Object> kv : ((Map<String, Object>) entry.getOrDefault(field, Map.of())).entrySet()) {
                    if (SECRET_KEY.matcher(kv.getKey()).find() && !((String) kv.getValue()).contains("${")) {
                        findings.add(name + ": " + field + "." + kv.getKey() + " holds a literal value, reference an environment variable");
                    }
                }
            }
        }
        return findings;
    }

    /** Python's fnmatch.fnmatchcase: `*` any run, `?` one character, `[...]` a set. */
    static boolean fnmatch(String name, String pattern) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') re.append(".*");
            else if (c == '?') re.append('.');
            else if (c == '[') {
                int j = pattern.indexOf(']', i + 2);
                if (j < 0) re.append("\\[");
                else {
                    String set = pattern.substring(i + 1, j);
                    re.append('[').append(set.startsWith("!") ? "^" + set.substring(1) : set).append(']');
                    i = j;
                }
            } else re.append(Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(re.toString(), Pattern.DOTALL).matcher(name).matches();
    }

    /**
     * allow, ask or deny for an MCP tool named mcp__&lt;server&gt;__&lt;tool&gt;. Deny rules may use globs; an allow rule counts only when the
     * server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask.
     */
    @SuppressWarnings("unchecked")
    static String mcpDecision(Map<String, Object> settings, String tool) {
        Map<String, Object> perms = (Map<String, Object>) settings.getOrDefault("permissions", Map.of());
        for (Object rule : (List<Object>) perms.getOrDefault("deny", List.of())) if (fnmatch(tool, (String) rule)) return "deny";
        for (Object r : (List<Object>) perms.getOrDefault("allow", List.of())) {
            String rule = (String) r;
            String[] parts = rule.split("__", -1);
            if (parts.length >= 3 && parts[0].equals("mcp") && !parts[1].isEmpty() && !parts[1].contains("*") && fnmatch(tool, rule)) return "allow";
        }
        return "ask";
    }

    static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    static String truncate(String text) {
        return truncate(text, DESCRIPTION_LIMIT);
    }

    static String show(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Map<String, String> env = Map.of("GITHUB_TOKEN", "demo-gh", "NPM_TOKEN", "demo-npm");
        Map<String, Object> shared = parse("""
            {"mcpServers": {
              "github": {"type": "http", "url": "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", "headers": {"Authorization": "Bearer ${GITHUB_TOKEN}"}},
              "registry": {"type": "http", "url": "https://registry-mcp.example.com/mcp", "headers": {"Authorization": "Bearer ${NPM_TOKEN}"}},
              "docs": {"command": "python3", "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${DOCS_INDEX}"], "env": {"DOCS_API_KEY": "${DOCS_API_KEY}"}}
            }}""");
        Map<String, Map<String, Object>> sharedServers = (Map<String, Map<String, Object>>) (Object) shared.get("mcpServers");
        System.out.println("expanded entries:");
        for (Map.Entry<String, Map<String, Object>> s : sharedServers.entrySet()) {
            ExpandedServer out = expandServer(s.getValue(), env);
            System.out.println("  " + s.getKey() + ": " + show(out.entry()));
            for (String w : out.warnings()) System.out.println("    warning: " + w);
        }
        Map<String, Map<String, Object>> user = (Map<String, Map<String, Object>>) (Object) parse("""
            {"github": {"type": "http", "url": "https://other.example.com/mcp"}, "scratch": {"command": "python3", "args": ["scratch.py"]}}""");
        Resolved resolved = resolveServers(Map.of("project", sharedServers, "user", user));
        List<String> parts = new ArrayList<>();
        resolved.servers().forEach((n, s) -> parts.add(n + " from " + s.scope()));
        System.out.println("resolved servers: " + String.join(", ", parts));
        for (String w : resolved.warnings()) System.out.println("  warning: " + w);
        Map<String, Object> bad = parse("""
            {"mcpServers": {"api": {"type": "http", "headers": {"X-Api-Key": "abc123"}}, "tool": {"args": ["x"]}}}""");
        System.out.println("lint of a shared file with literal secrets:");
        for (String f : lint(bad)) System.out.println("  " + f);
        List<String> clean = lint(shared);
        System.out.println("lint of the file above: " + (clean.isEmpty() ? "no findings" : py(clean)));
        Map<String, Object> settings = parse("""
            {"permissions": {"allow": ["mcp__docs__*", "mcp__*"], "deny": ["mcp__github__delete_*"]}}""");
        for (String tool : List.of("mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository")) {
            System.out.println(tool + " -> " + mcpDecision(settings, tool));
        }
        System.out.println("a description of 3000 characters keeps " + truncate("x".repeat(3000)).length() + " of them");
    }
}
