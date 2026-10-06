import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MemoryLoadingTest {
    private static final Set<String> TREE = Set.of("CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md", "AGENTS.md");

    @Test
    void globsStayInOneFolderUnlessTheySayOtherwise() {
        assertTrue(MemoryLoading.globMatch("*.md", "README.md"));
        assertFalse(MemoryLoading.globMatch("*.md", "docs/guide.md"));
        assertTrue(MemoryLoading.globMatch("**/*.ts", "a.ts"));
        assertTrue(MemoryLoading.globMatch("**/*.ts", "a/b/c.ts"));
        assertFalse(MemoryLoading.globMatch("**/*.ts", "a/b/c.tsx"));
        assertTrue(MemoryLoading.globMatch("src/**/*", "src/a/b.py"));
        assertFalse(MemoryLoading.globMatch("src/**/*", "lib/a.py"));
        assertTrue(MemoryLoading.globMatch("src/components/*.tsx", "src/components/A.tsx"));
        assertFalse(MemoryLoading.globMatch("src/components/*.tsx", "src/components/x/A.tsx"));
    }

    @Test
    void bracesExpandAndMultiply() {
        assertEquals(List.of("src/*.ts", "src/*.tsx"), MemoryLoading.expandBraces("src/*.{ts,tsx}"));
        assertEquals(8, MemoryLoading.expandBraces("{a,b}/{c,d}/*.{ts,tsx}").size());
        assertTrue(MemoryLoading.globMatch("src/**/*.{ts,tsx}", "src/ui/x.tsx"));
    }

    @Test
    void aRuleWithoutPathsIsAlwaysLoadedAndAScopedOneWaitsForAMatch() {
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("commit.md", null);
        rules.put("testing.md", List.of("**/*.test.tsx"));
        rules.put("terraform.md", List.of("terraform/**/*"));
        assertEquals(List.of("commit.md"), MemoryLoading.rulesLoaded(rules, List.of()));
        assertEquals(List.of("commit.md", "testing.md"), MemoryLoading.rulesLoaded(rules, List.of("a/b/Button.test.tsx")));
        assertEquals(List.of("commit.md", "testing.md", "terraform.md"), MemoryLoading.rulesLoaded(rules, List.of("terraform/prod/main.tf", "x/Y.test.tsx")));
    }

    @Test
    void launchLoadsTheFoldersAboveAndOnDemandLoadsTheFoldersBelow() {
        assertEquals(List.of("CLAUDE.md", "CLAUDE.local.md"), MemoryLoading.launchFiles(TREE, ""));
        assertEquals(List.of("CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md"), MemoryLoading.launchFiles(TREE, "web"));
        assertEquals(List.of("web/CLAUDE.md", "web/ui/CLAUDE.md"), MemoryLoading.onDemandFiles(TREE, "", List.of("web/ui/Button.tsx")));
        assertEquals(List.of("web/ui/CLAUDE.md"), MemoryLoading.onDemandFiles(TREE, "web", List.of("web/ui/Button.tsx", "api/x.py")));
    }

    @Test
    void agentsMdIsReadOnlyWhenThereIsNoClaudeMd() {
        assertTrue(MemoryLoading.agentsMdRead(Set.of("AGENTS.md")));
        assertFalse(MemoryLoading.agentsMdRead(TREE));
        assertFalse(MemoryLoading.agentsMdRead(Set.of("AGENTS.md", "CLAUDE.local.md")));
    }

    @Test
    void importsFollowFourHopsAndSkipCodeSpans() {
        Map<String, String> texts = MemoryLoading.texts("CLAUDE.md", "See @docs/a.md and `@code` and @missing.md", "docs/a.md", "@b.md", "docs/b.md", "@c.md", "docs/c.md", "@d.md", "docs/d.md", "@e.md", "docs/e.md", "end");
        assertEquals(List.of("docs/a.md", "docs/b.md", "docs/c.md", "docs/d.md"), MemoryLoading.importsOf("CLAUDE.md", texts));
        assertEquals(List.of(), MemoryLoading.importsOf("CLAUDE.md", MemoryLoading.texts("CLAUDE.md", "```\n@docs/a.md\n```", "docs/a.md", "x")));
    }

    @Test
    void anImportSavesNoContext() {
        Map<String, String> texts = MemoryLoading.texts("CLAUDE.md", "line\n@docs/a.md", "docs/a.md", "1\n2\n3");
        List<String> paths = new java.util.ArrayList<>(List.of("CLAUDE.md"));
        paths.addAll(MemoryLoading.importsOf("CLAUDE.md", texts));
        assertEquals(5, MemoryLoading.contextLines(paths, texts));
    }

    @Test
    void anImportThatNamesNoFileIsReported() {
        Map<String, String> texts = MemoryLoading.texts("CLAUDE.md", "@docs/a.md and @docs/typo.md and `@code`", "docs/a.md", "x");
        assertEquals(List.of("docs/typo.md"), MemoryLoading.unresolvedImports("CLAUDE.md", texts));
    }
}
