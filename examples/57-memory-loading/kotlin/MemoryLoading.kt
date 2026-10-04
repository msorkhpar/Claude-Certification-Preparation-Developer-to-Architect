import harness.Show.py

/**
 * Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.
 *
 * The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
 * launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
 * import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
 * Nothing here starts Claude Code: the "project" is a list of file paths and a map of file texts.
 */
const val MAX_IMPORT_HOPS = 4
val IMPORT = Regex("""(?<![\w`])@([\w./-]+)""")
private val CODE = Regex("""```.*?```|`[^`]*`""", RegexOption.DOT_MATCHES_ALL)
private val BRACES = Regex("""\{([^{}]*)\}""")

fun expandBraces(pattern: String): List<String> {
    val match = BRACES.find(pattern) ?: return listOf(pattern)
    return match.groupValues[1].split(",").flatMap { option -> expandBraces(pattern.substring(0, match.range.first) + option + pattern.substring(match.range.last + 1)) }
}

fun globRegex(pattern: String): Regex {
    val out = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        when {
            pattern.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
            pattern.startsWith("**", i) -> { out.append(".*"); i += 2 }
            pattern[i] == '*' -> { out.append("[^/]*"); i += 1 }
            pattern[i] == '?' -> { out.append("[^/]"); i += 1 }
            else -> { out.append(Regex.escape(pattern[i].toString())); i += 1 }
        }
    }
    return Regex(out.toString())
}

/** `*` stays inside one folder, a `**` folder segment crosses folders, braces expand: `*.md` is the project root only, any-folder-prefix plus `*.ts` is every folder. */
fun globMatch(pattern: String, path: String): Boolean = expandBraces(pattern).any { globRegex(it).matches(path) }

/** Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded. */
fun rulesLoaded(rules: Map<String, List<String>?>, touched: List<String>): List<String> =
    rules.filter { (_, paths) -> paths == null || paths.any { p -> touched.any { f -> globMatch(p, f) } } }.keys.toList()

private fun segments(path: String): List<String> = path.split("/").filter { it.isNotEmpty() }

/** Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each. */
fun launchFiles(tree: Set<String>, cwd: String = ""): List<String> {
    val parts = segments(cwd)
    val folders = listOf("") + parts.indices.map { parts.subList(0, it + 1).joinToString("/") }
    val found = mutableListOf<String>()
    for (folder in folders) {
        for (name in listOf("CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md")) {
            val path = "$folder/$name".trimStart('/')
            if (path in tree) found += path
        }
    }
    return found
}

/** Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first. */
fun onDemandFiles(tree: Set<String>, cwd: String, touched: List<String>): List<String> {
    val found = mutableListOf<String>()
    for (file in touched) {
        val parts = file.split("/").dropLast(1)
        for (i in segments(cwd).size + 1..parts.size) {
            for (name in listOf("CLAUDE.md", "CLAUDE.local.md")) {
                val path = (parts.take(i) + name).joinToString("/")
                if (path in tree && path !in found) found += path
            }
        }
    }
    return found
}

/** AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it. */
fun agentsMdRead(tree: Set<String>, cwd: String = ""): Boolean = launchFiles(tree, cwd).isEmpty() && "AGENTS.md" in tree

private fun target(file: String, ref: String): String = segments("${if ("/" in file) file.substringBeforeLast("/") else ""}/$ref").joinToString("/")

/** Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped. */
fun importsOf(path: String, texts: Map<String, String>, hops: Int = MAX_IMPORT_HOPS): List<String> {
    val found = mutableListOf<String>()
    fun visit(file: String, left: Int) {
        val text = CODE.replace(texts[file] ?: "", "")
        for (ref in IMPORT.findAll(text).map { it.groupValues[1] }) {
            val target = target(file, ref)
            if (target in texts && target !in found) {
                found += target
                if (left > 1) visit(target, left - 1)
            }
        }
    }
    visit(path, hops)
    return found
}

/** The @path references of a file that name no file: a typo here silently imports nothing. */
fun unresolvedImports(path: String, texts: Map<String, String>): List<String> {
    val text = CODE.replace(texts[path] ?: "", "")
    return IMPORT.findAll(text).map { it.groupValues[1] }.filter { target(path, it) !in texts }.toList()
}

/** Lines the files put into context. An import does not save any: the imported file loads at launch too. */
fun contextLines(paths: List<String>, texts: Map<String, String>): Int = paths.sumOf { texts.getValue(it).lines().size - (if (texts.getValue(it).endsWith("\n")) 1 else 0) }

fun main() {
    val tree = setOf("CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md")
    val rules = linkedMapOf("commit.md" to null, "testing.md" to listOf("**/*.test.{ts,tsx}"), "terraform.md" to listOf("terraform/**/*"))
    println("launch in web/: ${py(launchFiles(tree, "web"))}")
    println("reading web/ui/Button.tsx adds: ${py(onDemandFiles(tree, "web", listOf("web/ui/Button.tsx")))}")
    println("AGENTS.md read: ${py(agentsMdRead(tree))} - without any CLAUDE.md: ${py(agentsMdRead(setOf("AGENTS.md")))}")
    for (touched in listOf("web/ui/Button.test.tsx", "terraform/main.tf", "README.md")) println("touching $touched: ${py(rulesLoaded(rules, listOf(touched)))}")
    for ((pattern, path) in listOf("*.md" to "README.md", "*.md" to "docs/guide.md", "**/*.ts" to "a/b/c.ts", "src/**/*.{ts,tsx}" to "src/ui/x.tsx")) {
        println("$pattern matches $path: ${py(globMatch(pattern, path))}")
    }
    val texts = linkedMapOf(
        "CLAUDE.md" to "See @docs/a.md and `@not-an-import`", "docs/a.md" to "@b.md", "docs/b.md" to "@c.md", "docs/c.md" to "@d.md", "docs/d.md" to "@e.md", "docs/e.md" to "x",
    )
    println("imports: ${py(importsOf("CLAUDE.md", texts))}")
    println("unresolved: ${py(unresolvedImports("CLAUDE.md", texts + ("CLAUDE.md" to texts.getValue("CLAUDE.md") + " and @docs/typo.md")))}")
}
