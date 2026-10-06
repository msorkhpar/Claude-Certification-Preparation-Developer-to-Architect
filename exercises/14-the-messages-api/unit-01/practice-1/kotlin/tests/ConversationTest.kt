import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConversationTest {
    @Suppress("UNCHECKED_CAST")
    private fun answer(text: String, stop: String = "end_turn", input: Long = 10, output: Long = 5): Map<String, Any?> =
        Json.parse(
            """{"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5",
            "content":[{"type":"text","text":"$text"}],"stop_reason":"$stop","stop_sequence":null,
            "usage":{"input_tokens":$input,"output_tokens":$output}}""",
        ) as Map<String, Any?>

    /** A scripted send: replies in order (a RuntimeException is thrown); keeps the bodies it was given, without copying. */
    private class Script(vararg replies: Any) : Send {
        val replies = replies.toMutableList()
        val bodies = mutableListOf<Map<String, Any?>>()

        @Suppress("UNCHECKED_CAST")
        override fun send(body: Map<String, Any?>): Map<String, Any?> {
            bodies.add(body) // no copy: a client that shares its list with us shows it here
            val item = replies.removeAt(0)
            if (item is RuntimeException) throw item
            return item as Map<String, Any?>
        }
    }

    private fun errorOf(call: () -> Unit): RuntimeException? =
        try {
            call()
            null
        } catch (e: RuntimeException) {
            e
        }

    @Suppress("UNCHECKED_CAST")
    private fun messages(body: Map<String, Any?>) = body["messages"] as List<Map<String, Any?>>

    private fun roles(turns: List<Map<String, Any?>>) = turns.map { it["role"] as String }

    @Test
    fun m1_everyRequestCarriesTheWholeHistoryInOrder() {
        val send = Script(answer("Paris."), answer("Since 987."), answer("The Seine."))
        val chat = Conversation(send, "claude-sonnet-5-5", 64)
        assertEquals("Paris.", chat.say("Capital of France?").text)
        chat.say("Since when?")
        chat.say("Its river?")
        assertEquals(listOf(1, 3, 5), send.bodies.map { messages(it).size })
        assertEquals(listOf("user", "assistant", "user", "assistant", "user"), roles(messages(send.bodies[2])))
        assertEquals(Json.parse("""{"role":"assistant","content":[{"type":"text","text":"Paris."}]}"""), messages(send.bodies[1])[1])
        assertTrue(send.bodies[0]["model"] == "claude-sonnet-5-5" && send.bodies[0]["max_tokens"] == 64L)
    }

    @Test
    fun e1_usageAddsUpOverTheTurns() {
        val chat = Conversation(Script(answer("a", input = 12, output = 4), answer("b", input = 30, output = 9)), "m", 64)
        chat.say("one")
        chat.say("two")
        assertEquals(mapOf("input_tokens" to 42L, "output_tokens" to 13L), chat.totals())
    }

    @Test
    fun e2_aFailedCallLeavesNoDanglingUserTurn() {
        val send = Script(answer("ok"), IllegalStateException("overloaded"), answer("fine"))
        val chat = Conversation(send, "m", 64)
        chat.say("first")
        val err = errorOf { chat.say("second") }
        assertTrue(err is IllegalStateException, "got $err")
        assertEquals(listOf("user", "assistant"), roles(chat.history()))
        chat.say("second again")
        assertEquals(listOf("user", "assistant", "user"), roles(messages(send.bodies[2])))
    }

    @Test
    fun e3_stopReasonIsReportedAndMaxTokensMarksTheReplyTruncated() {
        val chat = Conversation(Script(answer("Complete."), answer("Cut o", "max_tokens"), answer("done", "stop_sequence")), "m", 64)
        val first = chat.say("a")
        val second = chat.say("b")
        val third = chat.say("c")
        assertEquals(listOf("end_turn", false), listOf(first.stopReason, first.truncated))
        assertEquals(listOf("max_tokens", true, "Cut o"), listOf(second.stopReason, second.truncated, second.text))
        assertEquals(listOf("stop_sequence", false), listOf(third.stopReason, third.truncated))
    }

    @Test
    fun e4_systemIsATopLevelFieldAndStopSequencesArePassedOn() {
        val send = Script(answer("x"), answer("y"))
        Conversation(send, "m", 8, "Be brief.", listOf("END")).say("hi")
        assertEquals(1, send.bodies.size)
        val body = send.bodies[0]
        assertEquals("Be brief.", body["system"])
        assertEquals(listOf("END"), body["stop_sequences"])
        assertTrue(roles(messages(body)).none { it == "system" })
        Conversation(send, "m", 8).say("hi")
        assertEquals(2, send.bodies.size)
        assertFalse(send.bodies[1].containsKey("system") || send.bodies[1].containsKey("stop_sequences"))
    }

    @Test
    fun e5_eachRequestIsASnapshotAndHistoryIsACopy() {
        val send = Script(answer("a"), answer("b"))
        val chat = Conversation(send, "m", 8)
        chat.say("one")
        chat.say("two")
        assertEquals(2, send.bodies.size)
        assertEquals(1, messages(send.bodies[0]).size) // a later turn must not change an earlier request
        chat.history().add(mutableMapOf("role" to "user", "content" to "injected"))
        chat.history()[0]["content"] = "changed"
        assertEquals(4, chat.history().size)
        assertEquals("one", chat.history()[0]["content"])
    }

    @Test
    fun e6_aBlankTurnIsRefusedBeforeAnythingIsSent() {
        val send = Script(answer("never used"))
        val chat = Conversation(send, "m", 8)
        for (blank in listOf("", "   ", "\n")) {
            assertTrue(errorOf { chat.say(blank) } is IllegalArgumentException, blank)
        }
        assertEquals(0, send.bodies.size)
        assertEquals(0, chat.history().size)
    }
}
