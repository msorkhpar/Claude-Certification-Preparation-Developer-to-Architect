import harness.Scripted
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchRoundTripTest {
    @Test
    fun createPostsOneRequestPerTicketWithItsCustomId() {
        val transport = Scripted.http(*script().toTypedArray())
        Scripted.clientOn(transport, 0).messages().batches().create(requests())
        assertTrue(transport.urls[0].endsWith("/v1/messages/batches"))
        assertEquals(TICKETS.keys.toList(), transport.requests[0]["requests"].map { it["custom_id"].asText() })
    }

    @Test
    fun resultsComeBackInADifferentOrderThanTheRequests() {
        val order = results(Scripted.clientOn(Scripted.http(script()[3]), 0), "msgbatch_illustrative").keys.toList()
        assertEquals(listOf("t-3", "t-1", "t-4", "t-2"), order)
        assertNotEquals(TICKETS.keys.toList(), order)
    }

    @Test
    fun eachNonSuccessResultHasItsOwnType() {
        val results = results(Scripted.clientOn(Scripted.http(script()[3]), 0), "msgbatch_illustrative")
        assertEquals(mapOf("t-3" to "succeeded", "t-1" to "succeeded", "t-4" to "errored", "t-2" to "expired"), results.mapValues { it.value.kind })
        assertEquals("invalid_request_error", results.getValue("t-4").detail)
    }
}
