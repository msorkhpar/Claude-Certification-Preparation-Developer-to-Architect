import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PipelineCheckTest {
    private fun pipeline(dir: Path, vararg jobs: String): Path {
        Files.createDirectories(dir.resolve("ci"))
        Files.writeString(dir.resolve("ci/pipeline.json"), "{\"jobs\": [" + jobs.joinToString(",") + "]}")
        return dir
    }

    private fun job(audience: String, api: String, kind: String, passes: String, context: String, tools: String, command: String) =
        "{\"name\": \"j\", \"kind\": \"$kind\", \"audience\": \"$audience\", \"api\": \"$api\", \"passes\": $passes, \"session\": \"fresh\", \"context\": $context, \"tools\": $tools, \"command\": \"" +
            command.replace("\"", "\\\"") + "\"}"

    @Test
    fun theFlawedPipelineHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(listOf("blocking-batch: pre-merge-review", "single-pass-review: pre-merge-review", "shared-session: pre-merge-review", "no-prior-findings: pre-merge-review",
            "writes: pre-merge-review", "batchable: debt-report", "no-print-flag: test-generation", "no-existing-tests: test-generation"), audit(HERE.resolve("project-before")))
        assertEquals(listOf<String>(), audit(HERE.resolve("project-after")))
    }

    @Test
    fun whoWaitsDecidesBetweenBatchAndRealTime(@TempDir dir: Path) {
        assertEquals(listOf("blocking-batch: j"), audit(pipeline(dir.resolve("a"), job("waiting", "batch", "report", "[]", "[]", "[]", "python ci/run.py"))))
        assertEquals(listOf("batchable: j"), audit(pipeline(dir.resolve("b"), job("scheduled", "realtime", "report", "[]", "[]", "[]", "claude -p x"))))
        assertEquals(listOf<String>(), audit(pipeline(dir.resolve("c"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude -p x"))))
    }

    @Test
    fun aCommandIsHeadlessWithEitherSpellingAndAScriptNeedsNoFlag(@TempDir dir: Path) {
        assertEquals(listOf("no-print-flag: j"), audit(pipeline(dir.resolve("a"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude \"x\""))))
        assertEquals(listOf<String>(), audit(pipeline(dir.resolve("b"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude --print \"x\""))))
        assertEquals(listOf<String>(), audit(pipeline(dir.resolve("c"), job("scheduled", "batch", "report", "[]", "[]", "[]", "python ci/run.py"))))
        assertEquals(listOf("claude", "-p", "a b", "--x"), words("claude -p \"a b\" --x"))
    }

    @Test
    fun aReviewNeedsAPassPerFileAndThenAnIntegrationPassInThatOrder(@TempDir dir: Path) {
        val ctx = "[\"prior_findings\"]"
        assertEquals(listOf<String>(), audit(pipeline(dir.resolve("a"), job("waiting", "realtime", "review", "[\"per-file\", \"integration\"]", ctx, "[]", "claude -p x"))))
        assertEquals(listOf("single-pass-review: j"), audit(pipeline(dir.resolve("b"), job("waiting", "realtime", "review", "[\"integration\", \"per-file\"]", ctx, "[]", "claude -p x"))))
        assertEquals(listOf("single-pass-review: j"), audit(pipeline(dir.resolve("c"), job("waiting", "realtime", "review", "[\"per-file\"]", ctx, "[]", "claude -p x"))))
    }

    @Test
    fun onlyAReviewIsHeldToReadOnlyTools(@TempDir dir: Path) {
        assertEquals(listOf<String>(), audit(pipeline(dir, job("waiting", "realtime", "testgen", "[]", "[\"existing_tests\"]", "[\"Edit\"]", "claude -p x"))))
        assertEquals("Edit", load(HERE.resolve("project-after")).get(2).get("tools").get(2).asText())
    }
}
