import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import harness.Show.py

private val log = System.getLogger("skill_model")

/**
 * What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.
 *
 * The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
 * `/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
 * indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work:
 * the front matter is YAML, read with Jackson.
 */
val LEVELS = listOf("enterprise", "personal", "project") // the order in which a skill name is resolved: the first level wins
private val YAML = ObjectMapper(YAMLFactory())
private val FRONTMATTER = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL)

typealias Meta = Map<String, Any?>

/** The front matter of a command or skill file, and its body. */
data class Parsed(val meta: Meta, val body: String)

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

/** (front matter mapping, body) of a command or skill file. */
@Suppress("UNCHECKED_CAST")
fun parse(text: String): Parsed {
    val m = FRONTMATTER.matchEntire(text) ?: return Parsed(emptyMap(), text)
    val meta = YAML.readValue(m.groupValues[1], LinkedHashMap::class.java) as Meta?
    return Parsed(meta ?: emptyMap(), m.groupValues[2])
}

/** The slash command a file creates: a skill's `name`, else its folder; a command's file name. */
fun commandName(path: String, meta: Meta): String {
    val parts = path.split("/")
    if (parts.last() == "SKILL.md") return meta["name"]?.toString()?.takeIf { it.isNotEmpty() } ?: parts[parts.size - 2]
    return parts.last().dropLast(3)
}

/** Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}. */
fun winner(candidates: Map<String, String>): String? = LEVELS.firstOrNull { it in candidates }?.let { candidates[it] }

/** Who can start it, and whether its description is always in context. */
fun invocation(meta: Meta): Map<String, Boolean> {
    val manual = meta["disable-model-invocation"] == true
    val hidden = meta["user-invocable"] == false
    return linkedMapOf("you" to !hidden, "claude" to !manual, "description_in_context" to !manual)
}

private fun names(value: Any?): List<String> = when (value) {
    is String -> Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(value).map { it.value }.toList()
    is List<*> -> value.map { it.toString() }
    else -> emptyList()
}

/** True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command. */
fun preApproved(meta: Meta, tool: String, command: String = ""): Boolean {
    for (item in names(meta["allowed-tools"])) {
        val name = item.substringBefore("(")
        if (name != tool) continue
        val pattern = if ("(" in item) item.substringAfter("(").trimEnd(')') else ""
        if (pattern.isEmpty() || (pattern.endsWith(" *") && (command == pattern.dropLast(2) || command.startsWith(pattern.dropLast(1)))) || pattern == command) return true
    }
    return false
}

/** True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/...)` leaves the tool in place. */
fun removed(meta: Meta, tool: String): Boolean = tool in names(meta["disallowed-tools"]).filter { "(" !in it }

fun toolStatus(meta: Meta, tool: String, command: String = ""): String = when {
    removed(meta, tool) -> "removed"
    preApproved(meta, tool, command) -> "pre-approved"
    else -> "permission settings decide"
}

/** Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input. */
fun render(body: String, raw: String, names: List<String> = emptyList()): String {
    val args = if (raw.isEmpty()) emptyList() else shlexSplit(raw)
    val named = names.zip(args).toMap()
    val alternatives = """ARGUMENTS\[\d+\]|ARGUMENTS|\d+""" + names.joinToString("") { "|" + Regex.escape(it) }
    val indexed = Regex("""ARGUMENTS\[(\d+)\]|(\d+)""")
    var used = false
    var out = Regex("""\$($alternatives)""").replace(body) { m ->
        used = true
        val token = m.groupValues[1]
        if (token == "ARGUMENTS") raw else {
            val index = indexed.matchEntire(token)
            if (index != null) {
                val i = (index.groups[1]?.value ?: index.groups[2]!!.value).toInt()
                if (i < args.size) args[i] else m.value // an indexed placeholder with no argument stays as written
            } else named[token] ?: "" // a named placeholder with no argument is empty
        }
    }
    if (raw.isNotEmpty() && !used) out = out.trimEnd('\n') + "\nARGUMENTS: $raw\n"
    return out
}

/** The subagent a forked skill runs in, or null when it runs in the conversation. The subagent does not see the conversation. */
fun forkAgent(meta: Meta): String? = if (meta["context"] != "fork") null else meta["agent"]?.toString()?.takeIf { it.isNotEmpty() } ?: "general-purpose"

val SKILL = """---
name: release-tag
description: Tag a release and push the tag. Use when the user asks to cut a release.
disable-model-invocation: true
argument-hint: "[version]"
arguments: [version]
allowed-tools: Bash(git tag *) Bash(git push origin *)
disallowed-tools: Edit
---
Create the tag ${'$'}version and push it.
"""

fun main() {
    val skill = parse(SKILL)
    println("name: ${commandName(".claude/skills/release-tag/SKILL.md", skill.meta)} | legacy file: ${commandName(".claude/commands/standup.md", emptyMap())}")
    println("winner of three same-name skills: ${winner(mapOf("project" to "p/SKILL.md", "personal" to "u/SKILL.md"))}")
    println("who can start it: ${py(invocation(skill.meta))}")
    for ((tool, command) in listOf("Bash" to "git tag v1.2.0", "Bash" to "git push --force", "Bash" to "rm -rf build", "Edit" to "", "Read" to "")) {
        println("$tool ${py(command)}: ${toolStatus(skill.meta, tool, command)}")
    }
    println("bare Bash allowed: ${py(preApproved(mapOf("allowed-tools" to "Bash"), "Bash", "rm -rf build"))} | scoped disallow removes Edit: ${py(removed(mapOf("disallowed-tools" to "Edit(src/**)"), "Edit"))}")
    println("render: ${render(skill.body, "v1.2.0", listOf("version")).trim()}")
    println("render, no placeholder: ${py(render("Review the change.\n", "123"))}")
    println("quoted: ${render("first=\$0 second=\$1", "\"hello world\" second")}")
    println("fork: ${forkAgent(mapOf("context" to "fork")) ?: "None"} ${forkAgent(mapOf("context" to "fork", "agent" to "Explore")) ?: "None"} ${forkAgent(emptyMap()) ?: "None"}")
}
