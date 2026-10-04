import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceAuto;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a tool_choice that is kept.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
public final class ToolLoop {
    static final String MODEL = "claude-sonnet-5-5";

    static Tool tool(String name, String description) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder()
                .properties(JsonValue.from(map("city", map("type", "string", "description", "City name, for example Oslo"))))
                .required(List.of("city")).build())
            .build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("get_weather", "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts."),
        tool("get_time", "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere."));

    static final Map<String, Function<Map<String, Object>, String>> HANDLERS = Map.of(
        "get_weather", a -> Map.of("Oslo", "Oslo: 4 C, light rain").get(a.get("city")),
        "get_time", a -> Map.of("Oslo", "09:15", "Rome", "09:15").get(a.get("city")));

    /** One tool call answered: the result block that goes back to the model. */
    @SuppressWarnings("unchecked")
    static ToolResultBlockParam runTool(ToolUseBlock block) {
        Map<String, Object> input = ObjectMappers.jsonMapper().convertValue(block._input(), Map.class);
        String answer = HANDLERS.get(block.name()).apply(input);
        if (answer == null) {
            return ToolResultBlockParam.builder().toolUseId(block.id()).content("No data for '" + input.get("city") + "'. Known cities: Oslo, Rome.").isError(true).build();
        }
        return ToolResultBlockParam.builder().toolUseId(block.id()).content(answer).build();
    }

    /** The last reply and the whole transcript, as the requests carried it. */
    record Loop(Message reply, List<MessageParam> messages) {}

    static Loop loop(AnthropicClient client, String question, ToolChoice choice) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content(question).build());
        ToolChoice active = choice;
        while (true) {
            MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages);
            TOOLS.forEach(request::addTool);
            if (active != null) request.toolChoice(active);
            Message reply = client.messages().create(request.build());
            messages.add(reply.toParam());
            if (!reply.stopReason().get().asString().equals("tool_use")) return new Loop(reply, messages);
            List<ContentBlockParam> results = new ArrayList<>();
            for (ContentBlock b : reply.content()) if (b.isToolUse()) results.add(ContentBlockParam.ofToolResult(runTool(b.asToolUse())));
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build());
            if (active != null && (active.isAny() || active.isTool())) active = null; // a forced choice applies to the first request only; auto and none stay
        }
    }

    static final List<Object> REPLIES = List.of(
        message(List.of(text("Checking all three."), toolUse("toolu_01", "get_weather", map("city", "Oslo")), toolUse("toolu_02", "get_time", map("city", "Oslo")),
            toolUse("toolu_03", "get_weather", map("city", "Atlantis"))), "tool_use"),
        message(List.of(text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis."))));

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(REPLIES.toArray());
        Loop run = loop(rig.client(), "Weather and time in Oslo, and the weather in Atlantis?", ToolChoice.ofAuto(ToolChoiceAuto.builder().disableParallelToolUse(false).build()));
        System.out.println("model calls: " + rig.http().requests.size() + " | stop reasons: " + py(List.of("tool_use", run.reply().stopReason().get().asString())));
        System.out.println("roles after the first reply: " + py(run.messages().stream().map(m -> m.role().asString()).toList()));
        List<ContentBlockParam> results = run.messages().get(2).content().blockParams().get();
        System.out.println("tool results in ONE user message: " + results.size() + " | ids in order: " + py(results.stream().map(r -> r.asToolResult().toolUseId()).toList()));
        for (ContentBlockParam r : results) {
            ToolResultBlockParam result = r.asToolResult();
            System.out.println("  " + result.toolUseId() + ": is_error=" + py(result.isError().orElse(false)) + " content=" + py(result.content().get().string().get()));
        }
        System.out.println("tool_choice sent on requests 1 and 2: " + py(rig.http().requests.stream().map(r -> r.get("tool_choice")).toList()));
        boolean noHandler = true;
        for (var t : rig.http().requests.get(0).get("tools")) {
            java.util.Set<String> fields = new java.util.HashSet<>();
            t.fieldNames().forEachRemaining(fields::add);
            noHandler &= fields.equals(java.util.Set.of("name", "description", "input_schema"));
        }
        System.out.println("tool definitions sent carry no handler: " + py(noHandler));
        System.out.println("final text: " + run.reply().content().get(0).asText().text());
    }
}
