import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.RateLimitException
import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import harness.ScriptedHttp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SdkRetriesTest {
    private fun retryCounts(t: ScriptedHttp) = t.headers.map { it["x-stainless-retry-count"] }

    @Test
    fun twoRetriesRecoverFromTwoOverloads() {
        val t = Scripted.http(OVERLOADED, OVERLOADED, message(listOf(text("Hello."))))
        assertEquals("Hello.", clientFor(t, 2).messages().create(params()).content()[0].asText().text())
        assertEquals(listOf("0", "1", "2"), retryCounts(t))
        assertEquals(2, t.sleeps.size) // a wait was asked for before each retry
    }

    @Test
    fun noRetriesSurfacesTheStatusAndRequestId() {
        val t = Scripted.http(OVERLOADED, message(listOf(text("x"))))
        val err = assertThrows(AnthropicServiceException::class.java) { clientFor(t, 0).messages().create(params()) }
        assertEquals(529, err.statusCode())
        assertEquals("req_illustrative_0529", err.headers().values("request-id")[0])
        assertEquals(1, t.requests.size)
    }

    @Test
    fun a400IsNotRetried() {
        val t = Scripted.http(BAD_REQUEST, message(listOf(text("x"))))
        assertThrows(BadRequestException::class.java) { clientFor(t, 2).messages().create(params()) }
        assertEquals(1, t.requests.size)
    }

    @Test
    fun theSdkRetriesASpendCap429AlthoughItCannotSucceed() {
        val t = Scripted.http(SPEND_CAP, SPEND_CAP, SPEND_CAP)
        assertThrows(RateLimitException::class.java) { clientFor(t, 2).messages().create(params()) }
        assertEquals(3, t.requests.size)
    }
}
