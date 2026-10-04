import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.List;
import org.junit.jupiter.api.Test;

class SdkRetriesTest {
    private static List<String> retryCounts(ScriptedHttp t) {
        return t.headers.stream().map(h -> h.get("x-stainless-retry-count")).toList();
    }

    @Test
    void twoRetriesRecoverFromTwoOverloads() {
        ScriptedHttp t = Scripted.http(SdkRetries.OVERLOADED, SdkRetries.OVERLOADED, message(List.of(text("Hello."))));
        assertEquals("Hello.", SdkRetries.clientFor(t, 2).messages().create(SdkRetries.params()).content().get(0).asText().text());
        assertEquals(List.of("0", "1", "2"), retryCounts(t));
        assertEquals(2, t.sleeps.size()); // a wait was asked for before each retry
    }

    @Test
    void noRetriesSurfacesTheStatusAndRequestId() {
        ScriptedHttp t = Scripted.http(SdkRetries.OVERLOADED, message(List.of(text("x"))));
        AnthropicServiceException err = assertThrows(AnthropicServiceException.class, () -> SdkRetries.clientFor(t, 0).messages().create(SdkRetries.params()));
        assertEquals(529, err.statusCode());
        assertEquals("req_illustrative_0529", err.headers().values("request-id").get(0));
        assertEquals(1, t.requests.size());
    }

    @Test
    void a400IsNotRetried() {
        ScriptedHttp t = Scripted.http(SdkRetries.BAD_REQUEST, message(List.of(text("x"))));
        assertThrows(BadRequestException.class, () -> SdkRetries.clientFor(t, 2).messages().create(SdkRetries.params()));
        assertEquals(1, t.requests.size());
    }

    @Test
    void theSdkRetriesASpendCap429AlthoughItCannotSucceed() {
        ScriptedHttp t = Scripted.http(SdkRetries.SPEND_CAP, SdkRetries.SPEND_CAP, SdkRetries.SPEND_CAP);
        assertThrows(RateLimitException.class, () -> SdkRetries.clientFor(t, 2).messages().create(SdkRetries.params()));
        assertEquals(3, t.requests.size());
    }
}
