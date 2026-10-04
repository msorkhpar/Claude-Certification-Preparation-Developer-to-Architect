import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.beta.messages.BetaContextManagementConfig;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.CitationsConfigParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import harness.Scripted;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clearing old tool results, asking the API to clear them, and checking the citations in an answer.
 *
 * <p>The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
 * not captures; the numbers in the context editing response are the documentation's own example.
 * Messages are JSON trees (Jackson), the same shape the API takes; the SDK's own types read them for the beta call.
 */
public final class ContextTrimming {
    static final String MODEL = "claude-sonnet-5-5";
    static final String POLICY = "The grass is green. The sky is blue. Water is essential for life.";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call. */
    static int tokens(JsonNode messages) {
        int total = 0;
        for (JsonNode m : messages) {
            total += 4;
            JsonNode content = m.get("content");
            if (content.isTextual()) {
                total += (content.asText().length() + 3) / 4;
                continue;
            }
            for (JsonNode b : content) {
                switch (b.get("type").asText()) {
                    case "text" -> total += (b.get("text").asText().length() + 3) / 4;
                    case "tool_result" -> total += (b.get("content").asText().length() + 3) / 4;
                    default -> total += 10; // tool_use
                }
            }
        }
        return total;
    }

    static ArrayNode conversation() {
        ArrayNode messages = JSON.createArrayNode();
        messages.add(JSON.valueToTree(map("role", "user", "content", "Find every mention of the grass in the logs.")));
        for (int i = 1; i <= 5; i++) {
            messages.add(JSON.valueToTree(map("role", "assistant", "content", List.of(map("type", "tool_use", "id", "toolu_" + i, "name", "grep_logs", "input", map("pattern", "grass-" + i))))));
            messages.add(JSON.valueToTree(map("role", "user", "content", List.of(map("type", "tool_result", "tool_use_id", "toolu_" + i, "content", "log line " + i + ": " + "x".repeat(400))))));
        }
        messages.add(JSON.valueToTree(map("role", "assistant", "content", List.of(map("type", "text", "text", "Found them all.")))));
        return messages;
    }

    static List<ObjectNode> toolResults(JsonNode messages) {
        List<ObjectNode> results = new ArrayList<>();
        for (JsonNode m : messages) {
            if (!m.get("content").isArray()) continue;
            for (JsonNode b : m.get("content")) if (b.get("type").asText().equals("tool_result")) results.add((ObjectNode) b);
        }
        return results;
    }

    /** A copy in which every tool result but the newest `keep` has its content replaced; the calls stay. */
    static ArrayNode clearToolResults(JsonNode messages, int keep, String placeholder) {
        ArrayNode out = messages.deepCopy();
        List<ObjectNode> results = toolResults(out);
        for (ObjectNode block : results.subList(0, Math.max(results.size() - keep, 0))) block.put("content", placeholder);
        return out;
    }

