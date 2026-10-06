import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolGateTest {
    @Test
    fun aDeniedPermissionRefusesTheToolWhateverElseItAsksFor() {
        assertEquals(Decision("refused", listOf("network")), decide(listOf("read_files", "network")))
        assertEquals(Decision("refused", listOf("run_process", "network")), decide(listOf("run_process", "network")))
    }

    @Test
    fun aWriteWaitsForAPersonAndAReadRunsByItself() {
        assertEquals(Decision("needs_approval", listOf("write_files")), decide(listOf("read_files", "write_files")))
        assertEquals(Decision("auto", listOf()), decide(listOf("read_files")))
    }

    @Test
    fun aResultIsCheckedForFieldsTypesAndSizeBeforeTheAgentUsesIt() {
        assertEquals(listOf<String>(), checkOutput(mapOf("headline" to "ok", "rows" to 3)))
        assertEquals(listOf("missing: rows"), checkOutput(mapOf("headline" to "ok")))
        assertEquals(listOf("type: rows"), checkOutput(mapOf("headline" to "ok", "rows" to "3")))
        assertEquals(listOf("too large"), checkOutput(mapOf("headline" to "x".repeat(201), "rows" to 3)))
        assertEquals(listOf<String>(), checkOutput(mapOf("headline" to "x".repeat(200), "rows" to 3)))
    }
}
