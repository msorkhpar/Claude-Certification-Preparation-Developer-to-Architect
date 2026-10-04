import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harness.Scripted;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured outputs plus the checks a schema cannot make, against a scripted model.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
 */
public final class StructuredExtraction {
    static final String MODEL = "claude-sonnet-5-5";
    private static final ObjectMapper JSON = new ObjectMapper();
    static final JsonNode LOCAL_SCHEMA = Scripted.tree("""
        {"type": "object",
         "properties": {"vendor": {"type": "string"}, "total": {"type": "number", "minimum": 0},
                        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]}, "evidence": {"type": "string"}},
         "required": ["vendor", "total", "currency", "evidence"],
         "additionalProperties": false}""");
    static final List<String> UNSUPPORTED = List.of("minimum", "maximum", "multipleOf", "minLength", "maxLength");
    static final List<String> CURRENCIES = List.of("USD", "EUR", "GBP");

    /** The schema without the constraints that structured outputs reject; they move to the field's description. */
    static ObjectNode forApi(JsonNode schema) {
        ObjectNode out = schema.deepCopy();
        walk(out);
        return out;
    }

    private static void walk(ObjectNode node) {
        List<String> notes = new ArrayList<>();
        for (String k : UNSUPPORTED) if (node.has(k)) notes.add(k + " " + node.remove(k).asText());
        if (!notes.isEmpty()) node.put("description", ((node.has("description") ? node.get("description").asText() : "") + " (" + String.join(", ", notes) + ")").strip());
        if (node.has("properties")) node.get("properties").forEach(sub -> walk((ObjectNode) sub));
    }

    /** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
    static List<String> problems(Map<String, Object> value, String document) {
        List<String> found = new ArrayList<>();
        if (!(value.get("total") instanceof Number n) || n.doubleValue() < 0) found.add("$.total: must be a number of at least 0");
        if (!CURRENCIES.contains(value.get("currency"))) found.add("$.currency: must be one of USD, EUR, GBP, not " + py(value.get("currency")));
        if (!(value.get("evidence") instanceof String e) || !document.contains(e)) found.add("$.evidence: is not found in the document");
        return found;
    }

    /** Structured outputs may change the capital letters of an enum value; compare without them. */
    static Map<String, Object> normaliseEnum(Map<String, Object> value) {
        if (value.get("currency") instanceof String c) {
            for (String allowed : CURRENCIES) {
                if (allowed.equalsIgnoreCase(c)) {
                    Map<String, Object> out = new LinkedHashMap<>(value);
                    out.put("currency", allowed);
                    return out;
                }
            }
        }
        return value;
    }

    private static OutputConfig outputConfig() {
        Map<String, JsonValue> schema = new LinkedHashMap<>();
        forApi(LOCAL_SCHEMA).fields().forEachRemaining(e -> schema.put(e.getKey(), JsonValue.from(JSON.convertValue(e.getValue(), Object.class))));
        return OutputConfig.builder().format(JsonOutputFormat.builder().schema(JsonOutputFormat.Schema.builder().additionalProperties(schema).build()).build()).build();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> extract(AnthropicClient client, String document, int maxAttempts) {
        MessageCreateParams.Builder messages = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).outputConfig(outputConfig())
            .addUserMessage("Extract the invoice data.\n<document>\n" + document + "\n</document>");
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Message reply = client.messages().create(messages.build());
            String stop = reply.stopReason().get().asString();
            if (stop.equals("refusal") || stop.equals("max_tokens")) {
                return map("status", stop.equals("refusal") ? "refused" : "truncated", "attempts", attempt);
            }
            String raw = reply.content().get(0).asText().text();
            Map<String, Object> value;
            try {
                value = normaliseEnum(JSON.readValue(raw, LinkedHashMap.class));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            errors = problems(value, document);
            if (errors.isEmpty()) return map("status", "ok", "attempts", attempt, "value", value);
            messages.addAssistantMessage(raw).addUserMessage("Rejected:\n" + String.join("\n", errors) + "\nReturn corrected JSON.");
        }
        return map("status", "failed", "attempts", maxAttempts, "errors", errors);
    }

    static Map<String, Object> extract(AnthropicClient client, String document) {
        return extract(client, document, 2);
    }

    static final String DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";

    static String body(Map<String, Object> overrides) {
        Map<String, Object> fields = map("vendor", "Acme Tools", "total", 120.5, "currency", "EUR", "evidence", "Total due: 120.50 EUR");
        fields.putAll(overrides);
        try {
            return JSON.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static final List<Object> REPLIES = List.of(
        message(List.of(text(body(map("currency", "Eur"))))),
        message(List.of(text(body(map("evidence", "Total due: 999.00 USD"))))),
        message(List.of(text(body(map())))),
        message(List.of(text("I can't help with that.")), "refusal"),
        message(List.of(text("{\"vendor\": \"Acme")), "max_tokens"));

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(REPLIES.toArray());
        System.out.println("schema sent to the API: " + forApi(LOCAL_SCHEMA).get("properties").get("total"));
        for (int n : new int[] {1, 2}) System.out.println("document " + n + ": " + py(extract(rig.client(), DOC)));
        System.out.println("document 3: " + py(extract(rig.client(), DOC)));
        System.out.println("document 4: " + py(extract(rig.client(), DOC)));
        List<JsonNode> sent = rig.http().requests;
        System.out.println("requests sent: " + sent.size() + " | each carried output_config.format.type: "
            + sent.stream().map(r -> "'" + r.at("/output_config/format/type").asText() + "'").distinct().collect(Collectors.joining(", ", "{", "}")));
        JsonNode third = sent.get(2).get("messages");
        List<String> roles = new ArrayList<>();
        third.forEach(m -> roles.add(m.get("role").asText()));
        System.out.println("second call of document 2 sent: " + py(roles) + " | feedback: " + third.get(third.size() - 1).get("content").asText().split("\n")[1]);
    }
}
