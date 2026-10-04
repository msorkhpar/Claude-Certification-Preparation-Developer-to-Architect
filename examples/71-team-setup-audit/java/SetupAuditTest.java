import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupAuditTest {
    @Test
    void aDoubleStarCrossesFoldersAndASingleStarDoesNot() {
        assertTrue(SetupAudit.globRegex("**/*.test.ts").matcher("a/b/c.test.ts").matches() && SetupAudit.globRegex("**/*.test.ts").matcher("c.test.ts").matches());
        assertTrue(SetupAudit.globRegex("src/*.ts").matcher("src/a.ts").matches() && !SetupAudit.globRegex("src/*.ts").matcher("src/x/a.ts").matches());
        assertFalse(SetupAudit.globRegex("src/api/**/*.ts").matcher("src/models/a.ts").matches());
    }

    @Test
    void thePathsOfARuleAreReadFromItsFrontMatterAndAbsentMeansAlways() {
        assertEquals(List.of("a/**", "b.md"), SetupAudit.rulePaths("---\npaths:\n  - \"a/**\"\n  - b.md\n---\n\nbody\n"));
        assertNull(SetupAudit.rulePaths("# no front matter\n"));
        assertNull(SetupAudit.rulePaths("---\nname: x\n---\nbody\n"));
    }

    @Test
    void theFlawedProjectHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(List.of("all-in-root: 5 sections in CLAUDE.md and no rule files", "test-uncovered: src/components/Button.test.tsx", "test-uncovered: src/api/orders.test.ts",
            "test-uncovered: src/models/order.test.ts", "no-shared-command", "env-readable", "bare-bash-allowed"), SetupAudit.audit(SetupAudit.HERE.resolve("project-before")));
        assertEquals(List.of(), SetupAudit.audit(SetupAudit.HERE.resolve("project-after")));
    }

    @Test
    void testFilesLoadTheTestsRuleWhereverTheySit() {
        var rules = SetupAudit.load(SetupAudit.HERE.resolve("project-after")).rules();
        assertEquals(List.of("components.md", "tests.md"), SetupAudit.rulesFor(rules, "src/components/Button.test.tsx"));
        assertEquals(List.of(), SetupAudit.rulesFor(rules, "docs/readme.md"));
    }

    @Test
    void aRuleWithoutPathsAndARuleThatMatchesNothingAreFlagged(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve(".claude/rules"));
        Files.createDirectories(dir.resolve(".claude/commands"));
        Files.writeString(dir.resolve(".claude/commands/review.md"), "review\n");
        Files.writeString(dir.resolve(".claude/settings.json"), "{\"permissions\": {\"deny\": [\"Read(./.env)\"]}}");
        Files.writeString(dir.resolve("files.txt"), "src/a.ts\n");
        Files.writeString(dir.resolve("CLAUDE.md"), "# Notes\n");
        Files.writeString(dir.resolve(".claude/rules/everywhere.md"), "# Always\n");
        Files.writeString(dir.resolve(".claude/rules/typo.md"), "---\npaths:\n  - \"source/**/*.ts\"\n---\n\nx\n");
        assertEquals(List.of("rule-loads-always: everywhere.md", "rule-matches-nothing: typo.md"), SetupAudit.audit(dir));
    }
}
