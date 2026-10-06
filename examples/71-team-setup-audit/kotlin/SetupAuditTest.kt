import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SetupAuditTest {
    @Test
    fun aDoubleStarCrossesFoldersAndASingleStarDoesNot() {
        assertTrue(globRegex("**/*.test.ts").matches("a/b/c.test.ts") && globRegex("**/*.test.ts").matches("c.test.ts"))
        assertTrue(globRegex("src/*.ts").matches("src/a.ts") && !globRegex("src/*.ts").matches("src/x/a.ts"))
        assertFalse(globRegex("src/api/**/*.ts").matches("src/models/a.ts"))
    }

    @Test
    fun thePathsOfARuleAreReadFromItsFrontMatterAndAbsentMeansAlways() {
        assertEquals(listOf("a/**", "b.md"), rulePaths("---\npaths:\n  - \"a/**\"\n  - b.md\n---\n\nbody\n"))
        assertNull(rulePaths("# no front matter\n"))
        assertNull(rulePaths("---\nname: x\n---\nbody\n"))
    }

    @Test
    fun theFlawedProjectHasEveryFindingAndTheFixedOneHasNone() {
        assertEquals(listOf("all-in-root: 5 sections in CLAUDE.md and no rule files", "test-uncovered: src/components/Button.test.tsx", "test-uncovered: src/api/orders.test.ts",
            "test-uncovered: src/models/order.test.ts", "no-shared-command", "env-readable", "bare-bash-allowed"), audit(HERE.resolve("project-before")))
        assertEquals(listOf<String>(), audit(HERE.resolve("project-after")))
    }

    @Test
    fun testFilesLoadTheTestsRuleWhereverTheySit() {
        val rules = load(HERE.resolve("project-after")).rules
        assertEquals(listOf("components.md", "tests.md"), rulesFor(rules, "src/components/Button.test.tsx"))
        assertEquals(listOf<String>(), rulesFor(rules, "docs/readme.md"))
    }

    @Test
    fun aRuleWithoutPathsAndARuleThatMatchesNothingAreFlagged(@TempDir dir: Path) {
        Files.createDirectories(dir.resolve(".claude/rules"))
        Files.createDirectories(dir.resolve(".claude/commands"))
        Files.writeString(dir.resolve(".claude/commands/review.md"), "review\n")
        Files.writeString(dir.resolve(".claude/settings.json"), "{\"permissions\": {\"deny\": [\"Read(./.env)\"]}}")
        Files.writeString(dir.resolve("files.txt"), "src/a.ts\n")
        Files.writeString(dir.resolve("CLAUDE.md"), "# Notes\n")
        Files.writeString(dir.resolve(".claude/rules/everywhere.md"), "# Always\n")
        Files.writeString(dir.resolve(".claude/rules/typo.md"), "---\npaths:\n  - \"source/**/*.ts\"\n---\n\nx\n")
        assertEquals(listOf("rule-loads-always: everywhere.md", "rule-matches-nothing: typo.md"), audit(dir))
    }
}
