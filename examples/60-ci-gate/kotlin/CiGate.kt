import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import harness.Show.py

private val log = System.getLogger("ci_gate")

/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network. The JSON is read with Jackson.
 */
val SEVERITIES = listOf("low", "medium", "high")
private val JSON = ObjectMapper()

fun json(text: String): JsonNode = JSON.readTree(text)

val REVIEW_SCHEMA: JsonNode = json(
    """
    {"type": "object",
     "properties": {"findings": {"type": "array", "items": {"type": "object", "properties": {
       "file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string", "enum": ["bug", "security", "style", "other"]},
       "severity": {"type": "string", "enum": ["low", "medium", "high"]}, "issue": {"type": "string"}, "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}},
       "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], "additionalProperties": false}}},
     "required": ["findings"], "additionalProperties": false}
    """,
)

/** What a team decides about review findings: the lowest severity to post, the categories to drop and the severities that fail the job. */
data class Policy(val minSeverity: String, val disabledCategories: List<String>, val failOn: List<String>)

/** One review comment to post. */
data class Comment(val file: String, val line: Int, val severity: String, val body: String)

/** The decision: the exit status, the comments to post and what went wrong. */
data class Decision(val exit: Int, val comments: List<Comment>, val problems: List<String>)

/** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
fun buildCommand(
    prompt: String,
    schema: JsonNode,
    maxTurns: Int = 8,
    tools: List<String> = listOf("Read", "Grep", "Glob", "Bash(git diff *)"),
    contextFile: String = "CLAUDE.md",
): List<String> = listOf(
    "claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", schema.toString(),
    "--max-turns", maxTurns.toString(), "--allowedTools", tools.joinToString(","),
)

/** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
fun lintCommand(raw: String): List<String> {
    val text = raw.replace(Regex("""\\\n\s*"""), " ")
    val found = mutableListOf<String>()
    if (!Regex("""\sclaude\b""").containsMatchIn(" $text")) return listOf("no-claude-command")
    if (!Regex("""\s(-p|--print)\b""").containsMatchIn(text)) found += "no-print"
    if (!Regex("""--output-format[ =]json\b""").containsMatchIn(text)) found += "no-json"
    if (!Regex("""--json-schema\b""").containsMatchIn(text)) found += "no-schema"
    val turns = Regex("""--max-turns[ =](\d+)""").find(text)
    if (turns == null || turns.groupValues[1].toInt() > 20) found += "no-turn-limit"
    val allowed = Regex("""--allowed[Tt]ools[ =]("[^"]*"|'[^']*'|\S+)""").find(text)
    val names = if (allowed != null) Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(allowed.groupValues[1].trim().trim('"', '\'')).map { it.value }.toList() else emptyList()
    if (allowed == null) found += "no-tool-list"
    else if (names.any { it in listOf("Bash", "Edit", "Write", "MultiEdit", "NotebookEdit") }) found += "wide-tools"
    if ("--bare" !in text) found += "no-bare"
    else if (!Regex("""--append-system-prompt(-file)?\b""").containsMatchIn(text)) found += "bare-without-context"
    return found
}

private fun isType(value: JsonNode, kind: String): Boolean = when (kind) {
    "object" -> value.isObject
    "array" -> value.isArray
    "string" -> value.isTextual
    "boolean" -> value.isBoolean
    "null" -> value.isNull
    "integer" -> value.isIntegralNumber
    "number" -> value.isNumber
    else -> throw IllegalArgumentException(kind)
}

/** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
fun schemaCheck(value: JsonNode, schema: JsonNode, path: String = "$"): List<String> {
    val want = schema.get("type")
    val wants = when {
        want == null -> emptyList()
        want.isArray -> want.map { it.asText() }
        else -> listOf(want.asText())
    }
    if (wants.isNotEmpty() && wants.none { isType(value, it) }) return listOf("$path: expected ${wants.joinToString(" or ")}")
    val errors = mutableListOf<String>()
    val enumeration = schema.get("enum")
    if (enumeration != null && enumeration.none { it == value }) errors += "$path: ${py(value)} is not one of ${py(enumeration)}"
    if (value.isObject) {
        schema.get("required")?.forEach { k -> if (!value.has(k.asText())) errors += "$path.${k.asText()}: is required" }
        val properties = schema.get("properties")
        val extra = schema.get("additionalProperties")
        if (extra != null && extra.isBoolean && !extra.asBoolean()) value.fieldNames().forEach { k -> if (properties == null || !properties.has(k)) errors += "$path.$k: is not allowed" }
        properties?.fields()?.forEach { (k, sub) -> if (value.has(k)) errors += schemaCheck(value.get(k), sub, "$path.$k") }
    }
    if (value.isArray && schema.has("items")) value.forEachIndexed { i, item -> errors += schemaCheck(item, schema.get("items"), "$path[$i]") }
    return errors
}

/** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
fun gate(stdout: String, exitCode: Int, schema: JsonNode, policy: Policy): Decision {
    val problems = mutableListOf<String>()
    if (exitCode != 0) problems += "claude exited with status $exitCode"
    val envelope = try { JSON.readTree(stdout) } catch (e: Exception) { null }
    if (envelope == null || !envelope.isObject) return Decision(1, emptyList(), problems + "the output is not a JSON object")
    val subtype = envelope.get("subtype")
    if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual || subtype.asText() != "success") {
        problems += "the run ended with ${if (subtype == null || subtype.isNull) "None" else subtype.asText()}"
    }
    val output = envelope.get("structured_output")
    if (output == null || output.isNull) problems += "the result has no structured_output" else problems += schemaCheck(output, schema).map { "schema $it" }
    if (problems.isNotEmpty()) return Decision(1, emptyList(), problems)
    val floor = SEVERITIES.indexOf(policy.minSeverity)
    val comments = output!!.get("findings")
        .filter { it.get("category").asText() !in policy.disabledCategories && SEVERITIES.indexOf(it.get("severity").asText()) >= floor }
        .map { Comment(it.get("file").asText(), it.get("line").asInt(), it.get("severity").asText(), "${it.get("issue").asText()} Suggested fix: ${it.get("suggested_fix").asText()}") }
    val blocked = comments.any { it.severity in policy.failOn }
    return Decision(if (blocked) 1 else 0, comments, emptyList())
}

fun envelope(over: Map<String, Any?> = emptyMap()): String =
    JSON.writeValueAsString(linkedMapOf<String, Any?>("type" to "result", "subtype" to "success", "is_error" to false, "result" to "done", "num_turns" to 3, "total_cost_usd" to 0.05, "session_id" to "s-1") + over)

/** Python's shlex.quote for one word. */
fun quote(s: String): String = if (s.isEmpty()) "''" else if (Regex("""[\w@%+=:,./-]+""").matches(s)) s else "'" + s.replace("'", "'\"'\"'") + "'"

