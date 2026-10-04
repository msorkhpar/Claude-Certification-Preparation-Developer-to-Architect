import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrontDoorsTest {
    @Test
    void onlyTheDirectApiAndBedrockPutTheModelInTheBody() {
        Map<String, String> models = new LinkedHashMap<>();
        for (String name : FrontDoors.DOORS.keySet()) models.put(name, FrontDoors.orNone(FrontDoors.send(name).transport().requests.get(0), "model"));
        assertEquals(Map.of("anthropic", "claude-sonnet-5-5", "bedrock", "anthropic.claude-sonnet-5-5", "vertex", "none"), models);
    }

    @Test
    void vertexPutsItsVersionInTheBodyAndSendsNoVersionHeader() {
        FrontDoors.Sent s = FrontDoors.send("vertex");
        assertEquals("vertex-2023-10-16", s.transport().requests.get(0).get("anthropic_version").asText());
        assertFalse(s.transport().headers.get(0).containsKey("anthropic-version"));
    }

    @Test
    void theModelIsInTheVertexUrl() {
        assertTrue(FrontDoors.send("vertex").transport().urls.get(0).contains("/publishers/anthropic/models/claude-sonnet-5-5:rawPredict"));
    }

    @Test
    void everyDoorReturnsTheSameReplyShape() {
        for (String name : FrontDoors.DOORS.keySet()) assertEquals("Paris.", FrontDoors.send(name).reply().json().at("/content/0/text").asText());
    }
}
