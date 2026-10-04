import static org.junit.jupiter.api.Assertions.*;

import harness.Scripted;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class PromptChainTest {
    @Test
    void aMissingVariableIsAnErrorNotABlank() {
        assertThrows(NoSuchElementException.class, () -> PromptChain.render("Hello {{name}} and {{other}}", Map.of("name", "x")));
    }

    @Test
    void aValueThatLooksLikeAPlaceholderIsLeftAlone() {
        assertEquals("Q: {{b}} / real", PromptChain.render("Q: {{a}} / {{b}}", Map.of("a", "{{b}}", "b", "real")));
    }

    @Test
    void documentsAreNumberedAndCarryTheirSource() {
        String block = PromptChain.documentsBlock(PromptChain.DOCUMENTS);
        assertTrue(block.startsWith("<documents>") && block.contains("<document index=\"2\">") && block.contains("<source>expenses-faq.txt</source>"));
    }

    @Test
    void theLongDocumentsComeFirstAndTheQuestionLast() {
        Scripted.Rig rig = Scripted.client(PromptChain.REPLIES.get(0));
        PromptChain.step(rig.client(), "extract-quotes@2", PromptChain.values("documents", PromptChain.documentsBlock(PromptChain.DOCUMENTS), "question", PromptChain.QUESTION));
        String prompt = rig.http().requests.get(0).at("/messages/0/content").asText();
        assertTrue(prompt.indexOf("<documents>") < prompt.indexOf("<question>") && prompt.stripTrailing().endsWith("</question>"));
    }

    @Test
    void eachStepSendsItsOwnSystemPromptAndEndsOnAUserTurn() {
        Scripted.Rig rig = Scripted.client(PromptChain.REPLIES.toArray());
        PromptChain.step(rig.client(), "extract-quotes@2", PromptChain.values("documents", PromptChain.documentsBlock(PromptChain.DOCUMENTS), "question", PromptChain.QUESTION));
        PromptChain.step(rig.client(), "answer-from-quotes@1", PromptChain.values("quotes", "<quotes></quotes>", "question", PromptChain.QUESTION));
        assertEquals(List.of(PromptChain.TEMPLATES.get("extract-quotes@2").system(), PromptChain.TEMPLATES.get("answer-from-quotes@1").system()),
            rig.http().requests.stream().map(r -> r.get("system").asText()).toList());
        rig.http().requests.forEach(r -> assertEquals("user", r.get("messages").get(r.get("messages").size() - 1).get("role").asText()));
        assertFalse(rig.http().requests.get(0).has("temperature") || rig.http().requests.get(0).has("top_p") || rig.http().requests.get(0).has("top_k"));
    }
}
