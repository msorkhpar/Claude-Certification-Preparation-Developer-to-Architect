package harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One scripted HTTP reply: a status, headers and a body. The static factories cover what the course scripts:
 * a JSON body, a raw text body (such as a .jsonl batch result) and a server-sent-event stream.
 */
public record Reply(int status, Map<String, String> headers, byte[] body) {
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The body parsed as JSON. */
    public com.fasterxml.jackson.databind.JsonNode json() {
        try {
            return JSON.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The body as text. */
    public String text() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /** A JSON reply; `headers` are name, value, name, value ... */
    public static Reply json(int status, Object body, String... headers) {
        try {
            return new Reply(status, headerMap("content-type", "application/json", headers), JSON.writeValueAsBytes(body));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A reply with a raw text body and its own content type. */
    public static Reply text(int status, String contentType, String body, String... headers) {
        return new Reply(status, headerMap("content-type", contentType, headers), body.getBytes(StandardCharsets.UTF_8));
    }

    /** A 200 streaming reply: one named server-sent event per map, as the API frames them (`event: <type>` then `data: <json>`). */
    public static Reply sse(List<? extends Map<String, ?>> events, String... headers) {
        StringBuilder body = new StringBuilder();
        try {
            for (Map<String, ?> e : events) body.append("event: ").append(e.get("type")).append("\ndata: ").append(JSON.writeValueAsString(e)).append("\n\n");
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        return text(200, "text/event-stream", body.toString(), headers);
    }

    private static Map<String, String> headerMap(String name, String value, String... more) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(name, value);
        for (int i = 0; i + 1 < more.length; i += 2) out.put(more[i].toLowerCase(), more[i + 1]);
        return out;
    }
}
