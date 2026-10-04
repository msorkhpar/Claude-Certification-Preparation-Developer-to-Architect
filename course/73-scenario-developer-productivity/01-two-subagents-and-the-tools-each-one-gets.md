# Two subagents and the tools each one gets

**Level:** Architect · **Module 73:** Scenario: developer productivity · **Page 1 of 2**
**Exams:** A2; A3; S4

**After this page you can** give an agent that explores a legacy system and an agent that generates boilerplate exactly the tools their jobs need, restrict file edits to one folder with a rule that Claude Code consults, let agents read an MCP server without letting them change it, and audit the three files that must agree: the servers, the subagents and the permission rules.

Checked on 2026-10-04 against the Claude Code documentation pages "Configure permissions", "Create custom subagents", "Connect Claude Code to tools via MCP" and "Best practices for Claude Code" (the pages name Claude Code version 2.1.286), and against the Architect exam guide (version 1.0, scenario 4). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small folders of plain files; it does not start Claude Code or a server. This page is a capstone: it uses modules 47, 52, 54, 55 and 56 and puts them in the order the exam asks about.

## Why it matters

Scenario S4 of the Architect exam is a developer-productivity tool. It helps engineers explore unfamiliar codebases, understand legacy systems, generate boilerplate and automate repetitive tasks, with the built-in tools (Read, Write, Bash, Grep, Glob) and Model Context Protocol servers. Its primary domains are tool design and MCP integration, Claude Code configuration, and agentic architecture. The questions are about fit. Which tools belong to which job, which rule limits a write to one folder, which setting lets an agent read a ticket system without being able to close a ticket. The mistakes are in the details: a tool list left out, a rule written for the wrong tool, a server named in one file and missing in another.

## The idea

### The scenario in plain words

A team has a legacy system that nobody fully understands. It wants one helper that answers questions about how the system works and never changes it, a second helper that writes the boilerplate of a new module in a folder reserved for generated code, a documentation server and a ticket server that both helpers can reach, and a setup that any new developer gets with a clone and a few environment variables.

### The requirement decides the tools

| The requirement | The configuration | Why the tempting choice fails |
|---|---|---|
| Explore the legacy system and change nothing | A project subagent whose `tools` line lists only reading and searching tools: `Read`, `Grep`, `Glob` and the documentation search tool | Leaving the line out grants every tool. The shell tool can write files. A prompt sentence is a request, not a limit |
| Generate boilerplate, and only inside `src/generated` | A subagent with `Read`, `Glob`, `Edit` and `Write`, and one allow rule, `Edit(src/generated/**)` | A bare `Edit` rule approves every edit. A path rule written for `Write` is never consulted. A prompt that names the folder is a request |
| Read tickets, never create or delete them | Allow the get tool by name; deny the create and delete tools | An allow rule for the whole server approves the destructive tools. In a settings file, a deny rule with parentheses on an MCP tool is skipped |
| A token for the ticket server | `Bearer ${TICKETS_TOKEN}` in `.mcp.json`, each developer exports the variable | A literal token in the file is committed with it. A default value is a credential in the file |
| Keep `.env` out of every session | A deny rule, `Read(./.env)` | A sentence in a memory file is context, not enforcement |

The table is the capstone of four earlier modules, and each row is a sentence the documentation states. Here they are, in the order the example checks them.

### Tools are a list, and leaving it out is the widest list

A project subagent is a markdown file in `.claude/agents/` with a front matter that holds a `name`, a `description` and, optionally, `tools`. The `description` is how the main conversation decides when to delegate, so it says when to use the subagent and starts with "Use when". The `tools` line is the allowlist. A subagent that omits `tools` inherits every tool that is available to subagents, so leaving the line out grants the most, not the least. A sentence in the prompt is a request that the model weighs; the `tools` line is a list the subagent cannot go beyond. The shell tool can write files through redirections and commands, so a read-only agent has no use for it. A subagent works in its own context window and returns its result to the main conversation, which is the other reason to give exploration to one: the file contents it reads stay out of the main context. A longer memory file does not do that job: the root file loads in every session and costs context each time. Claude Code ships such a helper itself, the Explore subagent, which is read-only (module 67, "Exploring a large codebase", goes further); a project subagent of your own is the way to give the same shape the project's documentation server too.

### Writing in one folder: the rule must be about Edit

