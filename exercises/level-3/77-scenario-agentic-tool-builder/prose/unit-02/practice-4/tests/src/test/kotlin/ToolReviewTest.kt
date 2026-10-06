import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolReviewTest {
    private val policy = Policy(12, 10, 256, listOf("network", "run_process"), listOf("write_files"))
    private val read = "def run(path):\n    return open(path).read()\n"

    private fun proposal(name: String = "summarise_report", words: Int = 15, permissions: List<String> = listOf("read_files"), timeoutS: Int = 5, memoryMb: Int = 128, code: String = read) =
        Proposal(name, List(words) { "word" }.joinToString(" "), permissions, timeoutS, memoryMb, code)

    private fun out(decision: String, refusals: List<String> = listOf(), findings: List<String> = listOf(), used: List<String> = listOf("read_files")) =
        Report("summarise_report", decision, refusals, findings, used, "summarise_report: $decision")

    @Test
    fun m1_aWellFormedReadOnlyToolIsApprovedAndLeavesAnAuditLine() {
        assertEquals(out("approve"), review(proposal(), policy))
    }

    @Test
    fun e1_aDescriptionNeedsAtLeastTheMinimumNumberOfWords() {
        assertEquals(listOf<String>(), review(proposal(words = 12), policy).findings)
        val short = review(proposal(words = 11), policy)
        assertEquals(listOf("short_description"), short.findings)
        assertEquals("revise", short.decision)
    }

    @Test
    fun e2_theTimeoutAndTheMemoryMayEqualTheirLimitsAndNotExceedThem() {
        assertEquals(listOf<String>(), review(proposal(timeoutS = 10, memoryMb = 256), policy).findings)
        assertEquals(listOf("timeout"), review(proposal(timeoutS = 11), policy).findings)
        assertEquals(listOf("memory"), review(proposal(memoryMb = 257), policy).findings)
    }

    @Test
    fun e3_aNameIsLowerCaseSnakeCaseOfAtMostSixtyFourCharacters() {
        assertEquals(listOf<String>(), review(proposal(name = "a".repeat(64)), policy).findings)
        assertEquals(listOf("bad_name"), review(proposal(name = "a".repeat(65)), policy).findings)
        assertEquals(listOf("bad_name"), review(proposal(name = "Summarise"), policy).findings)
        assertEquals(listOf("bad_name"), review(proposal(name = "ab"), policy).findings)
    }

    @Test
    fun e4_forbiddenCallsInTheCodeRefuseTheToolAndAreAllListedInOrder() {
        val result = review(proposal(code = read + "eval(text)\nos.system('ls')\n"), policy)
        assertEquals("refuse", result.decision)
        assertEquals(listOf("forbidden:eval(", "forbidden:os.system"), result.refusals)
    }

    @Test
    fun e5_aPermissionTheCodeUsesWithoutDeclaringItOrADeniedOneRefusesTheTool() {
        val sneaky = review(proposal(code = read + "requests.get(url)\n"), policy)
        assertEquals("refuse", sneaky.decision)
        assertEquals(listOf("undeclared:network"), sneaky.refusals)
        assertEquals(listOf("network", "read_files"), sneaky.used)
        val declared = review(proposal(permissions = listOf("read_files", "network"), code = read + "requests.get(url)\n"), policy)
        assertEquals("refuse", declared.decision)
        assertEquals(listOf("denied:network"), declared.refusals)
    }

    @Test
    fun e6_aDeclaredWriteIsApprovedOnlyWithAGateAndAReadAloneIsApprovedOutright() {
        val writer = review(proposal(permissions = listOf("write_files"), code = "out.write(text)\n"), policy)
        assertEquals(out("approve_with_gate", used = listOf("write_files")), writer)
        assertEquals("approve", review(proposal(), policy).decision)
    }

    @Test
    fun e7_aRefusalBeatsARevisionAndARevisionBeatsAGate() {
        val both = review(proposal(words = 3, code = read + "eval(text)\n"), policy)
        assertEquals("refuse", both.decision)
        assertEquals(listOf("short_description"), both.findings)
        assertEquals("revise", review(proposal(words = 3, permissions = listOf("read_files", "write_files"), code = read + "out.write(text)\n"), policy).decision)
    }

    @Test
    fun e8_thePermissionsTheCodeUsesAreReportedInAlphabeticalOrder() {
        val result = review(proposal(permissions = listOf("read_files", "write_files"), code = "shutil.copy(a, b)\nopen(a).read()\n"), policy)
        assertEquals(listOf("read_files", "write_files"), result.used)
    }
}
