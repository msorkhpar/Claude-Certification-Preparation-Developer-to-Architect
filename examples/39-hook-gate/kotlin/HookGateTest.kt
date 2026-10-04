import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HookGateTest {
    private val json = ObjectMapper()

    private fun bash(command: String) = preToolUse(mapOf("hook_event_name" to "PreToolUse", "tool_name" to "Bash", "tool_input" to mapOf("command" to command)))

    private fun decision(out: String) = json.readTree(out)["hookSpecificOutput"]["permissionDecision"].asText()

    @Test
    fun otherFormsOfAForbiddenCommandAreStillRefused() {
        for (command in listOf("git push origin main", "git -C . push", "git -c push.default=current push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push")) {
            val a = bash(command)
            assertEquals(0, a.code, command)
            assertEquals("deny", decision(a.out), command)
        }
        assertNull(dangerous("git status"))
        assertNull(dangerous("git log --oneline"))
        assertNull(dangerous("echo push"))
    }

    @Test
    fun aRecursiveForcedDeleteIsRefusedInEverySpellingAndAPlainDeleteIsNot() {
        for (c in listOf("rm -rf build", "rm -fr build", "rm -r -f build", "/bin/rm -rf x", "sh -c 'rm -rf x'", "rm --recursive --force x")) assertNotNull(dangerous(c), c)
        for (c in listOf("rm build/old.txt", "rm -r build", "rm -f build/a")) assertNull(dangerous(c), c)
    }

    @Test
    fun aProtectedPathIsBlockedWithExitTwoAndAReasonOnStandardError() {
        val a = preToolUse(mapOf("tool_name" to "Write", "tool_input" to mapOf("file_path" to "C:\\work\\.git\\config")))
        assertEquals(2, a.code)
        assertEquals("", a.out)
        assertTrue("protected pattern '.git/'" in a.err)
        assertEquals(Answer(0, "", ""), preToolUse(mapOf("tool_name" to "Edit", "tool_input" to mapOf("file_path" to "/w/src/main.py"))))
        assertEquals(Answer(0, "", ""), preToolUse(mapOf("tool_name" to "Read", "tool_input" to mapOf("file_path" to "/w/.env"))))
    }

    private fun runHook(event: String): Triple<Int, String, String> {
        val java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val p = ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), "HookGateKt", "--hook").start()
        p.outputStream.use { it.write(event.toByteArray()) }
        val out = p.inputStream.readBytes().decodeToString()
        val err = p.errorStream.readBytes().decodeToString()
        return Triple(p.waitFor(), out, err)
    }

    @Test
    fun theFileWorksAsAHookProcess() {
        val (code, out, _) = runHook(json.writeValueAsString(mapOf("tool_name" to "Bash", "tool_input" to mapOf("command" to "git push"))))
        assertEquals(0, code)
        assertEquals("deny", decision(out))
        val (blockedCode, _, err) = runHook(json.writeValueAsString(mapOf("tool_name" to "Edit", "tool_input" to mapOf("file_path" to ".env"))))
        assertEquals(2, blockedCode)
        assertTrue("Blocked" in err)
    }

    @Test
    fun skillAndAgentFilesAreLinted() {
        assertEquals(2, lintSkill("---\nname: deploy\ndescription: Deploy it\nallowed-tools: Bash\n---\nx").size)
        assertEquals(emptyList<String>(), lintSkill("---\nname: deploy\ndescription: Deploy it\ndisable-model-invocation: true\nallowed-tools: Bash(git add *) Read\n---\nx"))
        assertEquals(listOf("description is missing: Claude uses it to decide when to load the skill"), lintSkill("---\nname: x\n---\nbody"))
        assertEquals(emptyList<String>(), lintAgent("---\nname: r\ndescription: d\ntools: Read, Grep\nmemory: project\n---\nx"))
        assertEquals(3, lintAgent("---\nname: r\ndescription: d\nmemory: team\npermissionMode: bypassPermissions\n---\nx").size)
    }
}
