import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToolReviewTest {
    private static final ToolReview.Policy POLICY = new ToolReview.Policy(12, 10, 256, List.of("network", "run_process"), List.of("write_files"));
    private static final String READ = "def run(path):\n    return open(path).read()\n";

    private static ToolReview.Proposal proposal(String name, int words, List<String> permissions, int timeoutS, int memoryMb, String code) {
        return new ToolReview.Proposal(name, String.join(" ", java.util.Collections.nCopies(words, "word")), permissions, timeoutS, memoryMb, code);
    }

    private static ToolReview.Proposal proposal() {
        return proposal("summarise_report", 15, List.of("read_files"), 5, 128, READ);
    }

    private static ToolReview.Proposal withName(String name) {
        return proposal(name, 15, List.of("read_files"), 5, 128, READ);
    }

    private static ToolReview.Proposal withWords(int words) {
        return proposal("summarise_report", words, List.of("read_files"), 5, 128, READ);
    }

    private static ToolReview.Proposal withCode(String code, String... permissions) {
        return proposal("summarise_report", 15, List.of(permissions), 5, 128, code);
    }

    private static ToolReview.Report out(String decision, List<String> refusals, List<String> findings, List<String> used) {
        return new ToolReview.Report("summarise_report", decision, refusals, findings, used, "summarise_report: " + decision);
    }

    @Test
    void m1_aWellFormedReadOnlyToolIsApprovedAndLeavesAnAuditLine() {
        assertEquals(out("approve", List.of(), List.of(), List.of("read_files")), ToolReview.review(proposal(), POLICY));
    }

    @Test
    void e1_aDescriptionNeedsAtLeastTheMinimumNumberOfWords() {
        assertEquals(List.of(), ToolReview.review(withWords(12), POLICY).findings());
        var shortOne = ToolReview.review(withWords(11), POLICY);
        assertEquals(List.of("short_description"), shortOne.findings());
        assertEquals("revise", shortOne.decision());
    }

    @Test
    void e2_theTimeoutAndTheMemoryMayEqualTheirLimitsAndNotExceedThem() {
        assertEquals(List.of(), ToolReview.review(proposal("summarise_report", 15, List.of("read_files"), 10, 256, READ), POLICY).findings());
        assertEquals(List.of("timeout"), ToolReview.review(proposal("summarise_report", 15, List.of("read_files"), 11, 128, READ), POLICY).findings());
        assertEquals(List.of("memory"), ToolReview.review(proposal("summarise_report", 15, List.of("read_files"), 5, 257, READ), POLICY).findings());
    }

    @Test
    void e3_aNameIsLowerCaseSnakeCaseOfAtMostSixtyFourCharacters() {
        assertEquals(List.of(), ToolReview.review(withName("a".repeat(64)), POLICY).findings());
        assertEquals(List.of("bad_name"), ToolReview.review(withName("a".repeat(65)), POLICY).findings());
        assertEquals(List.of("bad_name"), ToolReview.review(withName("Summarise"), POLICY).findings());
        assertEquals(List.of("bad_name"), ToolReview.review(withName("ab"), POLICY).findings());
    }

    @Test
    void e4_forbiddenCallsInTheCodeRefuseTheToolAndAreAllListedInOrder() {
        var result = ToolReview.review(withCode(READ + "eval(text)\nos.system('ls')\n", "read_files"), POLICY);
        assertEquals("refuse", result.decision());
        assertEquals(List.of("forbidden:eval(", "forbidden:os.system"), result.refusals());
    }

    @Test
    void e5_aPermissionTheCodeUsesWithoutDeclaringItOrADeniedOneRefusesTheTool() {
        var sneaky = ToolReview.review(withCode(READ + "requests.get(url)\n", "read_files"), POLICY);
        assertEquals("refuse", sneaky.decision());
        assertEquals(List.of("undeclared:network"), sneaky.refusals());
        assertEquals(List.of("network", "read_files"), sneaky.used());
        var declared = ToolReview.review(withCode(READ + "requests.get(url)\n", "read_files", "network"), POLICY);
        assertEquals("refuse", declared.decision());
        assertEquals(List.of("denied:network"), declared.refusals());
    }

    @Test
    void e6_aDeclaredWriteIsApprovedOnlyWithAGateAndAReadAloneIsApprovedOutright() {
        var writer = ToolReview.review(withCode(READ + "out.write(text)\n", "read_files", "write_files"), POLICY);
        assertEquals(out("approve_with_gate", List.of(), List.of(), List.of("read_files", "write_files")), writer);
        assertEquals("approve", ToolReview.review(proposal(), POLICY).decision());
    }

    @Test
    void e7_aRefusalBeatsARevisionAndARevisionBeatsAGate() {
        var both = ToolReview.review(proposal("summarise_report", 3, List.of("read_files"), 5, 128, READ + "eval(text)\n"), POLICY);
        assertEquals("refuse", both.decision());
        assertEquals(List.of("short_description"), both.findings());
        assertEquals("revise", ToolReview.review(proposal("summarise_report", 3, List.of("read_files", "write_files"), 5, 128, READ + "out.write(text)\n"), POLICY).decision());
    }

    @Test
    void e8_thePermissionsTheCodeUsesAreReportedInAlphabeticalOrder() {
        var result = ToolReview.review(withCode("shutil.copy(a, b)\nopen(a).read()\n", "read_files", "write_files"), POLICY);
        assertEquals(List.of("read_files", "write_files"), result.used());
    }
}
