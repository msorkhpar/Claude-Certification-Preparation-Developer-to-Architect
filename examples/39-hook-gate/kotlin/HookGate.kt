import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlin.system.exitProcess

/**
 * A PreToolUse hook that blocks destructive commands, and a linter for skill and subagent files.
 *
 * Claude Code starts a hook as a process, writes one JSON event to its standard input and reads the answer from its exit code, its
 * standard output and its standard error. Exit code 2 blocks the call and the standard error is the reason; exit code 0 with a JSON
 * `permissionDecision` answers in a structured way; exit code 0 with nothing printed gives no opinion. This file is such a hook (run it
 * with --hook) and a demonstration (run it plain). The event and output shapes are those of the Claude Code hooks reference, read on
 * 2026-10-03. The command check normalises what a prefix rule such as `Bash(git push *)` would miss: another form of the same command.
 * The front matter of the skill and subagent files is YAML, read with Jackson.
 */
val PROTECTED = listOf(".env", "package-lock.json", ".git/")
private val JSON = ObjectMapper()
private val YAML = ObjectMapper(YAMLFactory())

/** What the hook process answers: the exit code, standard output and standard error. */
data class Answer(val code: Int, val out: String, val err: String)

/** The words of a command line as a POSIX shell would split them (quotes and backslashes); a bad quote is an error. */
fun shlexSplit(s: String): List<String> {
    val words = mutableListOf<String>()
    var word: StringBuilder? = null
    var i = 0
    while (i < s.length) {
        val c = s[i++]
        if (c.isWhitespace()) {
            word?.let { words += it.toString() }
            word = null
            continue
        }
        val w = word ?: StringBuilder().also { word = it }
        when (c) {
            '\\' -> {
                require(i < s.length) { "No escaped character" }
                w.append(s[i++])
            }
            '\'' -> {
                val end = s.indexOf('\'', i)
                require(end >= 0) { "No closing quotation" }
                w.append(s, i, end)
                i = end + 1
            }
            '"' -> while (true) {
                require(i < s.length) { "No closing quotation" }
                var d = s[i++]
                if (d == '"') break
                if (d == '\\' && i < s.length && s[i] in "\"\\$`\n") d = s[i++]
                w.append(d)
            }
            else -> w.append(c)
        }
    }
    word?.let { words += it.toString() }
    return words
}

private fun gitSubcommand(args: List<String>): String? {
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (a in listOf("-C", "-c", "--git-dir", "--work-tree")) i += 2 else if (a.startsWith("-")) i += 1 else return a
    }
    return null
}

/** The reason a command is refused, or null. Looks through compound commands, `sh -c`, env assignments, paths and git options. */
fun dangerous(command: String): String? {
    for (part in command.split(Regex("&&|\\|\\||;|\\||&|\n"))) {
        val split = try {
            shlexSplit(part)
        } catch (e: IllegalArgumentException) {
            return "a command that cannot be parsed is not run unreviewed"
        }
        val words = split.dropWhile { Regex("""\w+=\S*""").matches(it) }
        if (words.isEmpty()) continue
        val program = words[0].substringAfterLast('/')
        val args = words.drop(1)
        val dashC = args.indexOf("-c")
        if (program in listOf("sh", "bash", "zsh") && dashC >= 0 && dashC + 1 < args.size) {
            dangerous(args[dashC + 1])?.let { return it }
        }
        if (program == "git" && gitSubcommand(args) == "push") return "nothing is pushed from an agent"
        val short = args.filter { it.startsWith("-") && !it.startsWith("--") }.joinToString("") { it.drop(1) }
        val recursive = "r" in short || "R" in short || "--recursive" in args
        val force = "f" in short || "--force" in args
        if (program == "rm" && recursive && force) return "a recursive forced delete is not run from an agent"
    }
    return null
}

/** The answer for one PreToolUse event. */
fun preToolUse(event: Map<String, Any?>): Answer {
    val tool = event["tool_name"] as String
    @Suppress("UNCHECKED_CAST") val toolInput = (event["tool_input"] ?: emptyMap<String, Any?>()) as Map<String, Any?>
    if (tool == "Bash") {
        dangerous(toolInput["command"] as String? ?: "")?.let { reason ->
            val specific = linkedMapOf("hookEventName" to "PreToolUse", "permissionDecision" to "deny", "permissionDecisionReason" to reason)
            return Answer(0, JSON.writeValueAsString(mapOf("hookSpecificOutput" to specific)), "")
        }
    }
    if (tool in listOf("Edit", "Write", "MultiEdit")) {
        val path = (toolInput["file_path"] as String? ?: "").replace("\\", "/")
        for (pattern in PROTECTED) if (pattern in path) return Answer(2, "", "Blocked: $path matches protected pattern '$pattern'")
    }
    return Answer(0, "", "")
}