    /** A citation is a claim about where text came from; check it against the document. */
    static List<Map<String, Object>> verify(JsonNode blocks, List<String> documents) {
        List<Map<String, Object>> bad = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            JsonNode citations = blocks.get(i).path("citations");
            for (int j = 0; j < citations.size(); j++) {
                JsonNode cite = citations.get(j);
                String cited = documents.get(cite.get("document_index").asInt()).substring(cite.get("start_char_index").asInt(), cite.get("end_char_index").asInt());
                if (!cited.equals(cite.get("cited_text").asText())) bad.add(map("block", i, "citation", j, "problem", "text_mismatch"));
            }
        }
        return bad;
    }

    static String footnotes(JsonNode blocks, List<String> titles) {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        for (JsonNode block : blocks) {
            out.append(block.get("text").asText());
            for (JsonNode cite : block.path("citations")) {
                String key = cite.get("document_index").asInt() + ":" + cite.get("start_char_index").asInt() + ":" + cite.get("end_char_index").asInt();
                if (!numbers.containsKey(key)) {
                    numbers.put(key, numbers.size() + 1);
                    lines.add("[" + numbers.get(key) + "] " + titles.get(cite.get("document_index").asInt()) + ": \"" + cite.get("cited_text").asText() + "\"");
                }
                out.append("[").append(numbers.get(key)).append("]");
            }
        }
        return out + (lines.isEmpty() ? "" : "\n\nSources:\n" + String.join("\n", lines));
    }

    static Map<String, Object> cite(int start, int end) {
        return map("type", "char_location", "cited_text", POLICY.substring(start, end), "document_index", 0, "document_title", "Policy",
            "start_char_index", start, "end_char_index", end, "file_id", null);
    }

    static final String EDITS = """
        {"edits": [{"type": "clear_tool_uses_20250919", "trigger": {"type": "input_tokens", "value": 30000}, "keep": {"type": "tool_uses", "value": 3},
                    "clear_at_least": {"type": "input_tokens", "value": 5000}, "exclude_tools": ["web_search"]}]}""";

    static Map<String, Object> editingReply() {
        Map<String, Object> body = message(List.of(text("Found them all.")));
        body.put("context_management", map("applied_edits", List.of(map("type", "clear_tool_uses_20250919", "cleared_tool_uses", 8, "cleared_input_tokens", 50000))));
        return body;
    }

    static Map<String, Object> citedReply() {
        return message(List.of(
            map("type", "text", "text", "The grass is green. ", "citations", List.of(cite(0, 19))),
            map("type", "text", "text", "Water matters. ", "citations", List.of(cite(37, 65))),
            map("type", "text", "text", "Green again.", "citations", List.of(cite(0, 19)))));
    }

    public static void main(String[] args) throws Exception {
        ArrayNode before = conversation();
        ArrayNode after = clearToolResults(before, 2, "[cleared]");
        long cleared = toolResults(after).stream().filter(b -> b.get("content").asText().equals("[cleared]")).count();
        boolean callsKept = true;
        for (int i = 0; i < before.size(); i++) if (before.get(i).get("role").asText().equals("assistant") && !before.get(i).equals(after.get(i))) callsKept = false;
        System.out.println("conversation: " + before.size() + " messages, 5 tool results, about " + tokens(before) + " tokens");
        System.out.println("after clearing all but the newest 2 results: about " + tokens(after) + " tokens, " + cleared + " results replaced, calls kept: " + py(callsKept));
        Scripted.Rig rig = Scripted.client(editingReply(), citedReply());
        BetaMessage reply = rig.client().beta().messages().create(com.anthropic.models.beta.messages.MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300)
            .messages(ObjectMappers.jsonMapper().convertValue(before, new TypeReference<List<BetaMessageParam>>() {}))
            .addBeta("context-management-2025-06-27").contextManagement(ObjectMappers.jsonMapper().readValue(EDITS, BetaContextManagementConfig.class)).build());
        System.out.println("beta header sent: " + rig.http().headers.get(0).get("anthropic-beta"));
        JsonNode edit = rig.http().requests.get(0).at("/context_management/edits/0");
        System.out.println("edit sent: " + edit.get("type").asText() + " trigger " + edit.at("/trigger/value").asInt() + " keep " + edit.at("/keep/value").asInt() + " exclude " + py(edit.get("exclude_tools")));
        JsonNode applied = ObjectMappers.jsonMapper().valueToTree(reply.contextManagement().get().appliedEdits().get(0));
        System.out.println("applied edit reported: " + applied.get("type").asText() + " cleared " + applied.get("cleared_tool_uses").asInt() + " tool uses, " + applied.get("cleared_input_tokens").asInt() + " input tokens");
        MessageCreateParams ask = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).addUserMessageOfBlockParams(List.of(
            ContentBlockParam.ofDocument(DocumentBlockParam.builder().source(DocumentBlockParam.Source.ofText(POLICY)).title("Policy").citations(CitationsConfigParam.builder().enabled(true).build()).build()),
            ContentBlockParam.ofText(TextBlockParam.builder().text("What does the policy say about grass and water?").build()))).build();
        Message answer = rig.client().messages().create(ask);
        JsonNode blocks = ObjectMappers.jsonMapper().valueToTree(answer.content());
        System.out.println("citations enabled in the request: " + py(rig.http().requests.get(1).at("/messages/0/content/0/citations")));
        System.out.println("citation problems: " + py(verify(blocks, List.of(POLICY))));
        ArrayNode tampered = blocks.deepCopy();
        ((ObjectNode) tampered.get(1).get("citations").get(0)).put("cited_text", "Water is optional.");
        System.out.println("after tampering with one cited_text: " + py(verify(tampered, List.of(POLICY))));
        System.out.println(footnotes(blocks, List.of("Policy")));
    }
}