fun join(words: List<String>): String = words.joinToString(" ") { quote(it) }

fun finding(file: String = "api.py", line: Int = 12, category: String = "bug", severity: String = "medium", issue: String = "Unchecked None.", fix: String = "Return early.", pattern: String = "missing-none-check"): MutableMap<String, Any?> =
    linkedMapOf("file" to file, "line" to line, "category" to category, "severity" to severity, "issue" to issue, "suggested_fix" to fix, "detected_pattern" to pattern)

fun main() {
    val command = buildCommand("Review the diff on standard input.", json("""{"type": "object", "properties": {"findings": {"type": "array"}}}"""), tools = listOf("Read", "Grep"))
    println("command: ${command.take(5).joinToString(" ")} ... ${py(command.takeLast(4))}")
    println("lint, the good step: ${py(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'"))}")
    println("lint, a careless step: ${py(lintCommand("claude 'Review the change' --allowedTools Bash,Edit"))}")
    val policy = Policy("medium", listOf("style"), listOf("high"))
    val findings = listOf(
        finding(severity = "high", issue = "Unchecked None.", fix = "Return early.", pattern = "missing-none-check"),
        finding(line = 40, category = "style", severity = "high", issue = "Long line.", fix = "Wrap it.", pattern = "line-length"),
        finding(file = "ui.py", line = 3, severity = "low", issue = "Odd name.", fix = "Rename.", pattern = "naming"),
    )
    val runs = linkedMapOf(
        "a finding that blocks" to (envelope(mapOf("structured_output" to mapOf("findings" to findings))) to 0),
        "no findings" to (envelope(mapOf("structured_output" to mapOf("findings" to emptyList<Any>()))) to 0),
        "turn limit" to (envelope(mapOf("subtype" to "error_max_turns", "is_error" to true)) to 1),
        "success without output" to (envelope() to 0),
        "not JSON" to ("Error: no key" to 1),
        "wrong shape" to (envelope(mapOf("structured_output" to mapOf("findings" to listOf(mapOf("file" to "a.py"))))) to 0),
    )
    for ((label, run) in runs) {
        val d = gate(run.first, run.second, REVIEW_SCHEMA, policy)
        println("$label: exit ${d.exit}, ${d.comments.size} comment(s)${if (d.problems.isNotEmpty()) ", " + d.problems[0] else ""}")
    }
}