// --- linting the files that configure Claude Code -----------------------------------------------------------------------------

private val SIDE_EFFECTS = Regex("""\b(deploy|publish|delete|commit|push|merge)\b""", RegexOption.IGNORE_CASE)

/** The front matter of a Markdown file that starts with a --- block; an empty map when it has none. */
@Suppress("UNCHECKED_CAST")
fun frontmatter(text: String): Map<String, Any?> {
    val m = Regex("^---\\n(.*?)\\n---\\n?(.*)$", RegexOption.DOT_MATCHES_ALL).matchEntire(text) ?: return emptyMap()
    return YAML.readValue(m.groupValues[1], LinkedHashMap::class.java) as Map<String, Any?>? ?: emptyMap()
}

private fun text(o: Any?) = o?.toString() ?: ""

fun lintSkill(text: String): List<String> {
    val fm = frontmatter(text)
    val findings = mutableListOf<String>()
    val desc = text(fm["description"])
    if (desc.isEmpty()) findings += "description is missing: Claude uses it to decide when to load the skill"
    if (desc.length + text(fm["when_to_use"]).length > 1536) findings += "description and when_to_use together exceed 1,536 characters and are cut"
    if (SIDE_EFFECTS.containsMatchIn("${text(fm["name"])} $desc") && fm["disable-model-invocation"] != true) {
        findings += "a skill with side effects should set disable-model-invocation: true"
    }
    val allowed = text(fm["allowed-tools"])
    if (allowed.isNotEmpty() && "Bash" in allowed.replace(",", " ").trim().split(Regex("\\s+"))) {
        findings += "allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead"
    }
    return findings
}

fun lintAgent(text: String): List<String> {
    val fm = frontmatter(text)
    val findings = listOf("name", "description").filter { text(fm[it]).isEmpty() }.map { "$it is required" }.toMutableList()
    if ("tools" !in fm) findings += "tools is omitted: the subagent inherits every tool"
    if (fm["memory"] != null && fm["memory"] !in listOf("user", "project", "local")) findings += "memory must be user, project or local"
    if (fm["permissionMode"] == "bypassPermissions") findings += "permissionMode bypassPermissions skips every prompt in this subagent"
    return findings
}

const val SKILL = "---\nname: deploy\ndescription: Deploy the service to production\nallowed-tools: Bash\n---\nRun the release script.\n"
const val AGENT = "---\nname: reviewer\ndescription: Reviews a diff for bugs\nmemory: team\n---\nYou review code.\n"

/** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
fun py(v: Any?): String {
    if (v is List<*>) return v.joinToString(", ", "[", "]") { py(it) }
    val s = v.toString()
    val q = if ("'" in s && "\"" !in s) "\"" else "'"
    return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q
}

fun demo() {
    val events = listOf(
        "Bash" to ("command" to "git push origin main"), "Bash" to ("command" to "git -C . push origin main"), "Bash" to ("command" to "bash -c 'rm -rf build'"),
        "Bash" to ("command" to "ls && git status"), "Bash" to ("command" to "rm build/old.txt"), "Edit" to ("file_path" to "/work/app/.env"),
        "Edit" to ("file_path" to "/work/app/main.py"),
    )
    for ((tool, input) in events) {
        val (code, out, err) = preToolUse(mapOf("hook_event_name" to "PreToolUse", "tool_name" to tool, "tool_input" to mapOf(input)))
        val shown = when {
            out.isNotEmpty() -> "deny (exit 0, JSON): " + (JSON.readTree(out)["hookSpecificOutput"]["permissionDecisionReason"].asText())
            code == 2 -> "block (exit 2): $err"
            else -> "no decision (exit 0)"
        }
        println("$tool ${py(input.second)} -> $shown")
    }
    println("skill findings: ${py(lintSkill(SKILL))}")
    println("agent findings: ${py(lintAgent(AGENT))}")
}

fun main(args: Array<String>) {
    if ("--hook" in args) {
        @Suppress("UNCHECKED_CAST")
        val (code, out, err) = preToolUse(JSON.readValue(System.`in`, LinkedHashMap::class.java) as Map<String, Any?>)
        print(out)
        System.err.print(err)
        System.out.flush()
        System.err.flush()
        exitProcess(code)
    }
    demo()
}
