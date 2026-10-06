import harness.Scripted
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptChainTest {
    @Test
    fun aMissingVariableIsAnErrorNotABlank() {
        assertThrows(NoSuchElementException::class.java) { render("Hello {{name}} and {{other}}", mapOf("name" to "x")) }
    }

    @Test
    fun aValueThatLooksLikeAPlaceholderIsLeftAlone() {
        assertEquals("Q: {{b}} / real", render("Q: {{a}} / {{b}}", mapOf("a" to "{{b}}", "b" to "real")))
    }

    @Test
    fun documentsAreNumberedAndCarryTheirSource() {
        val block = documentsBlock(DOCUMENTS)
        assertTrue(block.startsWith("<documents>") && "<document index=\"2\">" in block && "<source>expenses-faq.txt</source>" in block)
    }

    @Test
    fun theLongDocumentsComeFirstAndTheQuestionLast() {
        val rig = Scripted.client(REPLIES[0])
        step(rig.client(), "extract-quotes@2", mapOf("documents" to documentsBlock(DOCUMENTS), "question" to QUESTION))
        val prompt = rig.http().requests[0].at("/messages/0/content").asText()
        assertTrue(prompt.indexOf("<documents>") < prompt.indexOf("<question>") && prompt.trimEnd().endsWith("</question>"))
    }

    @Test
    fun eachStepSendsItsOwnSystemPromptAndEndsOnAUserTurn() {
        val rig = Scripted.client(*REPLIES.toTypedArray())
        step(rig.client(), "extract-quotes@2", mapOf("documents" to documentsBlock(DOCUMENTS), "question" to QUESTION))
        step(rig.client(), "answer-from-quotes@1", mapOf("quotes" to "<quotes></quotes>", "question" to QUESTION))
        assertEquals(listOf(TEMPLATES.getValue("extract-quotes@2").system, TEMPLATES.getValue("answer-from-quotes@1").system), rig.http().requests.map { it["system"].asText() })
        assertTrue(rig.http().requests.all { r -> r["messages"].last()["role"].asText() == "user" })
        assertFalse(listOf("temperature", "top_p", "top_k").any { rig.http().requests[0].has(it) })
    }
}