Permission rules are checked in a fixed order. In the documentation's words, "Rules are evaluated in order: deny, then ask, then allow." A matching deny rule wins over any allow rule, whatever the specificity. For files, an `Edit` rule applies to every built-in tool that edits files, so one `Edit(src/generated/**)` allow rule covers the `Edit` tool and the `Write` tool alike. A path rule written for the Write tool is accepted and never consulted, because file permissions are checked against Edit and Read path rules only; the documentation says to use `Edit(docs/**)` in place of `Write(docs/**)`. The path in a rule follows gitignore syntax and, for an allow rule, `src/generated/**` is anchored at the working directory, so it matches that folder and nothing else. A bare `Edit` rule has no path and approves every edit. Denying the shell does not limit the file tools, which have their own rules.

### MCP servers: one name in three places

A server is configured once, in the project's `.mcp.json`, and referred to in two other places by the same name. Its tools are written `mcp__<server>__<tool>`, in a subagent's `tools` line and in a permission rule. A rule can name a whole server (`mcp__tickets`), every tool of it (`mcp__tickets__*`) or one tool (`mcp__tickets__get_ticket`). In a settings file, a rule for an MCP tool that carries parentheses is skipped, so it matches nothing: an argument pattern is not a way to narrow an MCP tool there. To make a server read-only for the agents, allow the reading tools by name and deny the others by name, and remember that a deny rule beats any allow.

The three places must agree. A subagent whose `tools` line names `mcp__wiki__search` when `.mcp.json` has no `wiki` server holds a reference to something the project never defines. The files do not say so by themselves; a check that reads them does.

### Credentials come from the environment

The `.mcp.json` of a project is committed so that the team shares its servers. Expansion of `${VAR}` and `${VAR:-default}` works in a server's `command`, `args`, `env`, `url` and `headers`, so a token is written as a reference and each developer sets the variable in the shell. `${VAR:-default}` expands to the variable if it is set and to the default otherwise. A default is right for a value that is not secret, such as the ticket server's address, and wrong for a token: a default for a token would be a credential in the file. The project's servers load without a prompt in `claude -p` and SDK runs, so a pull request that changes `.mcp.json` deserves the review of a change to code (module 55).

### The audit: a checklist that reads the files

The example implements the checklist this course uses for the scenario, on two projects beside it: `project-before`, a draft that has the usual mistakes, and `project-after`, where every row of the table has its mechanism. For each project the audit reports:

- `literal-secret`: a header or environment entry whose name says token, key, secret or authorization and whose value has no `${` reference.
- `unknown-server`: a tool reference, in a subagent or in a rule, that names a server `.mcp.json` does not configure.
- `agent-inherits-all` and `agent-bare-bash`: a subagent with no `tools` line, or with the shell tool in it.
- `env-readable`: the settings do not deny `Read(./.env)`.
- `bare-write-allowed`: an allow rule that approves the whole `Edit` or `Write` tool.

For a project with no findings, the example prints the tools each subagent has.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the scenario's agent uses the built-in tools Read, Write, Bash, Grep and Glob and integrates MCP servers, and the questions about it ask which tool or server fits which job and where the configuration goes. *What the product does now (documentation checked 2026-10-04):* the file tools are Read, Edit and Write, and a permission rule for file edits is written for `Edit`, which covers Write too; a rule with a path for Write is accepted and never consulted. A subagent that lists `tools` is limited to them, and one that omits them inherits every tool. So on the exam, give each agent its narrow tool list and never rely on a prompt for a limit; in a real project, write the file rule for Edit.

### The example

The example loads each project and runs the checklist. It prints one line of facts per project, then the findings for the draft, and for the fixed project the tools of each subagent. The program and its output are the same in all four languages.

