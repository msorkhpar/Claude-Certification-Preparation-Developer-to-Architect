import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineCheckTest {
    private static Path pipeline(Path dir, String... jobs) throws IOException {
        Files.createDirectories(dir.resolve("ci"));
        Files.writeString(dir.resolve("ci/pipeline.json"), "{\"jobs\": [" + String.join(",", jobs) + "]}");
        return dir;
    }

    /** A job with the given overrides spliced over the defaults of a scheduled batch report. */
    private static String job(String audience, String api, String kind, String passes, String context, String tools, String command) {
        return "{\"name\": \"j\", \"kind\": \"" + kind + "\", \"audience\": \"" + audience + "\", \"api\": \"" + api + "\", \"passes\": " + passes + ", \"session\": \"fresh\", \"context\": " + context
            + ", \"tools\": " + tools + ", \"command\": \"" + command.replace("\"", "\\\"") + "\"}";
    }

    @Test
    void theFlawedPipelineHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(List.of("blocking-batch: pre-merge-review", "single-pass-review: pre-merge-review", "shared-session: pre-merge-review", "no-prior-findings: pre-merge-review",
            "writes: pre-merge-review", "batchable: debt-report", "no-print-flag: test-generation", "no-existing-tests: test-generation"), PipelineCheck.audit(PipelineCheck.HERE.resolve("project-before")));
        assertEquals(List.of(), PipelineCheck.audit(PipelineCheck.HERE.resolve("project-after")));
    }

    @Test
    void whoWaitsDecidesBetweenBatchAndRealTime(@TempDir Path dir) throws IOException {
        assertEquals(List.of("blocking-batch: j"), PipelineCheck.audit(pipeline(dir.resolve("a"), job("waiting", "batch", "report", "[]", "[]", "[]", "python ci/run.py"))));
        assertEquals(List.of("batchable: j"), PipelineCheck.audit(pipeline(dir.resolve("b"), job("scheduled", "realtime", "report", "[]", "[]", "[]", "claude -p x"))));
        assertEquals(List.of(), PipelineCheck.audit(pipeline(dir.resolve("c"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude -p x"))));
    }

    @Test
    void aCommandIsHeadlessWithEitherSpellingAndAScriptNeedsNoFlag(@TempDir Path dir) throws IOException {
        assertEquals(List.of("no-print-flag: j"), PipelineCheck.audit(pipeline(dir.resolve("a"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude \"x\""))));
        assertEquals(List.of(), PipelineCheck.audit(pipeline(dir.resolve("b"), job("waiting", "realtime", "report", "[]", "[]", "[]", "claude --print \"x\""))));
        assertEquals(List.of(), PipelineCheck.audit(pipeline(dir.resolve("c"), job("scheduled", "batch", "report", "[]", "[]", "[]", "python ci/run.py"))));
        assertEquals(List.of("claude", "-p", "a b", "--x"), PipelineCheck.words("claude -p \"a b\" --x"));
    }

    @Test
    void aReviewNeedsAPassPerFileAndThenAnIntegrationPassInThatOrder(@TempDir Path dir) throws IOException {
        String ctx = "[\"prior_findings\"]";
        assertEquals(List.of(), PipelineCheck.audit(pipeline(dir.resolve("a"), job("waiting", "realtime", "review", "[\"per-file\", \"integration\"]", ctx, "[]", "claude -p x"))));
        assertEquals(List.of("single-pass-review: j"), PipelineCheck.audit(pipeline(dir.resolve("b"), job("waiting", "realtime", "review", "[\"integration\", \"per-file\"]", ctx, "[]", "claude -p x"))));
        assertEquals(List.of("single-pass-review: j"), PipelineCheck.audit(pipeline(dir.resolve("c"), job("waiting", "realtime", "review", "[\"per-file\"]", ctx, "[]", "claude -p x"))));
    }

    @Test
    void onlyAReviewIsHeldToReadOnlyTools(@TempDir Path dir) throws IOException {
        assertEquals(List.of(), PipelineCheck.audit(pipeline(dir, job("waiting", "realtime", "testgen", "[]", "[\"existing_tests\"]", "[\"Edit\"]", "claude -p x"))));
        assertEquals("Edit", PipelineCheck.load(PipelineCheck.HERE.resolve("project-after")).get(2).get("tools").get(2).asText());
    }
}
