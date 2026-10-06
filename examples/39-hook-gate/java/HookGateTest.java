import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HookGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static HookGate.Answer bash(String command) throws IOException {
        return HookGate.preToolUse(Map.of("hook_event_name", "PreToolUse", "tool_name", "Bash", "tool_input", Map.of("command", command)));
    }

    @SuppressWarnings("unchecked")
    private static String decision(String out) throws IOException {
        return (String) ((Map<String, Object>) JSON.readValue(out, Map.class).get("hookSpecificOutput")).get("permissionDecision");
    }

    @Test
    void otherFormsOfAForbiddenCommandAreStillRefused() throws IOException {
        for (String command : List.of("git push origin main", "git -C . push", "git -c push.default=current push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push")) {
            HookGate.Answer a = bash(command);
            assertEquals(0, a.code(), command);
            assertEquals("deny", decision(a.out()), command);
        }
        assertNull(HookGate.dangerous("git status"));
        assertNull(HookGate.dangerous("git log --oneline"));
        assertNull(HookGate.dangerous("echo push"));
    }

    @Test
    void aRecursiveForcedDeleteIsRefusedInEverySpellingAndAPlainDeleteIsNot() {
        for (String c : List.of("rm -rf build", "rm -fr build", "rm -r -f build", "/bin/rm -rf x", "sh -c 'rm -rf x'", "rm --recursive --force x")) assertNotNull(HookGate.dangerous(c), c);
        for (String c : List.of("rm build/old.txt", "rm -r build", "rm -f build/a")) assertNull(HookGate.dangerous(c), c);
    }

    @Test
    void aProtectedPathIsBlockedWithExitTwoAndAReasonOnStandardError() throws IOException {
        HookGate.Answer a = HookGate.preToolUse(Map.of("tool_name", "Write", "tool_input", Map.of("file_path", "C:\\work\\.git\\config")));
        assertEquals(2, a.code());
        assertEquals("", a.out());
        assertTrue(a.err().contains("protected pattern '.git/'"));
        assertEquals(new HookGate.Answer(0, "", ""), HookGate.preToolUse(Map.of("tool_name", "Edit", "tool_input", Map.of("file_path", "/w/src/main.py"))));
        assertEquals(new HookGate.Answer(0, "", ""), HookGate.preToolUse(Map.of("tool_name", "Read", "tool_input", Map.of("file_path", "/w/.env"))));
    }

    private record Run(int code, String out, String err) {}

    private static Run runHook(String event) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        Process p = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), "HookGate", "--hook").start();
        p.getOutputStream().write(event.getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Run(p.waitFor(), out, err);
    }

    @Test
    void theFileWorksAsAHookProcess() throws Exception {
        Run run = runHook(JSON.writeValueAsString(Map.of("tool_name", "Bash", "tool_input", Map.of("command", "git push"))));
        assertEquals(0, run.code());
        assertEquals("deny", decision(run.out()));
        Run blocked = runHook(JSON.writeValueAsString(Map.of("tool_name", "Edit", "tool_input", Map.of("file_path", ".env"))));
        assertEquals(2, blocked.code());
        assertTrue(blocked.err().contains("Blocked"));
    }

    @Test
    void skillAndAgentFilesAreLinted() throws IOException {
        assertEquals(2, HookGate.lintSkill("---\nname: deploy\ndescription: Deploy it\nallowed-tools: Bash\n---\nx").size());
        assertEquals(List.of(), HookGate.lintSkill("---\nname: deploy\ndescription: Deploy it\ndisable-model-invocation: true\nallowed-tools: Bash(git add *) Read\n---\nx"));
        assertEquals(List.of("description is missing: Claude uses it to decide when to load the skill"), HookGate.lintSkill("---\nname: x\n---\nbody"));
        assertEquals(List.of(), HookGate.lintAgent("---\nname: r\ndescription: d\ntools: Read, Grep\nmemory: project\n---\nx"));
        assertEquals(3, HookGate.lintAgent("---\nname: r\ndescription: d\nmemory: team\npermissionMode: bypassPermissions\n---\nx").size());
    }
}
