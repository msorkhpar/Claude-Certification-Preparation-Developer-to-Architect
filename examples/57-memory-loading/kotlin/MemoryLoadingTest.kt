import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemoryLoadingTest {
    private val tree = setOf("CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md", "AGENTS.md")

    @Test
    fun globsStayInOneFolderUnlessTheySayOtherwise() {
        assertTrue(globMatch("*.md", "README.md"))
        assertFalse(globMatch("*.md", "docs/guide.md"))
        assertTrue(globMatch("**/*.ts", "a.ts"))
        assertTrue(globMatch("**/*.ts", "a/b/c.ts"))
        assertFalse(globMatch("**/*.ts", "a/b/c.tsx"))
        assertTrue(globMatch("src/**/*", "src/a/b.py"))
        assertFalse(globMatch("src/**/*", "lib/a.py"))
        assertTrue(globMatch("src/components/*.tsx", "src/components/A.tsx"))
        assertFalse(globMatch("src/components/*.tsx", "src/components/x/A.tsx"))
    }

    @Test
    fun bracesExpandAndMultiply() {
        assertEquals(listOf("src/*.ts", "src/*.tsx"), expandBraces("src/*.{ts,tsx}"))
        assertEquals(8, expandBraces("{a,b}/{c,d}/*.{ts,tsx}").size)
        assertTrue(globMatch("src/**/*.{ts,tsx}", "src/ui/x.tsx"))
    }

    @Test
    fun aRuleWithoutPathsIsAlwaysLoadedAndAScopedOneWaitsForAMatch() {
        val rules = linkedMapOf("commit.md" to null, "testing.md" to listOf("**/*.test.tsx"), "terraform.md" to listOf("terraform/**/*"))
        assertEquals(listOf("commit.md"), rulesLoaded(rules, emptyList()))
        assertEquals(listOf("commit.md", "testing.md"), rulesLoaded(rules, listOf("a/b/Button.test.tsx")))
        assertEquals(listOf("commit.md", "testing.md", "terraform.md"), rulesLoaded(rules, listOf("terraform/prod/main.tf", "x/Y.test.tsx")))
    }

    @Test
    fun launchLoadsTheFoldersAboveAndOnDemandLoadsTheFoldersBelow() {
        assertEquals(listOf("CLAUDE.md", "CLAUDE.local.md"), launchFiles(tree, ""))
        assertEquals(listOf("CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md"), launchFiles(tree, "web"))
        assertEquals(listOf("web/CLAUDE.md", "web/ui/CLAUDE.md"), onDemandFiles(tree, "", listOf("web/ui/Button.tsx")))
        assertEquals(listOf("web/ui/CLAUDE.md"), onDemandFiles(tree, "web", listOf("web/ui/Button.tsx", "api/x.py")))
    }

    @Test
    fun agentsMdIsReadOnlyWhenThereIsNoClaudeMd() {
        assertTrue(agentsMdRead(setOf("AGENTS.md")))
        assertFalse(agentsMdRead(tree))
        assertFalse(agentsMdRead(setOf("AGENTS.md", "CLAUDE.local.md")))
    }

    @Test
    fun importsFollowFourHopsAndSkipCodeSpans() {
        val texts = mapOf(
            "CLAUDE.md" to "See @docs/a.md and `@code` and @missing.md", "docs/a.md" to "@b.md", "docs/b.md" to "@c.md", "docs/c.md" to "@d.md", "docs/d.md" to "@e.md", "docs/e.md" to "end",
        )
        assertEquals(listOf("docs/a.md", "docs/b.md", "docs/c.md", "docs/d.md"), importsOf("CLAUDE.md", texts))
        assertEquals(emptyList<String>(), importsOf("CLAUDE.md", mapOf("CLAUDE.md" to "```\n@docs/a.md\n```", "docs/a.md" to "x")))
    }

    @Test
    fun anImportSavesNoContext() {
        val texts = mapOf("CLAUDE.md" to "line\n@docs/a.md", "docs/a.md" to "1\n2\n3")
        assertEquals(5, contextLines(listOf("CLAUDE.md") + importsOf("CLAUDE.md", texts), texts))
    }

    @Test
    fun anImportThatNamesNoFileIsReported() {
        val texts = mapOf("CLAUDE.md" to "@docs/a.md and @docs/typo.md and `@code`", "docs/a.md" to "x")
        assertEquals(listOf("docs/typo.md"), unresolvedImports("CLAUDE.md", texts))
    }
}
