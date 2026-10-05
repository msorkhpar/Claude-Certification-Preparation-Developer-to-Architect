import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PersonalSetupTest {
    // config.dir is the folder that holds home/ (the user's ~) and project/ (a repository): starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val json = ObjectMapper()
    private val home = "home/.claude"
    private val project = "project/.claude"
    private val builtinStyles = setOf("Proactive", "Concise", "Explanatory", "Learning")
    private val styleFields = setOf("name", "description", "keep-coding-instructions", "force-for-plugin")
    private val contexts = setOf("Global", "Chat", "Autocomplete", "Settings", "Confirmation", "Tabs", "Help", "Transcript", "HistorySearch", "Task", "ThemePicker",
        "Attachments", "Footer", "MessageSelector", "DiffDialog", "DiffPanel", "ModelPicker", "EffortSlider", "Select", "Plugin", "Pane", "PaneField", "Agents", "Scroll")
    private val modifiers = mapOf("ctrl" to "ctrl", "control" to "ctrl", "shift" to "shift", "alt" to "alt", "opt" to "alt", "option" to "alt", "meta" to "alt",
        "cmd" to "cmd", "command" to "cmd", "super" to "cmd", "win" to "cmd")
    private val namedKeys = setOf("escape", "esc", "enter", "return", "tab", "space", "up", "down", "left", "right", "pageup", "pagedown", "home", "end",
        "backspace", "delete", "wheelup", "wheeldown")
    private val reserved = setOf("ctrl+c", "ctrl+d", "ctrl+m", "ctrl+[", "ctrl+i", "ctrl+h")
    private val action = Regex("^[a-z][A-Za-z]*:[A-Za-z]+$")
    private val statusFields = setOf("model.id", "model.display_name", "cwd", "workspace.current_dir", "workspace.project_dir", "workspace.added_dirs",
        "workspace.git_worktree", "cost.total_cost_usd", "cost.total_duration_ms", "cost.total_api_duration_ms", "cost.total_lines_added", "cost.total_lines_removed",
        "context_window.total_input_tokens", "context_window.total_output_tokens", "context_window.context_window_size", "context_window.used_percentage",
        "context_window.remaining_percentage", "exceeds_200k_tokens", "fast_mode", "effort.level", "thinking.enabled", "rate_limits.five_hour.used_percentage",
        "rate_limits.seven_day.used_percentage", "session_id", "session_name", "transcript_path", "version", "output_style.name", "vim.mode", "agent.name", "pr.number", "pr.url",
        "pr.review_state", "worktree.name", "worktree.path", "worktree.branch")
    private val personal = Regex("/home/|/Users/|C:\\\\Users|[\\w.+-]+@[\\w-]+\\.[\\w.-]+|sk-ant|TODO|FIXME")

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun readJson(rel: String): JsonNode {
        val data = try {
            json.readTree(read(rel))
        } catch (error: java.io.IOException) {
            throw AssertionError("$rel is not valid JSON: ${error.message}")
        }
        assertTrue(data.isObject, "$rel must hold an object")
        return data
    }

    /** The front matter fields of a Markdown file (key: value lines) and its body. */
    private fun front(rel: String): Pair<Map<String, String>, String> {
        val m = Regex("^---\\n(.*?)\\n---\\n?(.*)$", RegexOption.DOT_MATCHES_ALL).matchEntire(read(rel))
        assertNotNull(m, "$rel has no front matter")
        val fields = LinkedHashMap<String, String>()
        for (line in m!!.groupValues[1].split("\n")) {
            val at = line.indexOf(':')
            if (at > 0 && line.substring(0, at).isNotBlank()) fields[line.substring(0, at).trim()] = line.substring(at + 1).trim()
        }
        return fields to m.groupValues[2]
    }

    private fun styleFiles(): List<String> {
        val folder = root.resolve(home).resolve("output-styles")
        if (!Files.isDirectory(folder)) return emptyList()
        return Files.list(folder).use { s -> s.map { it.fileName.toString() }.filter { it.endsWith(".md") }.sorted().toList() }
    }

    private fun customStyles(): Set<String> = styleFiles().map { file ->
        val name = front("$home/output-styles/$file").first["name"]
        if (!name.isNullOrEmpty()) name else file.removeSuffix(".md")
    }.toSet()

    private fun knownStyle(name: JsonNode) = name.isTextual && name.asText().isNotEmpty() && (name.asText() in builtinStyles || name.asText() in customStyles())

    private fun keyProblem(keystroke: String): String? {
        for (part in keystroke.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }) {
            val pieces = part.split("+")
            val key = pieces.last().lowercase()
            if (!(key.length == 1 || key in namedKeys)) return "$part: unknown key $key"
            for (mod in pieces.dropLast(1)) if (mod.lowercase() !in modifiers) return "$part: unknown modifier $mod"
        }
        return null
    }

    private fun normal(keystroke: String): String {
        val parts = keystroke.lowercase().split("+")
        return (parts.dropLast(1).map { modifiers[it] ?: it } + parts.last()).joinToString("+")
    }

    @Test
    fun m1_theUserLevelHoldsAStyleAStatusLineAndAKeybindingsFileThatFitTogether() {
        val settings = readJson("$home/settings.json")
        assertTrue(settings.path("outputStyle").isTextual && settings.path("outputStyle").asText().isNotEmpty(), "the user settings select an output style")
        assertTrue(knownStyle(settings.path("outputStyle")), "outputStyle ${settings.path("outputStyle").asText()} names no built-in style and no style file")
        val line = settings.path("statusLine")
        assertTrue(line.isObject && line.path("type").asText(null) == "command", "statusLine is an object of type command")
        val command = line.path("command").asText("")
        assertTrue(command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder")
        assertTrue(Files.isRegularFile(root.resolve("home/.claude").resolve(command.removePrefix("~/.claude/"))), "$command is not a file")
        val bindings = readJson("$home/keybindings.json").path("bindings")
        assertTrue(bindings.isArray && bindings.size() > 0, "keybindings.json holds a non-empty bindings array")
        for (b in bindings) assertTrue(b.isObject && b.path("context").isTextual && b.path("bindings").isObject, "each block has a context and a bindings object")
    }

    @Test
    fun e1_theStyleFileKeepsTheCodingInstructionsAndCarriesOnlyDocumentedFields() {
        val files = styleFiles()
        assertFalse(files.isEmpty(), "an output style file is needed")
        for (file in files) {
            val (fields, body) = front("$home/output-styles/$file")
            val unknown = (fields.keys - styleFields).sorted()
            assertTrue(unknown.isEmpty(), "$file: unknown front matter fields $unknown (a misspelled field is ignored without an error)")
            assertFalse(fields["name"].isNullOrEmpty(), "$file: name the style")
            assertFalse(fields["description"].isNullOrEmpty(), "$file: describe the style")
            assertEquals("true", fields["keep-coding-instructions"], "$file: a style that only changes communication keeps the coding instructions")
            assertTrue(body.lines().count { it.isNotBlank() } >= 3, "$file: the instructions are too thin")
        }
    }

    @Test
    fun e2_theStatusLineReadsOnlyFieldsTheSessionSendsAndRefreshesNoFasterThanEverySecond() {
        val line = readJson("$home/settings.json").path("statusLine")
        assertTrue(line.isObject, "statusLine is an object")
        if (line.has("padding")) assertTrue(line.get("padding").isIntegralNumber && line.get("padding").asInt() >= 0, "padding is a number of characters, 0 or more")
        if (line.has("refreshInterval")) assertTrue(line.get("refreshInterval").isIntegralNumber && line.get("refreshInterval").asInt() >= 1, "refreshInterval is in seconds, 1 or more")
        val command = line.path("command").asText("")
        assertTrue(command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder")
        val script = read("$home/${command.removePrefix("~/.claude/")}")
        assertTrue(script.startsWith("#!/bin/sh") || script.startsWith("#!/usr/bin/env bash") || script.startsWith("#!/bin/bash"), "the script starts with a shebang line")
        val used = sortedSetOf<String>()
        for (expr in Regex("jq -r '([^']*)'").findAll(script)) for (f in Regex("(?<![\\w$\"])\\.([A-Za-z_][\\w.]*)").findAll(expr.groupValues[1])) used.add(f.groupValues[1])
        assertFalse(used.isEmpty(), "the script reads at least one field of the session JSON with jq")
        val unknown = used - statusFields
        assertTrue(unknown.isEmpty(), "fields the session does not send: $unknown")
    }

    @Test
    fun e3_theKeybindingsUseRealContextsAndActionsFreeKeysAndANullToUnbind() {
        val blocks = readJson("$home/keybindings.json").path("bindings")
        assertTrue(blocks.isArray && blocks.size() > 0, "the file holds a bindings array")
        var rebound = 0
        var unbound = 0
        for (block in blocks) {
            assertTrue(block.path("context").asText("") in contexts, "${block.path("context").asText()} is not a context (names are case sensitive)")
            for (keystroke in block.path("bindings").fieldNames().asSequence().toList()) {
                val problem = keyProblem(keystroke)
                assertNull(problem, problem.toString())
                assertFalse(normal(keystroke) in reserved, "$keystroke is reserved and cannot be rebound")
                val act = block.path("bindings").get(keystroke)
                if (act.isNull) unbound++
                else {
                    assertTrue(act.isTextual && action.matches(act.asText()), "$act is not a namespace:action name")
                    rebound++
                }
            }
        }
        assertTrue(rebound >= 1 && unbound >= 1, "rebind one key to an action and unbind another with null")
    }

    @Test
    fun e4_personalSettingsStayInTheUserFilesAndTheProjectFileStaysShared() {
        val shared = readJson("$project/settings.json")
        for (key in listOf("outputStyle", "statusLine")) assertFalse(shared.has(key), "$key is personal: a shared project file would override every teammate's own choice")
        val allow = shared.path("permissions").path("allow")
        assertTrue(allow.isArray && allow.size() > 0, "the shared file carries the team's permission rules")
        val local = readJson("$project/settings.local.json")
        assertTrue(knownStyle(local.path("outputStyle")), "the local file overrides the style for this project with an exact style name")
    }

    @Test
    fun e5_theLocalSettingsFileIsKeptOutOfGit() {
        val lines = read("project/.gitignore").lines().map { it.trim() }
        assertTrue(".claude/settings.local.json" in lines || "**/.claude/settings.local.json" in lines, "list the local settings file in the ignore file")
    }

    @Test
    fun e6_noFileHoldsAPersonalPathAnAddressAKeyOrAnUnfinishedMarker() {
        val files = Files.walk(root).use { s -> s.filter { Files.isRegularFile(it) }.toList() }
        assertFalse(files.isEmpty(), "there are no files")
        for (path in files) {
            val m = personal.find(Files.readString(path))
            assertNull(m, "${root.relativize(path)} holds ${m?.value}")
        }
    }
}
