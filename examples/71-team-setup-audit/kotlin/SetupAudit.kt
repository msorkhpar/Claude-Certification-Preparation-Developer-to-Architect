import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.
 *
 * The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
 * at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()

/** A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character. */
fun globRegex(glob: String): Regex {
    val out = StringBuilder()
    var i = 0
    while (i < glob.length) {
        when {
            glob.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
            glob.startsWith("**", i) -> { out.append(".*"); i += 2 }
            glob[i] == '*' -> { out.append("[^/]*"); i += 1 }
            glob[i] == '?' -> { out.append("[^/]"); i += 1 }
            else -> { out.append(Regex.escape(glob[i].toString())); i += 1 }
        }
    }
    return Regex(out.toString())
}

/** The paths list of a rule file's front matter, or null when it has none (the rule then loads at launch). */
fun rulePaths(text: String): List<String>? {
    val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(text) ?: return null
    val listed = Regex("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", RegexOption.MULTILINE).find(head.groupValues[1] + "\n") ?: return null
    return listed.groupValues[1].split("\n").filter { it.isNotBlank() }.map { it.replaceFirst(Regex("^\\s*-\\s+"), "").trim().replace(Regex("^[\"']|[\"']$"), "") }
}

class Project(val files: List<String>, val memory: String, val rules: Map<String, List<String>?>, val commands: List<String>, val settings: JsonNode)

private fun list(dir: Path, suffix: String): List<String> =
    if (!Files.isDirectory(dir)) listOf() else Files.list(dir).use { s -> s.map { it.fileName.toString() }.filter { it.endsWith(suffix) }.sorted().toList() }

fun load(root: Path): Project {
    val rules = linkedMapOf<String, List<String>?>()
    for (name in list(root.resolve(".claude/rules"), ".md")) rules[name] = rulePaths(Files.readString(root.resolve(".claude/rules").resolve(name)))
    return Project(Files.readString(root.resolve("files.txt")).trim().split(Regex("\\s+")), Files.readString(root.resolve("CLAUDE.md")), rules,
        list(root.resolve(".claude/commands"), ".md"), ObjectMapper().readTree(Files.readString(root.resolve(".claude/settings.json"))))
}

private fun matches(paths: List<String>?, file: String) = paths != null && paths.any { globRegex(it).matches(file) }

fun rulesFor(rules: Map<String, List<String>?>, file: String): List<String> = rules.filter { (_, paths) -> paths == null || matches(paths, file) }.keys.toList()

private fun has(array: JsonNode?, value: String) = array != null && array.any { it.asText() == value }

fun audit(root: Path): List<String> {
    val p = load(root)
    val found = mutableListOf<String>()
    val sections = Regex("^## ", RegexOption.MULTILINE).findAll(p.memory).count()
    if (p.rules.isEmpty() && sections >= 3) found += "all-in-root: $sections sections in CLAUDE.md and no rule files"
    for ((name, paths) in p.rules) {
        if (paths == null) found += "rule-loads-always: $name"
        else if (p.files.none { matches(paths, it) }) found += "rule-matches-nothing: $name"
    }
    for (f in p.files) if (Regex(".*\\.test\\.tsx?").matches(f) && p.rules.values.none { matches(it, f) }) found += "test-uncovered: $f"
    if (p.commands.isEmpty()) found += "no-shared-command"
    val perms = p.settings.path("permissions")
    if (!has(perms.get("deny"), "Read(./.env)")) found += "env-readable"
    if (has(perms.get("allow"), "Bash")) found += "bare-bash-allowed"
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val root = HERE.resolve(name)
        val p = load(root)
        println("$name: CLAUDE.md ${p.memory.trimEnd().split("\n").size} lines, ${p.rules.size} rule files, ${p.commands.size} shared commands")
        val found = audit(root)
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for (f in p.files) println("  $f <- ${rulesFor(p.rules, f).joinToString(", ").ifEmpty { "CLAUDE.md only" }}")
        }
    }
}
