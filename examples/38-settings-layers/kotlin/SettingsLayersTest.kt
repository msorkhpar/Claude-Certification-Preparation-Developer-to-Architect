import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsLayersTest {
    private fun layers() = linkedMapOf(
        "managed" to json("""{"permissions": {"deny": ["Bash(curl *)"]}}"""),
        "user" to json("""{"model": "sonnet", "permissions": {"allow": ["Bash(git status *)"]}}"""),
        "project" to json("""{"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"], "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}}"""),
        "local" to json("""{"model": "haiku", "permissions": {"allow": ["Bash(git push *)"]}}"""),
    )

    @Suppress("UNCHECKED_CAST")
    private fun perms(s: Map<String, Any?>) = s["permissions"] as Map<String, Any?>

    private fun decisions(s: Map<String, Any?>, tool: String, vararg args: String) = args.map { decide(s, tool, it) }

    @Test
    fun aHigherLevelWinsAScalarKeyAndListsCombine() {
        val s = effectiveSettings(layers())
        assertEquals("haiku", s["model"])
        assertEquals(listOf("Bash(git status *)", "Bash(npm run *)", "Bash(curl *)", "Bash(git push *)"), perms(s)["allow"])
    }

    @Test
    fun aRepositoryFileCannotSetBypassPermissionsAndItsAllowRulesWaitForTrust() {
        assertFalse("defaultMode" in perms(effectiveSettings(layers())))
        assertEquals("ask", decide(effectiveSettings(layers(), trusted = false), "Bash", "npm run build"))
        assertEquals("allow", decide(effectiveSettings(layers()), "Bash", "npm run build"))
    }

    @Test
    fun denyBeatsAskBeatsAllowWhateverTheLevel() {
        val s = effectiveSettings(layers())
        assertEquals("deny", decide(s, "Bash", "curl https://example.com"))
        assertEquals("ask", decide(s, "Bash", "git push origin main"))
        assertEquals("ask", decide(s, "Bash", "npm run build && git push origin main"))
        assertEquals("ask", decide(s, "Bash", "rm -rf build"))
    }

    @Test
    fun wildcardsMatchTheBareCommandButNotALongerProgramName() {
        val s = json("""{"permissions": {"allow": ["Bash(ls *)", "Bash(npm run build)"]}}""")
        assertEquals(listOf("allow", "allow", "ask", "allow", "ask"), decisions(s, "Bash", "ls", "ls -la", "lsof", "npm run build", "npm run build --watch"))
    }

    @Test
    fun pathRulesFollowTheRuleTypeAndAReadDenyAlsoBlocksEdits() {
        val s = json("""{"permissions": {"deny": ["Read(./.env)", "Read(secrets/**)"], "allow": ["Edit(src/**)"]}}""")
        assertEquals(listOf("deny", "deny", "deny", "deny", "allow"), decisions(s, "Read", "./.env", "sub/.env", "secrets/a.txt", "vendor/secrets/a.txt", "src/app.ts"))
        assertEquals(listOf("deny", "allow", "ask"), decisions(s, "Edit", ".env", "src/app.ts", "vendor/pkg/src/lib.js"))
    }

    @Test
    fun memoryFilesLoadBroadToSpecificWithImportsAndWithoutBacktickedOnes() {
        val files = mapOf(
            "/m/CLAUDE.md" to "m", "/u/CLAUDE.md" to "u", "/r/CLAUDE.md" to "See @docs/a.md and `@README`", "/r/docs/a.md" to "A, then @b.md",
            "/r/docs/b.md" to "B", "/r/s/CLAUDE.md" to "s", "/r/s/CLAUDE.local.md" to "l", "/r/o/CLAUDE.md" to "o",
        )
        assertEquals(
            listOf("/m/CLAUDE.md", "/u/CLAUDE.md", "/r/CLAUDE.md", "/r/docs/a.md", "/r/docs/b.md", "/r/s/CLAUDE.md", "/r/s/CLAUDE.local.md"),
            loadMemory(files, "/r/s", "/m/CLAUDE.md", "/u/CLAUDE.md"),
        )
    }

    @Test
    fun acceptEditsModeAcceptsAnUndecidedEditButNotADenyOrACommand() {
        val s = json("""{"permissions": {"deny": ["Edit(.env)"]}}""")
        assertEquals("allow", decide(s, "Edit", "src/a.py", mode = "acceptEdits"))
        assertEquals("deny", decide(s, "Edit", ".env", mode = "acceptEdits"))
        assertEquals("ask", decide(s, "Bash", "make deploy", mode = "acceptEdits"))
        assertEquals("ask", decide(s, "Edit", "src/a.py"))
    }
}
