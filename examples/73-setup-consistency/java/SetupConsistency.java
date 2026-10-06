import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.
 *
 * <p>The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
 * mcp__&lt;server&gt;__&lt;tool&gt; in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
 * .claude/agents/. Nothing here starts Claude Code or an MCP server.
 */
public final class SetupConsistency {
    private static final System.Logger LOG = System.getLogger(SetupConsistency.class.getName());
    static final Path HERE = Path.of("..").toAbsolutePath().normalize();
    private static final Pattern SECRET_KEY = Pattern.compile("token|key|secret|authorization", Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper JSON = new ObjectMapper();

    record Project(JsonNode servers, Map<String, List<String>> agents, JsonNode perms) {}

    /** The tools line of each subagent, or null when it has none (the subagent then inherits every tool). */
    static Map<String, List<String>> agents(Path root) {
        Map<String, List<String>> found = new LinkedHashMap<>();
        try (Stream<Path> s = Files.list(root.resolve(".claude/agents"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".md")).sorted().toList()) {
                Matcher head = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(Files.readString(p));
                head.find();
                Matcher tools = Pattern.compile("^tools:\\s*(.*)$", Pattern.MULTILINE).matcher(head.group(1));
                Matcher name = Pattern.compile("^name:\\s*(\\S+)", Pattern.MULTILINE).matcher(head.group(1));
                name.find();
                found.put(name.group(1), tools.find() ? List.of(tools.group(1).split(",")).stream().map(String::strip).toList() : null);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return found;
    }

    static Project load(Path root) {
        try {
            return new Project(JSON.readTree(Files.readString(root.resolve(".mcp.json"))).get("mcpServers"), agents(root),
                JSON.readTree(Files.readString(root.resolve(".claude/settings.json"))).path("permissions"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> literalSecrets(JsonNode servers) {
        List<String> out = new ArrayList<>();
        servers.fields().forEachRemaining(server -> {
            for (String field : List.of("headers", "env")) {
                server.getValue().path(field).fields().forEachRemaining(kv -> {
                    if (SECRET_KEY.matcher(kv.getKey()).find() && !kv.getValue().asText().contains("${")) out.add(server.getKey() + " " + field + "." + kv.getKey());
                });
            }
        });
        return out;
    }

    private static List<String> rules(JsonNode perms, String kind) {
        List<String> out = new ArrayList<>();
        perms.path(kind).forEach(n -> out.add(n.asText()));
        return out;
    }

    static List<String> audit(Path root) {
        Project p = load(root);
        List<String> found = new ArrayList<>();
        literalSecrets(p.servers()).forEach(s -> found.add("literal-secret: " + s));
        List<String[]> refs = new ArrayList<>();
        p.agents().forEach((agent, tools) -> {
            if (tools != null) tools.stream().filter(t -> t.startsWith("mcp__")).forEach(t -> refs.add(new String[] {agent, t}));
        });
        for (String kind : List.of("allow", "deny")) rules(p.perms(), kind).stream().filter(r -> r.startsWith("mcp__")).forEach(r -> refs.add(new String[] {"settings", r}));
        for (String[] ref : refs) {
            String server = ref[1].split("__")[1];
            if (!p.servers().has(server)) found.add("unknown-server: " + server + " (" + ref[0] + ")");
        }
        p.agents().forEach((name, tools) -> {
            if (tools == null) found.add("agent-inherits-all: " + name);
            else if (tools.contains("Bash")) found.add("agent-bare-bash: " + name);
        });
        if (!rules(p.perms(), "deny").contains("Read(./.env)")) found.add("env-readable");
        if (rules(p.perms(), "allow").stream().anyMatch(r -> r.equals("Edit") || r.equals("Write"))) found.add("bare-write-allowed");
        return found;
    }

    public static void main(String[] args) {
        for (String name : List.of("project-before", "project-after")) {
            Project p = load(HERE.resolve(name));
            int rules = rules(p.perms(), "allow").size() + rules(p.perms(), "deny").size();
            System.out.println(name + ": " + p.servers().size() + " servers, " + p.agents().size() + " agents, " + rules + " permission rules");
            List<String> found = audit(HERE.resolve(name));
            found.forEach(f -> System.out.println("  finding: " + f));
            if (found.isEmpty()) {
                System.out.println("  no findings");
                p.agents().forEach((agent, tools) -> System.out.println("  " + agent + ": " + String.join(", ", tools)));
            }
        }
    }
}