<!-- example: m73-setup-consistency tabs: python,typescript,java,kotlin -->
```python
"""Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.

The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
.claude/agents/. Nothing here starts Claude Code or an MCP server.
"""
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
SECRET_KEY = re.compile(r"token|key|secret|authorization", re.I)


def agents(root):
    found = {}
    for path in sorted((root / ".claude/agents").glob("*.md")):
        head = re.match(r"---\n(.*?)\n---\n", path.read_text(), re.S).group(1)
        tools = re.search(r"^tools:\s*(.*)$", head, re.M)
        found[re.search(r"^name:\s*(\S+)", head, re.M).group(1)] = [t.strip() for t in tools.group(1).split(",")] if tools else None
    return found


def load(root):
    return json.loads((root / ".mcp.json").read_text())["mcpServers"], agents(root), json.loads((root / ".claude/settings.json").read_text()).get("permissions", {})


def literal_secrets(servers):
    out = []
    for name, server in servers.items():
        for field in ("headers", "env"):
            for key, value in server.get(field, {}).items():
                if SECRET_KEY.search(key) and "${" not in value:
                    out.append(f"{name} {field}.{key}")
    return out


def audit(root):
    servers, subagents, perms = load(root)
    found = [f"literal-secret: {s}" for s in literal_secrets(servers)]
    refs = [(agent, t) for agent, tools in subagents.items() for t in tools or [] if t.startswith("mcp__")]
    refs += [("settings", r) for kind in ("allow", "deny") for r in perms.get(kind, []) if r.startswith("mcp__")]
    for who, ref in refs:
        server = ref.split("__")[1]
        if server not in servers:
            found.append(f"unknown-server: {server} ({who})")
    for name, tools in subagents.items():
        if tools is None:
            found.append(f"agent-inherits-all: {name}")
        elif "Bash" in tools:
            found.append(f"agent-bare-bash: {name}")
    if "Read(./.env)" not in perms.get("deny", []):
        found.append("env-readable")
    if any(r in ("Edit", "Write") for r in perms.get("allow", [])):
        found.append("bare-write-allowed")
    return found


def main():
    for name in ("project-before", "project-after"):
        servers, subagents, perms = load(HERE / name)
        rules = sum(len(perms.get(k, [])) for k in ("allow", "deny"))
        print(f"{name}: {len(servers)} servers, {len(subagents)} agents, {rules} permission rules")
        found = audit(HERE / name)
        for finding in found:
            print(f"  finding: {finding}")
        if not found:
            print("  no findings")
            for agent, tools in subagents.items():
                print(f"  {agent}: {', '.join(tools)}")


if __name__ == "__main__":
    main()
```
```text
project-before: 2 servers, 2 agents, 2 permission rules
  finding: literal-secret: tickets headers.Authorization
  finding: unknown-server: wiki (explorer)
  finding: agent-bare-bash: explorer
  finding: agent-inherits-all: scaffolder
  finding: env-readable
  finding: bare-write-allowed
project-after: 2 servers, 2 agents, 5 permission rules
  no findings
  explorer: Read, Grep, Glob, mcp__docs__search
  scaffolder: Read, Glob, Edit, Write
```
```typescript
// Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.
//
// The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
// documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
// mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
// .claude/agents/. Nothing here starts Claude Code or an MCP server.
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = fileURLToPath(new URL("..", import.meta.url));
const SECRET_KEY = /token|key|secret|authorization/i;

export type Server = { headers?: Record<string, string>; env?: Record<string, string> };

export function agents(root: string): Record<string, string[] | null> {
  const found: Record<string, string[] | null> = {};
  for (const file of readdirSync(join(root, ".claude/agents")).filter((f) => f.endsWith(".md")).sort()) {
    const head = readFileSync(join(root, ".claude/agents", file), "utf8").match(/^---\n([\s\S]*?)\n---\n/)![1];
    const tools = head.match(/^tools:\s*(.*)$/m);
    found[head.match(/^name:\s*(\S+)/m)![1]] = tools ? tools[1].split(",").map((t) => t.trim()) : null;
  }
  return found;
}

export function load(root: string) {
  const servers: Record<string, Server> = JSON.parse(readFileSync(join(root, ".mcp.json"), "utf8")).mcpServers;
  const perms = JSON.parse(readFileSync(join(root, ".claude/settings.json"), "utf8")).permissions ?? {};
  return { servers, subagents: agents(root), perms };
}

export function literalSecrets(servers: Record<string, Server>): string[] {
  const out: string[] = [];
  for (const [name, server] of Object.entries(servers)) {
    for (const field of ["headers", "env"] as const) {
      for (const [key, value] of Object.entries(server[field] ?? {})) if (SECRET_KEY.test(key) && !value.includes("${")) out.push(`${name} ${field}.${key}`);
    }
  }
  return out;
}

export function audit(root: string): string[] {
  const { servers, subagents, perms } = load(root);
  const found: string[] = literalSecrets(servers).map((s) => `literal-secret: ${s}`);
  const refs: Array<[string, string]> = [];
  for (const [agent, tools] of Object.entries(subagents)) for (const t of tools ?? []) if (t.startsWith("mcp__")) refs.push([agent, t]);
  for (const kind of ["allow", "deny"]) for (const r of perms[kind] ?? []) if (r.startsWith("mcp__")) refs.push(["settings", r]);
  for (const [who, ref] of refs) {
    const server = ref.split("__")[1];
    if (!(server in servers)) found.push(`unknown-server: ${server} (${who})`);
  }
  for (const [name, tools] of Object.entries(subagents)) {
    if (tools === null) found.push(`agent-inherits-all: ${name}`);
    else if (tools.includes("Bash")) found.push(`agent-bare-bash: ${name}`);
  }
  if (!(perms.deny ?? []).includes("Read(./.env)")) found.push("env-readable");
  if ((perms.allow ?? []).some((r: string) => r === "Edit" || r === "Write")) found.push("bare-write-allowed");
  return found;
}

function main() {
  for (const name of ["project-before", "project-after"]) {
    const { servers, subagents, perms } = load(join(HERE, name));
    const rules = (perms.allow ?? []).length + (perms.deny ?? []).length;
    console.log(`${name}: ${Object.keys(servers).length} servers, ${Object.keys(subagents).length} agents, ${rules} permission rules`);
    const found = audit(join(HERE, name));
    for (const finding of found) console.log(`  finding: ${finding}`);
    if (found.length === 0) {
      console.log("  no findings");
      for (const [agent, tools] of Object.entries(subagents)) console.log(`  ${agent}: ${tools!.join(", ")}`);
    }
  }
}

if (import.meta.main) main();
```
```text
project-before: 2 servers, 2 agents, 2 permission rules
  finding: literal-secret: tickets headers.Authorization
  finding: unknown-server: wiki (explorer)
  finding: agent-bare-bash: explorer
  finding: agent-inherits-all: scaffolder
  finding: env-readable
  finding: bare-write-allowed
project-after: 2 servers, 2 agents, 5 permission rules
  no findings
  explorer: Read, Grep, Glob, mcp__docs__search
  scaffolder: Read, Glob, Edit, Write
```
```java
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
```
```text
project-before: 2 servers, 2 agents, 2 permission rules
  finding: literal-secret: tickets headers.Authorization
  finding: unknown-server: wiki (explorer)
  finding: agent-bare-bash: explorer
  finding: agent-inherits-all: scaffolder
  finding: env-readable
  finding: bare-write-allowed
project-after: 2 servers, 2 agents, 5 permission rules
  no findings
  explorer: Read, Grep, Glob, mcp__docs__search
  scaffolder: Read, Glob, Edit, Write
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.
 *
 * The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
 * mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
 * .claude/agents/. Nothing here starts Claude Code or an MCP server.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()
private val SECRET_KEY = Regex("token|key|secret|authorization", RegexOption.IGNORE_CASE)
private val JSON = ObjectMapper()

class Project(val servers: JsonNode, val agents: Map<String, List<String>?>, val perms: JsonNode)

/** The tools line of each subagent, or null when it has none (the subagent then inherits every tool). */
fun agents(root: Path): Map<String, List<String>?> {
    val found = linkedMapOf<String, List<String>?>()
    for (p in Files.list(root.resolve(".claude/agents")).use { s -> s.filter { it.toString().endsWith(".md") }.sorted().toList() }) {
        val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(Files.readString(p))!!.groupValues[1]
        val tools = Regex("^tools:\\s*(.*)$", RegexOption.MULTILINE).find(head)
        found[Regex("^name:\\s*(\\S+)", RegexOption.MULTILINE).find(head)!!.groupValues[1]] = tools?.groupValues?.get(1)?.split(",")?.map { it.trim() }
    }
    return found
}

fun load(root: Path) = Project(JSON.readTree(Files.readString(root.resolve(".mcp.json"))).get("mcpServers"), agents(root),
    JSON.readTree(Files.readString(root.resolve(".claude/settings.json"))).path("permissions"))

fun literalSecrets(servers: JsonNode): List<String> {
    val out = mutableListOf<String>()
    for ((name, server) in servers.fields()) {
        for (field in listOf("headers", "env")) {
            for ((key, value) in server.path(field).fields()) if (SECRET_KEY.containsMatchIn(key) && "\${" !in value.asText()) out += "$name $field.$key"
        }
    }
    return out
}

private fun rules(perms: JsonNode, kind: String): List<String> = perms.path(kind).map { it.asText() }

fun audit(root: Path): List<String> {
    val p = load(root)
    val found = literalSecrets(p.servers).map { "literal-secret: $it" }.toMutableList()
    val refs = mutableListOf<Pair<String, String>>()
    for ((agent, tools) in p.agents) for (t in tools ?: listOf()) if (t.startsWith("mcp__")) refs += agent to t
    for (kind in listOf("allow", "deny")) for (r in rules(p.perms, kind)) if (r.startsWith("mcp__")) refs += "settings" to r
    for ((who, ref) in refs) {
        val server = ref.split("__")[1]
        if (!p.servers.has(server)) found += "unknown-server: $server ($who)"
    }
    for ((name, tools) in p.agents) {
        if (tools == null) found += "agent-inherits-all: $name" else if ("Bash" in tools) found += "agent-bare-bash: $name"
    }
    if ("Read(./.env)" !in rules(p.perms, "deny")) found += "env-readable"
    if (rules(p.perms, "allow").any { it == "Edit" || it == "Write" }) found += "bare-write-allowed"
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val p = load(HERE.resolve(name))
        val rules = rules(p.perms, "allow").size + rules(p.perms, "deny").size
        println("$name: ${p.servers.size()} servers, ${p.agents.size} agents, $rules permission rules")
        val found = audit(HERE.resolve(name))
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for ((agent, tools) in p.agents) println("  $agent: ${tools!!.joinToString(", ")}")
        }
    }
}
```
```text
project-before: 2 servers, 2 agents, 2 permission rules
  finding: literal-secret: tickets headers.Authorization
  finding: unknown-server: wiki (explorer)
  finding: agent-bare-bash: explorer
  finding: agent-inherits-all: scaffolder
  finding: env-readable
  finding: bare-write-allowed
project-after: 2 servers, 2 agents, 5 permission rules
  no findings
  explorer: Read, Grep, Glob, mcp__docs__search
  scaffolder: Read, Glob, Edit, Write
```
<!-- /example -->

