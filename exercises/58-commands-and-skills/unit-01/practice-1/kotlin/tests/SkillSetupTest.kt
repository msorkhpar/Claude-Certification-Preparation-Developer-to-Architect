import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SkillSetupTest {
    // config.dir is the project folder that holds .claude/ and personal/: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val review = ".claude/skills/review-pr/SKILL.md"
    private val tag = ".claude/skills/release-tag/SKILL.md"
    private val standup = ".claude/commands/standup.md"
    private val mine = "personal/review-pr-mine/SKILL.md"
    private val all = listOf(review, tag, standup, mine)

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun load(rel: String): Parsed = try {
        parse(read(rel))
    } catch (e: Exception) {
        throw AssertionError("$rel is not readable: ${e.message}")
    }

    private fun str(o: Any?) = o?.toString() ?: ""

    private fun has(regex: String, text: String) = Regex(regex).containsMatchIn(text)

    @Test
    fun m1_theReviewSkillIsAForkedSkillWithAnExplicitTaskAndAnArgument() {
        val (meta, body) = load(review)
        assertNotNull(forkAgent(meta), "a review that prints a lot belongs in a forked context: set context: fork")
        assertTrue(str(meta["agent"]).isNotBlank(), "name the subagent type in agent")
        assertTrue(Regex("(?m)^1\\. ").containsMatchIn(body), "a forked skill is given its content as the task: write numbered steps, not guidelines")
        assertTrue(str(meta["argument-hint"]).isNotBlank(), "show what to type with argument-hint")
        assertTrue(has("""\$0|\$ARGUMENTS""", body), "use the pull request number in the steps with \$0")
    }

    @Test
    fun e1_theReleaseSkillIsStartedOnlyByAPersonAndPreApprovesPatterns() {
        val meta = load(tag).meta
        assertEquals(mapOf("you" to true, "claude" to false, "description_in_context" to false), invocation(meta), "a skill with side effects sets disable-model-invocation: true")
        val items = Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(str(meta["allowed-tools"])).map { it.value }.toList()
        assertTrue(items.isNotEmpty() && items.all { "(" in it }, "pre-approve patterns such as Bash(git tag *), never a whole tool")
        assertTrue(preApproved(meta, "Bash", "git tag -a v1.2.0 -m x") && preApproved(meta, "Bash", "git push origin v1.2.0"))
        assertTrue(!preApproved(meta, "Bash", "git push --force origin main") && !preApproved(meta, "Bash", "rm -rf build"))
    }

    @Test
    fun e2_toolsAreTakenAwayWithDisallowedToolsAndAllowedToolsOnlyPreApproves() {
        val meta = load(review).meta
        assertTrue(toolStatus(meta, "Edit") == "removed" && toolStatus(meta, "Write") == "removed", "list Edit and Write by bare name in disallowed-tools")
        assertNotEquals("removed", toolStatus(meta, "Read"), "only the tools that change things are removed")
        assertTrue(preApproved(meta, "Bash", "gh pr diff 12") && preApproved(meta, "Bash", "gh pr view 12"))
        assertTrue(!preApproved(meta, "Bash", "gh pr merge 12") && !preApproved(meta, "Bash", "rm -rf build"), "allowed-tools pre-approves the read-only gh commands and no more")
    }

    @Test
    fun e3_argumentsFillThePlaceholdersOfEveryFile() {
        var out = render(load(review).body, "123", emptyList())
        assertTrue("\$0" !in out && "gh pr diff 123" in out && "ARGUMENTS:" !in out)
        val (tagMeta, tagBody) = load(tag)
        assertEquals(listOf("version"), tagMeta["arguments"], "declare the named argument in arguments")
        out = render(tagBody, "v1.4.0", listOf("version"))
        assertTrue("\$version" !in out && "git tag -a v1.4.0 -m \"Release v1.4.0\"" in out && "ARGUMENTS:" !in out)
        out = render(load(standup).body, "ana")
        assertTrue("\$ARGUMENTS" !in out && "by ana since" in out && "ARGUMENTS:" !in out)
    }

    @Test
    fun e4_everyFileCreatesItsOwnSlashCommandAndThePersonalVariantHasANewName() {
        val names = all.associateWith { commandName(it, load(it).meta) }
        assertTrue(names[standup] == "standup" && names[review] == "review-pr" && names[tag] == "release-tag")
        assertEquals(4, names.values.toSet().size, "two files create one command: $names")
        assertNotEquals("review-pr", names[mine], "a personal skill with the team's name replaces it for you: give the variant its own name")
        val meta = load(standup).meta
        assertTrue(str(meta["description"]).isNotBlank() && str(meta["argument-hint"]).isNotBlank(), "the old command file keeps working: give it a description and a hint")
    }

    @Test
    fun e5_eachPieceOfGuidanceLivesWhereItLoadsTheWayItIsUsed() {
        val rows = linkedMapOf<String, String>()
        for (line in read("docs/placement.md").lines()) {
            val cells = line.trim().trim('|').split("|").map { it.trim().trim('`').trim() }
            if (cells.size == 2 && cells[0] != "Guidance" && cells[0] != "---") rows[cells[0]] = cells[1]
        }
        val want = linkedMapOf(
            "The team's pull request review checklist, run on demand" to ".claude/skills/review-pr/SKILL.md",
            "My own variant of that review with extra style notes" to "~/.claude/skills/review-pr-mine/SKILL.md",
            "Coding standards that apply to every task in the repository" to "CLAUDE.md",
            "Test file conventions for test files in many folders" to ".claude/rules/testing.md",
            "A release procedure with side effects that only a person starts" to ".claude/skills/release-tag/SKILL.md",
        )
        assertEquals(want, rows)
    }

    @Test
    fun e6_everySkillSaysWhenToUseItAndStaysInsideTheListingBudget() {
        for (rel in listOf(review, tag, mine)) {
            val meta = load(rel).meta
            val description = str(meta["description"])
            assertTrue(has("""(?:^|\. )Use when\b""", description), "$rel: the description starts a sentence with Use when")
            assertTrue(description.length + str(meta["when_to_use"]).length <= 1536, "$rel: the listing keeps 1,536 characters of description and when_to_use")
        }
    }

    @Test
    fun e7_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val patterns = listOf("home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""), "key" to Regex("""sk-ant-[\w-]{6,}"""))
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
