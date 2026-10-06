import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RolloutTest {
    // the folder that holds managed/, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val managedOnly = listOf("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags")
    private val effort = listOf("low", "medium", "high", "xhigh", "max")
    private val precedence = listOf("managed settings", "command line", "project local", "shared project", "user")

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun loadJson(rel: String): JsonNode = try {
        ObjectMapper().readTree(read(rel))
    } catch (e: java.io.IOException) {
        throw AssertionError("$rel is not valid JSON: ${e.message}")
    }

    private fun managed(): JsonNode = loadJson("managed/managed-settings.json")

    private fun section(title: String): String {
        val text = read("docs/rollout.md")
        val found = Regex("^## (.*)$", RegexOption.MULTILINE).findAll(text).toList()
        for ((i, m) in found.withIndex()) {
            if (m.groupValues[1].trim() == title) return text.substring(m.range.last + 1, if (i + 1 < found.size) found[i + 1].range.first else text.length).trim()
        }
        throw AssertionError("the section '$title' is missing")
    }

    private fun table(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in text.split("\n")) {
            if (line.startsWith("|")) {
                val cells = line.trim().replace(Regex("^\\||\\|$"), "").split("|").map { it.trim() }
                if (!cells.all { it.matches(Regex("[-: ]*")) }) rows += cells
            }
        }
        return rows.drop(1)
    }

    private fun limits(rows: List<List<String>>, level: String): List<Int> = rows.filter { it[0] == level }.map { it[2].replace(",", "").toInt() }

    @Test
    fun m1_theManagedFileLocksPermissionsAndProtectsTheEnvironmentFile() {
        val settings = managed()
        assertTrue(settings.path("allowManagedPermissionRulesOnly").asBoolean(false), "make managed settings the only source of permission rules")
        assertEquals("disable", settings.path("permissions").path("disableBypassPermissionsMode").asText(""), "turn off bypass mode")
        assertTrue(settings.path("permissions").path("deny").any { it.asText() == "Read(./.env)" }, "deny reading the environment file")
    }

    @Test
    fun e1_keysOnlyAnOrganisationCanSetLiveInTheManagedFileAndNeverInTheProjectFile() {
        val project = loadJson(".claude/settings.json")
        for (key in managedOnly) assertFalse(project.has(key), "$key has no effect in the project file: move it to the managed file")
        assertTrue(managed().path("allowManagedHooksOnly").asBoolean(false), "only managed hooks run")
    }

    @Test
    fun e2_pluginsComeOnlyFromTheCompanysMarketplaceAndCannotBeSideloaded() {
        val settings = managed()
        val sources = settings.path("strictKnownMarketplaces")
        assertTrue(sources.isArray && sources.size() > 0, "allow the company's marketplace; an empty list blocks every source, the official one too")
        for (entry in sources) {
            val ok = (entry.path("source").asText() == "github" && entry.path("repo").asText("").startsWith("example-org/")) ||
                (entry.path("source").asText() == "url" && entry.path("url").asText("").startsWith("https://plugins.example.com/"))
            assertTrue(ok, "$entry is not a source the company owns")
        }
        assertTrue(settings.path("disableSideloadFlags").asBoolean(false), "reject the flags that load plugins from a folder or an address")
    }

    @Test
    fun e3_onlyTheManagedMcpAllowlistAppliesAndAServerIsNeverAllowedAndDenied() {
        val settings = managed()
        assertTrue(settings.path("allowManagedMcpServersOnly").asBoolean(false), "ignore allowlists in user, project and local files")
        val allowed = settings.path("allowedMcpServers")
        assertTrue(allowed.size() > 0, "allow at least one server")
        val names = mutableSetOf<String>()
        for (entry in allowed) {
            val keys = entry.fieldNames().asSequence().toList()
            assertTrue(keys.size == 1 && keys[0] in listOf("serverName", "serverCommand", "serverUrl"), "$entry: exactly one of serverName, serverCommand or serverUrl")
            if (entry.has("serverName")) {
                assertTrue(entry.path("serverName").asText().matches(Regex("[A-Za-z0-9_-]+")), "${entry.path("serverName").asText()}: letters, numbers, hyphens and underscores only")
                names += entry.path("serverName").asText()
            }
        }
        val denied = settings.path("deniedMcpServers").filter { it.has("serverName") }.map { it.path("serverName").asText() }.toSet()
        assertEquals(setOf<String>(), names intersect denied, "${names intersect denied} is allowed and denied: the denial wins")
    }

    @Test
    fun e4_theModelChoiceIsLockedByAListAndTheEffortCapIsAtMostHigh() {
        val settings = managed()
        val models = settings.path("availableModels")
        assertTrue(models.isArray && models.size() > 0, "a managed model is only a default: list availableModels to lock the choice")
        if (settings.has("model")) assertTrue(models.any { it.asText() == settings.path("model").asText() }, "the default model is one of the available models")
        val cap = settings.path("maxEffortLevel").asText("")
        assertTrue(cap in effort && effort.indexOf(cap) <= effort.indexOf("high"), "cap the effort at high or lower; max sets no cap")
    }

    @Test
    fun e5_theGroupSpendLimitsAddUpToTheOrganisationLimitAndNoMore() {
        val text = section("Spend limits")
        assertTrue(text.lowercase().contains("usage credits"), "say that usage credits are on")
        val rows = table(text)
        val org = limits(rows, "organization")
        val groups = limits(rows, "group")
        val members = limits(rows, "member")
        assertTrue(org.size == 1 && groups.isNotEmpty() && members.isNotEmpty(), "list the organisation, its groups and the member default")
        assertTrue(groups.sum() <= org[0], "the group limits add up to the organisation limit or less")
        assertTrue(members[0] <= groups.min(), "a member's limit is within the smallest group's")
    }

    @Test
    fun e6_adoptionIsMeasuredByOutcomesAgainstABaselineOfAtLeast4Weeks() {
        val text = section("Adoption")
        val baseline = Regex("Baseline:\\s*(\\d+) weeks").find(text)
        assertTrue(baseline != null && baseline.groupValues[1].toInt() >= 4, "take a baseline of at least 4 weeks before the rollout")
        val block = Regex("Outcome targets:\\n((?:- .*\\n?)+)").find(text + "\n")
        assertNotNull(block, "list the outcome targets")
        val targets = block!!.groupValues[1].split("\n").filter { it.isNotEmpty() }.map { it.substring(2).lowercase() }
        assertTrue(targets.size >= 2, "set at least two outcome targets")
        for (target in targets) {
            assertTrue(listOf("pull requests", "time to merge", "review", "defects").any { target.contains(it) }, "'$target' is not an outcome")
            assertFalse(listOf("lines accepted", "prompts", "suggestions accepted").any { target.contains(it) }, "'$target' measures activity")
        }
    }

    @Test
    fun e7_thePrecedenceTableIsInTheDocumentedOrderAndNoFileHoldsPersonalData() {
        assertEquals(precedence, table(section("Precedence")).map { it[1].lowercase() }, "the levels are managed, command line, project local, shared project and user, in that order")
        val doc = read("docs/rollout.md")
        assertTrue(doc.contains("per-group") && doc.contains("not yet supported"), "say that server-managed settings cannot target a group yet")
        val hits = mutableListOf<String>()
        val home = Regex("(/home/\\w+|/Users/\\w+|C:\\\\Users)")
        val email = Regex("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+")
        Files.walk(root).use { s ->
            for (p in s.filter { Files.isRegularFile(it) }.sorted().toList()) {
                val text = Files.readString(p)
                val rel = root.relativize(p).toString()
                if (home.containsMatchIn(text)) hits += "$rel: home path"
                if (email.containsMatchIn(text)) hits += "$rel: email address"
            }
        }
        assertEquals(listOf<String>(), hits)
    }
}
