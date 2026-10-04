import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FrontDoorsTest {
    @Test
    fun onlyTheDirectApiAndBedrockPutTheModelInTheBody() {
        val models = DOORS.keys.associateWith { orNone(send(it).transport.requests[0], "model") }
        assertEquals(mapOf("anthropic" to "claude-sonnet-5-5", "bedrock" to "anthropic.claude-sonnet-5-5", "vertex" to "none"), models)
    }

    @Test
    fun vertexPutsItsVersionInTheBodyAndSendsNoVersionHeader() {
        val s = send("vertex")
        assertEquals("vertex-2023-10-16", s.transport.requests[0]["anthropic_version"].asText())
        assertFalse("anthropic-version" in s.transport.headers[0])
    }

    @Test
    fun theModelIsInTheVertexUrl() {
        assertTrue("/publishers/anthropic/models/claude-sonnet-5-5:rawPredict" in send("vertex").transport.urls[0])
    }

    @Test
    fun everyDoorReturnsTheSameReplyShape() {
        for (name in DOORS.keys) assertEquals("Paris.", send(name).reply.json().at("/content/0/text").asText())
    }
}
