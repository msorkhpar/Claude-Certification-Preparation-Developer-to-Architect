import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkillModelTest {
    private val meta = mapOf("allowed-tools" to "Bash(git tag *) Bash(git push origin *)", "disallowed-tools" to "Edit Write(src/**)")

    @Test
    fun aCommandAndASkillWithOneNameCreateOneSlashCommand() {
        assertEquals("deploy", commandName(".claude/commands/deploy.md", emptyMap()))
        assertEquals("deploy", commandName(".claude/skills/deploy/SKILL.md", emptyMap()))
        assertEquals("ship", commandName(".claude/skills/deploy/SKILL.md", mapOf("name" to "ship")))
    }

    @Test
    fun theHigherLevelWinsASharedName() {
        assertEquals("e", winner(mapOf("project" to "p", "personal" to "u", "enterprise" to "e")))
        assertEquals("u", winner(mapOf("project" to "p", "personal" to "u")))
        assertEquals("p", winner(mapOf("project" to "p")))
        assertNull(winner(emptyMap()))
    }

    @Test
    fun whoCanStartASkill() {
        assertEquals(mapOf("you" to true, "claude" to true, "description_in_context" to true), invocation(emptyMap()))
        assertEquals(mapOf("you" to true, "claude" to false, "description_in_context" to false), invocation(mapOf("disable-model-invocation" to true)))
        assertEquals(mapOf("you" to false, "claude" to true, "description_in_context" to true), invocation(mapOf("user-invocable" to false)))
    }

    @Test
    fun allowedToolsPreApprovesPatternsAndABareNameApprovesEverything() {
        assertTrue(preApproved(meta, "Bash", "git tag v1"))
        assertTrue(preApproved(meta, "Bash", "git push origin v1"))
        assertFalse(preApproved(meta, "Bash", "git push --force"))
        assertFalse(preApproved(meta, "Bash", "rm -rf build"))
        assertTrue(preApproved(mapOf("allowed-tools" to "Bash"), "Bash", "rm -rf build"))
        assertTrue(preApproved(mapOf("allowed-tools" to "Read, Grep"), "Grep"))
    }

    @Test
    fun onlyABareDisallowedNameRemovesATool() {
        assertTrue(removed(meta, "Edit"))
        assertFalse(removed(meta, "Write"))
        assertFalse(removed(mapOf("disallowed-tools" to "Edit(src/**)"), "Edit"))
        assertEquals("removed", toolStatus(meta, "Edit"))
        assertEquals("pre-approved", toolStatus(meta, "Bash", "git tag v1"))
        assertEquals("permission settings decide", toolStatus(meta, "Read"))
    }

    @Test
    fun argumentsFillPlaceholdersInShellStyle() {
        assertEquals("pr 123 by ana", render("pr \$0 by \$1", "123 ana"))
        assertEquals("first=hello world all=\"hello world\" second", render("first=\$ARGUMENTS[0] all=\$ARGUMENTS", "\"hello world\" second"))
        assertEquals("tag v2 on main", render("tag \$version on \$branch", "v2 main", listOf("version", "branch")))
        assertEquals("only \$1", render("only \$1", "a"))
        assertEquals("tag v2 on ", render("tag \$version on \$branch", "v2", listOf("version", "branch")))
    }

    @Test
    fun inputThatNoPlaceholderReceivesIsAppended() {
        assertEquals("Review the change.\nARGUMENTS: 123\n", render("Review the change.\n", "123"))
        assertEquals("Review the change.\n", render("Review the change.\n", ""))
        assertEquals("Tag v1\n", render("Tag \$version\n", "v1", listOf("version")))
    }

    @Test
    fun aForkedSkillRunsInASubagentThatDefaultsToGeneralPurpose() {
        assertEquals("general-purpose", forkAgent(mapOf("context" to "fork")))
        assertEquals("Explore", forkAgent(mapOf("context" to "fork", "agent" to "Explore")))
        assertNull(forkAgent(emptyMap()))
    }

    @Test
    fun theFrontmatterIsSplitFromTheBody() {
        val p = parse("---\nname: x\narguments: [a, b]\n---\nHello \$a\n")
        assertEquals(mapOf("name" to "x", "arguments" to listOf("a", "b")), p.meta)
        assertEquals("Hello \$a\n", p.body)
    }
}
