import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.ScriptedHttp;
import harness.Scripted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
 *
 * <p>Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
 * is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
 * reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
 */
public final class FrontDoors {
    private static final System.Logger LOG = System.getLogger(FrontDoors.class.getName());
    static final List<Map<String, Object>> MESSAGES = List.of(map("role", "user", "content", "Capital of France?"));
    static final String PROJECT = "example-project", REGION = "us-east-1";

    /** One front door: where the request goes, its headers and its body. */
    record Door(String url, Map<String, String> headers, Map<String, Object> body) {}

    static final Map<String, Door> DOORS = new LinkedHashMap<>();

    static {
        DOORS.put("anthropic", new Door("https://api.anthropic.com/v1/messages",
            Map.of("anthropic-version", "2023-06-01", "content-type", "application/json"),
            map("model", "claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES)));
        DOORS.put("bedrock", new Door("https://bedrock-mantle." + REGION + ".api.aws/anthropic/v1/messages",
            Map.of("anthropic-version", "2023-06-01", "content-type", "application/json"),
            map("model", "anthropic.claude-sonnet-5-5", "max_tokens", 64, "messages", MESSAGES)));
        DOORS.put("vertex", new Door("https://aiplatform.googleapis.com/v1/projects/" + PROJECT + "/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
            Map.of("content-type", "application/json"),
            map("anthropic_version", "vertex-2023-10-16", "max_tokens", 64, "messages", MESSAGES)));
    }

    /** What went out and what came back for one door. */
    record Sent(ScriptedHttp transport, Reply reply) {}

    static Sent send(String name) {
        Door door = DOORS.get(name);
        ScriptedHttp transport = Scripted.http(message(List.of(text("Paris."))));
        Reply reply = transport.send("POST", door.url(), door.headers(), door.body());
        return new Sent(transport, reply);
    }

    static String orNone(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "none";
    }

    public static void main(String[] args) {
        for (String name : DOORS.keySet()) {
            Sent s = send(name);
            JsonNode sent = s.transport().requests.get(0);
            Map<String, String> headers = s.transport().headers.get(0);
            System.out.println(String.format("%-9s %s", name, s.transport().urls.get(0)));
            System.out.println("          version header: " + headers.getOrDefault("anthropic-version", "none") + " | body model: " + orNone(sent, "model")
                + " | body version: " + orNone(sent, "anthropic_version"));
            System.out.println("          reply: " + s.reply().status() + " '" + s.reply().json().at("/content/0/text").asText() + "' (same parser for every door)");
        }
    }
}
