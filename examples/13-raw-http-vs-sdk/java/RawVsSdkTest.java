import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.errors.RateLimitException;
import com.fasterxml.jackson.databind.ObjectMapper;
import harness.Scripted;
import harness.ScriptedHttp;
import org.junit.jupiter.api.Test;

class RawVsSdkTest {
    @Test
    void rawAndSdkSendTheSameRequestContract() {
        ScriptedHttp raw = Scripted.http(RawVsSdk.OK);
        ScriptedHttp sdk = Scripted.http(RawVsSdk.OK);
        RawVsSdk.rawCall(raw);
        RawVsSdk.sdkCall(sdk);
        assertEquals(raw.urls, sdk.urls);
        assertEquals(java.util.List.of("https://api.anthropic.com/v1/messages"), raw.urls);
        assertEquals(new ObjectMapper().valueToTree(RawVsSdk.PAYLOAD), raw.requests.get(0));
        assertEquals(raw.requests.get(0), sdk.requests.get(0));
        assertEquals("2023-06-01", raw.headers.get(0).get("anthropic-version"));
        assertEquals(raw.headers.get(0).get("anthropic-version"), sdk.headers.get(0).get("anthropic-version"));
        assertEquals(raw.headers.get(0).get("x-api-key"), sdk.headers.get(0).get("x-api-key"));
    }

    @Test
    void theSdkAddsHeadersTheRawCallDoesNotSend() {
        ScriptedHttp raw = Scripted.http(RawVsSdk.OK);
        ScriptedHttp sdk = Scripted.http(RawVsSdk.OK);
        RawVsSdk.rawCall(raw);
        RawVsSdk.sdkCall(sdk);
        assertTrue(sdk.headers.get(0).containsKey("x-stainless-lang"));
        assertFalse(raw.headers.get(0).containsKey("x-stainless-lang"));
    }

    @Test
    void a429IsAStatusForRawCodeAndATypedErrorForTheSdk() {
        assertEquals(429, RawVsSdk.rawCall(Scripted.http(RawVsSdk.LIMITED)).status());
        RateLimitException error = assertThrows(RateLimitException.class, () -> RawVsSdk.sdkCall(Scripted.http(RawVsSdk.LIMITED)));
        assertEquals("req_illustrative_0001", error.headers().values("request-id").get(0));
    }
}
