import com.fasterxml.jackson.databind.ObjectMapper
import com.anthropic.errors.RateLimitException
import harness.Scripted
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawVsSdkTest {
    @Test
    fun rawAndSdkSendTheSameRequestContract() {
        val raw = Scripted.http(OK)
        val sdk = Scripted.http(OK)
        rawCall(raw)
        sdkCall(sdk)
        assertEquals(raw.urls, sdk.urls)
        assertEquals(listOf("https://api.anthropic.com/v1/messages"), raw.urls)
        assertEquals(ObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(PAYLOAD), raw.requests[0])
        assertEquals(raw.requests[0], sdk.requests[0])
        assertEquals("2023-06-01", raw.headers[0]["anthropic-version"])
        assertEquals(raw.headers[0]["anthropic-version"], sdk.headers[0]["anthropic-version"])
        assertEquals(raw.headers[0]["x-api-key"], sdk.headers[0]["x-api-key"])
    }

    @Test
    fun theSdkAddsHeadersTheRawCallDoesNotSend() {
        val raw = Scripted.http(OK)
        val sdk = Scripted.http(OK)
        rawCall(raw)
        sdkCall(sdk)
        assertTrue("x-stainless-lang" in sdk.headers[0])
        assertFalse("x-stainless-lang" in raw.headers[0])
    }

    @Test
    fun a429IsAStatusForRawCodeAndATypedErrorForTheSdk() {
        assertEquals(429, rawCall(Scripted.http(LIMITED)).status())
        val error = assertThrows(RateLimitException::class.java) { sdkCall(Scripted.http(LIMITED)) }
        assertEquals("req_illustrative_0001", error.headers().values("request-id")[0])
    }
}
