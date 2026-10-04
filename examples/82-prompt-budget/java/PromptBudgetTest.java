import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptBudgetTest {
    private static final Map<String, String> VARS = Map.of("customer", "Ana", "tier", "gold", "question", "Q");

    @Test
    void tokensAreTheCeilingOfCharactersOverFour() {
        assertEquals(List.of(0, 1, 1, 2), List.of("", "a", "abcd", "abcde").stream().map(PromptBudget::tokens).toList());
    }

    @Test
    void staticModulesComeFirstAndTheBreakpointFollowsTheLastOne() {
        List<PromptBudget.Module> m = PromptBudget.MODULES;
        PromptBudget.Prompt prompt = PromptBudget.assemble(List.of(m.get(2), m.get(0), m.get(3), m.get(1)), VARS);
        assertEquals(List.of("role", "policy", "customer", "question"), prompt.blocks().stream().map(PromptBudget.Module::name).toList());
        assertEquals("policy", prompt.blocks().get(prompt.breakpoint()).name());
    }

    @Test
    void dynamicVariablesAreFilledAndStaticTextIsLeftAlone() {
        PromptBudget.Prompt prompt = PromptBudget.assemble(PromptBudget.MODULES, VARS);
        assertEquals("Customer: Ana. Tier: gold.", prompt.blocks().get(2).text());
        assertFalse(prompt.blocks().get(1).text().contains("{"));
    }

    @Test
    void aPrefixUnderTheMinimumGetsNoBreakpoint() {
        List<PromptBudget.Module> without = new ArrayList<>(PromptBudget.MODULES);
        without.remove(1);
        PromptBudget.Prompt prompt = PromptBudget.assemble(without, VARS);
        assertTrue(prompt.prefixTokens() < PromptBudget.MIN_CACHEABLE);
        assertNull(prompt.breakpoint());
    }

    @Test
    void thePrefixSurvivesADynamicChangeAndBreaksOnAStaticOne() {
        PromptBudget.Prompt base = PromptBudget.assemble(PromptBudget.MODULES, VARS);
        PromptBudget.Prompt other = PromptBudget.assemble(PromptBudget.MODULES, Map.of("customer", "Ben", "tier", "basic", "question", "Other"));
        assertEquals(PromptBudget.cachedPrefix(base), PromptBudget.cachedPrefix(other));
        assertNotEquals("", PromptBudget.cachedPrefix(base));
        List<PromptBudget.Module> edited = new ArrayList<>(PromptBudget.MODULES);
        edited.set(0, new PromptBudget.Module("role", true, edited.get(0).text() + " Extra."));
        assertNotEquals(PromptBudget.cachedPrefix(base), PromptBudget.cachedPrefix(PromptBudget.assemble(edited, VARS)));
    }
}