The draft has a literal token in the ticket header, an explorer that has the shell tool and names a `wiki` server nobody configured, a scaffolder with no `tools` line, a settings file that allows the whole `Write` tool and does not deny `.env`. The fixed project has none of those findings.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"One general helper with every tool, told in its prompt to explore only."** It is tempting because it is one file and the instruction is clear. The exam rejects it: a prompt is a request, and the tool list is the limit.
2. **"Allow the whole ticket server and let the agents decide what to call."** It is tempting because it is short. The exam rejects it: a whole-server rule approves the creating and deleting tools too.
3. **"Narrow the destructive tool with a pattern on its argument."** It is tempting because path rules narrow file tools. In a settings file, a rule for an MCP tool that carries parentheses is skipped, so it narrows nothing.
4. **"Give the token a default so that a fresh clone works."** It is tempting because the tools then work at once. The default is a credential in a committed file.

## Quiz

1. The audit of the draft prints `bare-write-allowed`. What removes that finding?
   - **a**: Giving the scaffolding subagent a tools line of its own, with a few names
   - **b**: Adding the file-writing tool to the tools line of the subagent that generates the code
   - **c**: Denying `Read(./.env)` in the same settings file as the allow rule
   - **d**: Replacing the whole-tool allow rule with one limited to `src/generated/`

