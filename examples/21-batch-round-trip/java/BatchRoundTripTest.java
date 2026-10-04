import static org.junit.jupiter.api.Assertions.*;

import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BatchRoundTripTest {
    @Test
    void createPostsOneRequestPerTicketWithItsCustomId() {
        ScriptedHttp transport = Scripted.http(BatchRoundTrip.script().toArray());
        Scripted.clientOn(transport, 0).messages().batches().create(BatchRoundTrip.requests());
        assertTrue(transport.urls.get(0).endsWith("/v1/messages/batches"));
        List<String> ids = new ArrayList<>();
        transport.requests.get(0).get("requests").forEach(r -> ids.add(r.get("custom_id").asText()));
        assertEquals(new ArrayList<>(BatchRoundTrip.TICKETS.keySet()), ids);
    }

    @Test
    void resultsComeBackInADifferentOrderThanTheRequests() {
        ScriptedHttp transport = Scripted.http(BatchRoundTrip.script().get(3));
        Map<String, BatchRoundTrip.Outcome> results = BatchRoundTrip.results(Scripted.clientOn(transport, 0), "msgbatch_illustrative");
        List<String> order = new ArrayList<>(results.keySet());
        assertEquals(List.of("t-3", "t-1", "t-4", "t-2"), order);
        assertNotEquals(new ArrayList<>(BatchRoundTrip.TICKETS.keySet()), order);
    }

    @Test
    void eachNonSuccessResultHasItsOwnType() {
        ScriptedHttp transport = Scripted.http(BatchRoundTrip.script().get(3));
        Map<String, BatchRoundTrip.Outcome> results = BatchRoundTrip.results(Scripted.clientOn(transport, 0), "msgbatch_illustrative");
        assertEquals(Map.of("t-3", "succeeded", "t-1", "succeeded", "t-4", "errored", "t-2", "expired"),
            Map.of("t-3", results.get("t-3").kind(), "t-1", results.get("t-1").kind(), "t-4", results.get("t-4").kind(), "t-2", results.get("t-2").kind()));
        assertEquals("invalid_request_error", results.get("t-4").detail());
    }
}
