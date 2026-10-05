import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class MarketplaceSetupTest {
    // config.dir is the marketplace folder (it holds .claude-plugin/marketplace.json): starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val mapper = ObjectMapper()
    private val plugin = "plugins/standards-kit"
    private val id = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
    private val semver = Regex("""\d+\.\d+\.\d+""")
    private val sha = Regex("[0-9a-f]{40}")
    private val tag = Regex("""v\d+\.\d+\.\d+""")
    private val repo = Regex("""[\w.-]+/[\w.-]+""")
    private val reserved = setOf(
        "claude-code-marketplace", "claude-code-plugins", "claude-plugins-official", "anthropic-marketplace", "anthropic-plugins", "agent-skills",
        "anthropic-agent-skills", "life-sciences", "knowledge-work-plugins", "claude-for-legal", "claude-for-financial-services",
        "financial-services-plugins", "first-party-plugins", "claude-tag-plugins", "claude-community", "claude-plugins-community", "healthcare",
        "anthropic-plugin-directory", "claude-plugin-directory", "inline", "builtin", "skills-dir", "synced", "claude-plugin-test", "npm", "pip", "uv", "cargo", "github", "gh",
    )
    private val sourceTypes = setOf("github", "url", "git-subdir", "npm", "archive", "command")
    private val rootVar = "\${CLAUDE_PLUGIN_ROOT}"

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun readJson(rel: String): JsonNode = try {
        mapper.readTree(read(rel))
    } catch (error: IOException) {
        throw AssertionError("$rel is not valid JSON: ${error.message}")
    }

    private fun market(): JsonNode = readJson(".claude-plugin/marketplace.json").also { assertTrue(it.isObject, "marketplace.json must hold an object") }

    private fun entries(): List<JsonNode> {
        val plugins = market().path("plugins")
        assertTrue(plugins.isArray && plugins.size() > 0, "plugins must be a non-empty list")
        return plugins.toList().onEach { assertTrue(it.isObject, "every plugin entry is an object") }
    }

    private fun entry(name: String): JsonNode = entries().firstOrNull { it.path("name").asText(null) == name } ?: throw AssertionError("no entry named $name")

    private fun manifest(): JsonNode = readJson("$plugin/.claude-plugin/plugin.json").also { assertTrue(it.isObject, "plugin.json must hold an object") }

    private fun names(node: JsonNode): List<String> = node.fieldNames().asSequence().toList()

    /** The front matter fields of a Markdown file (key: value lines) and its body. */
    private fun front(rel: String): Pair<Map<String, String>, String> {
        val m = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(read(rel))
        assertNotNull(m, "$rel has no front matter")
        val fields = m!!.groupValues[1].lines().filter { ':' in it }.associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }
        return fields to m.groupValues[2]
    }

    private fun mentionsAnthropic(low: String) = "claude" in low || "anthropic" in low

    @Test
    fun m1_theMarketplaceFileNamesAnOwnerAndListsEachPluginByASourceThatResolves() {
        val data = market()
        assertTrue(data.path("name").isTextual && data.path("name").asText().isNotBlank(), "a marketplace needs a name")
        assertTrue(data.path("owner").isObject && data.path("owner").path("name").asText("").isNotBlank(), "the owner needs a name")
        assertTrue(data.path("description").asText("").isNotBlank(), "describe the marketplace")
        val names = entries().map { p ->
            assertTrue(p.path("name").isTextual && p.path("name").asText().isNotEmpty(), "every entry needs a name")
            p.path("name").asText()
        }
        assertEquals(names.size, names.toSet().size, "entry names are unique")
        for (p in entries()) {
            val name = p.path("name").asText()
            assertTrue(p.has("source"), "$name needs a source")
            val source = p.get("source")
            if (source.isTextual) {
                val s = source.asText()
                assertTrue(s.startsWith("./") && ".." !in s && "\\" !in s, "$name: a relative path starts with ./ and stays inside the marketplace")
                assertTrue(Files.isDirectory(root.resolve(s)), "$name: $s is not a folder of the marketplace")
                val inner = mapper.readTree(Files.readString(root.resolve(s).resolve(".claude-plugin/plugin.json")))
                assertEquals(name, inner.path("name").asText(null), "$name: the entry name and the manifest name must be the same")
            } else {
                assertTrue(source.isObject && source.path("source").asText("") in sourceTypes, "$name: a source object names a type")
            }
        }
    }

    @Test
    fun e1_namesAreValidInAPluginIdAndAreNotReservedOrMistakenForOfficialOnes() {
        val name = market().path("name").asText("")
        assertEquals("example-org-tools", name, "call the marketplace example-org-tools")
        assertTrue(id.matches(name) && ".." !in name)
        assertTrue(name !in reserved && !name.startsWith("claudeai-"))
        assertFalse(mentionsAnthropic(name.lowercase()), "a name that mentions claude or anthropic risks being refused as an impersonation")
        val pluginNames = entries().map { it.path("name").asText() }.sorted()
        assertEquals(listOf("db-tools", "lint-helper", "standards-kit"), pluginNames, "list standards-kit, lint-helper and db-tools")
        for (n in pluginNames) {
            val low = n.lowercase()
            assertTrue(id.matches(n), "$n: letters, digits, dots, underscores and hyphens only")
            assertTrue(listOf("claude-", "anthropic-", "anthropics-", "cc-plugin-").none { low.startsWith(it) } && low !in listOf("claude", "anthropic", "anthropics", "claude-code", "claude-mods"), "$n passes as an Anthropic plugin")
            assertFalse("official" in low && mentionsAnthropic(low), "$n passes as an official plugin")
        }
        assertEquals("standards-kit", manifest().path("name").asText(null))
    }

    @Test
    fun e2_thePluginKeepsOnlyItsManifestInTheManifestFolderAndFindsItsFilesThroughThePluginRoot() {
        val inManifestFolder = Files.list(root.resolve(plugin).resolve(".claude-plugin")).use { s -> s.map { it.fileName.toString() }.toList() }
        assertEquals(listOf("plugin.json"), inManifestFolder, "only plugin.json goes inside .claude-plugin")
        assertFalse(Files.exists(root.resolve(plugin).resolve("CLAUDE.md")), "an instruction file at the plugin root is not loaded as context")
        val (fields, body) = front("$plugin/skills/changelog/SKILL.md")
        assertTrue(fields["name"] == "changelog" && "Use when" in (fields["description"] ?: "") && body.isNotBlank() && "TODO" !in body)
        val hooks = readJson("$plugin/hooks/hooks.json").path("hooks")
        assertTrue(hooks.isObject, "hooks.json wraps the event map in a top-level hooks key")
        val groups = hooks.path("PreToolUse")
        assertTrue(groups.size() == 1 && groups.get(0).path("matcher").asText(null) == "Edit|Write", "one PreToolUse group for Edit|Write")
        val handler = groups.get(0).path("hooks").path(0)
        assertTrue(handler.path("type").asText(null) == "command" && rootVar in handler.path("command").asText(""), "reach the script through $rootVar")
        assertTrue(Files.isRegularFile(root.resolve(plugin).resolve("scripts/protect.sh")))
        val servers = readJson("$plugin/.mcp.json").path("mcpServers")
        assertTrue(servers.size() > 0, "declare the ticket server")
        for (server in servers) assertTrue(rootVar in server.path("args").joinToString(" ") { it.asText() }, "every server reaches its code through $rootVar")
        for (key in listOf("skills", "commands", "agents", "hooks", "mcpServers")) {
            val value = manifest().path(key)
            val paths = if (value.isTextual) listOf(value.asText()) else if (value.isArray) value.map { it.asText() } else emptyList()
            for (path in paths) assertTrue(path.startsWith("./"), "a component path in plugin.json starts with ./ ($key)")
        }
    }

    @Test
    fun e3_theVersionLivesInTheManifestAloneAndIsSemantic() {
        val data = manifest()
        assertTrue(semver.matches(data.path("version").asText("")), "give the plugin a semantic version in plugin.json")
        assertFalse(data.path("description").asText("").isBlank())
        assertFalse(entry("standards-kit").has("version"), "set the version in plugin.json or in the entry, not both")
    }

    @Test
    fun e4_eachSourceKindIsWrittenInItsOwnFormAndPinnedToATagAndACommit() {
        assertEquals("./plugins/standards-kit", entry("standards-kit").path("source").asText(null))
        val helper = entry("lint-helper").path("source")
        assertTrue(helper.path("source").asText(null) == "github" && repo.matches(helper.path("repo").asText("")), "a github source takes owner/repo")
        val sub = entry("db-tools").path("source")
        assertTrue(sub.path("source").asText(null) == "git-subdir" && sub.path("path").asText(null) == "tools/db-tools" && sub.path("url").asText("").isNotEmpty(), "a git-subdir source takes a url and a path")
        for (p in entries()) {
            val source = p.get("source")
            if (source.isTextual) continue
            val name = p.path("name").asText()
            val type = source.path("source").asText("")
            if (type == "url") assertTrue(Regex("(https?://|file://|git@).*").matches(source.path("url").asText("")), "$name: a url source takes a full git url, not owner/repo")
            if (type in listOf("github", "url", "git-subdir")) {
                assertTrue(tag.matches(source.path("ref").asText("")), "$name: pin ref to a release tag such as v1.2.0")
                assertTrue(sha.matches(source.path("sha").asText("")), "$name: pin sha to a full 40-character lowercase commit")
            }
            if (type == "archive") assertTrue(Regex("[0-9a-fA-F]{64}").matches(source.path("sha256").asText("")), "$name: pin an archive with sha256")
        }
    }

    @Test
    fun e5_aDependencyCarriesARangeAndOneFromAnotherMarketplaceIsAllowedByName() {
        assertEquals(mapper.readTree("""[{"name": "lint-helper", "version": "~1.2.0"}]"""), manifest().get("dependencies"), "depend on lint-helper ~1.2.0")
        assertEquals(mapper.readTree("""[{"name": "audit-logger", "marketplace": "shared-tools"}]"""), entry("db-tools").get("dependencies"), "db-tools depends on audit-logger from shared-tools")
        assertEquals(mapper.readTree("""["shared-tools"]"""), market().get("allowCrossMarketplaceDependenciesOn"), "the root marketplace must allow shared-tools")
    }

    @Test
    fun e6_theTeamSettingsRegisterTheMarketplaceAndEnableOnlyWhatInstallsFromIt() {
        val settings = readJson(".claude/settings.json")
        val markets = settings.path("extraKnownMarketplaces")
        assertEquals(listOf(market().path("name").asText(null)), names(markets), "register the marketplace under its own name")
        val source = markets.get(market().path("name").asText()).path("source")
        assertTrue(source.path("source").asText(null) == "github" && repo.matches(source.path("repo").asText("")), "a github source with an owner/name repo")
        val enabled = settings.path("enabledPlugins")
        assertTrue(enabled.size() > 0)
        for (pid in names(enabled)) {
            assertTrue(enabled.get(pid).isBoolean && enabled.get(pid).asBoolean(), "$pid must be true")
            val name = pid.substringBefore('@')
            val marketplace = if ('@' in pid) pid.substringAfter('@') else ""
            assertEquals(market().path("name").asText(), marketplace, "$pid: enable plugins from the marketplace you register")
            assertTrue(entry(name).path("source").isTextual, "$pid: a plugin from an external source is not installed by the repository settings alone")
        }
        assertTrue(enabled.has("standards-kit@example-org-tools"))
    }

    @Test
    fun e7_renamesLeadEveryFormerNameToACurrentPluginOrToNull() {
        val renames = market().path("renames")
        assertTrue(renames.isObject && renames.path("std-kit").asText(null) == "standards-kit" && renames.has("old-linter") && renames.get("old-linter").isNull)
        val current = entries().map { it.path("name").asText() }.toSet()
        for (old in names(renames)) {
            assertFalse(old in current, "$old is still listed as a plugin")
            val seen = mutableSetOf(old)
            var step = renames.get(old)
            while (!step.isNull && step.asText() !in current) {
                assertTrue(renames.has(step.asText()) && step.asText() !in seen, "the rename of $old does not end at a plugin or at null")
                seen += step.asText()
                step = renames.get(step.asText())
            }
        }
    }

    @Test
    fun e8_noFileIsLeftUnfinishedOrHoldsAPersonalPathAnAddressOrAKey() {
        val all = Files.walk(root).use { w -> w.filter { Files.isRegularFile(it) }.sorted().toList() }
        val personal = Regex("""/home/|/Users/|[A-Za-z]:\\Users|sk-ant-""")
        val address = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
        for (path in all) {
            val text = Files.readString(path)
            val rel = root.relativize(path).toString()
            assertFalse("TODO" in text, "$rel still has a TODO")
            assertFalse(personal.containsMatchIn(text), "$rel holds a personal path or a key")
            for (m in address.findAll(text)) assertTrue(m.value.endsWith("@example.com"), "$rel holds an address that is not a placeholder")
        }
        assertTrue(all.size >= 8)
    }
}