2. A boilerplate agent may change files only in `src/generated`. Which settings give that?
   - **a**: An allow rule for the Edit tool, limited to that folder
   - **b**: An allow rule for the Write tool, limited to that folder
   - **c**: An allow rule for the whole Edit tool, plus a prompt naming the folder
   - **d**: A deny rule for the shell tool, since only commands can write

3. A ticket server lets a caller fetch, add and remove tickets, and agents may only look at them. Which settings give that?
   - **a**: Deny the delete tool in the settings with a rule that matches on the ticket number
   - **b**: Allow every tool of the server, and tell the agents to read only
   - **c**: Allow the get tool by name, and deny the create and delete tools by name
   - **d**: Deny the whole server, then allow its get tool by name

<details>
<summary>Answer key</summary>

1. **d**. The finding is about an allow rule that covers the whole tool: "`bare-write-allowed`: an allow rule that approves the whole `Edit` or `Write` tool." *b* is ruled out because a tools line lists tools and approves nothing in settings: "The `tools` line is the allowlist." *c* is ruled out because that is another finding: "`env-readable`: the settings do not deny `Read(./.env)`." *a* is ruled out because that is a third: "`agent-inherits-all` and `agent-bare-bash`: a subagent with no `tools` line, or with the shell tool in it".
2. **a**. An Edit rule covers every built-in file-editing tool and a path keeps it to one folder. *b* is ruled out because a Write path rule is never read: "A path rule written for the Write tool is accepted and never consulted". *c* is ruled out because a bare rule has no path: "A bare `Edit` rule has no path and approves every edit." *d* is ruled out because the file tools have rules of their own: "Denying the shell does not limit the file tools".
3. **c**. Allow by name and deny the others by name. *b* is ruled out because a sentence is no limit: "A sentence in the prompt is a request that the model weighs". *a* is ruled out because the rule is skipped: "In a settings file, a rule for an MCP tool that carries parentheses is skipped, so it matches nothing". *d* is ruled out because a deny rule beats every allow: "A matching deny rule wins over any allow rule, whatever the specificity."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario is Anthropic's. The questions here are written for this course.
