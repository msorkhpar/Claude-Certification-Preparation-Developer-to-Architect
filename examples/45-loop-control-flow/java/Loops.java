import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 * Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
 */
public final class Loops {
    private static final System.Logger LOG = System.getLogger(Loops.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    static Tool tool(String name, String description, String property, String type) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map(property, map("type", type)))).required(List.of(property)).build())
            .build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("save_file", "Save the report under a file name. Use it once the report is written.", "file", "string"),
        tool("lookup", "Look up item number n and return its record.", "n", "integer"));

    /** What a loop returns: the status, the number of model calls, the tools that ran, and the last reply's text. */
    record Outcome(String status, int calls, List<String> ran, String answer) {}

    static Message call(AnthropicClient client, List<MessageParam> messages) {
        MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).messages(messages);
        TOOLS.forEach(request::addTool);
        Message reply = client.messages().create(request.build());
        messages.add(reply.toParam());
        return reply;
    }

    static List<ContentBlockParam> runTools(Message reply, List<String> ran) {
        List<ContentBlockParam> results = new ArrayList<>();
        for (ContentBlock b : reply.content()) {
            if (!b.isToolUse()) continue;
            ran.add(b.asToolUse().name());
            results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder().toolUseId(b.asToolUse().id()).content(b.asToolUse().name() + " ok").build()));
        }
        return results;
    }

    static String textOf(Message reply) {
        StringBuilder out = new StringBuilder();
        for (ContentBlock b : reply.content()) if (b.isText()) out.append(b.asText().text());
        return out.toString();
    }

    static MessageParam user(String task) {
        return MessageParam.builder().role(MessageParam.Role.USER).content(task).build();
    }

    static MessageParam results(List<ContentBlockParam> blocks) {
        return MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(blocks).build();
    }

    /** Right: the model's own stop_reason decides. The count is only a backstop with a status of its own. */
    static Outcome byStopReason(AnthropicClient client, String task, int backstop) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        Message reply = null;
        for (int call = 1; call <= backstop; call++) {
            reply = call(client, messages);
            if (!reply.stopReason().get().asString().equals("tool_use")) return new Outcome("done", call, ran, textOf(reply));
            messages.add(results(runTools(reply, ran)));
        }
        return new Outcome("max_turns", backstop, ran, textOf(reply));
    }

    static Outcome byStopReason(AnthropicClient client, String task) {
        return byStopReason(client, task, 10);
    }

    /** Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply. */
    static Outcome byTextMarker(AnthropicClient client, String task) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        int call = 0;
        while (true) {
            call++;
            Message reply = call(client, messages);
            if (textOf(reply).toLowerCase().contains("done")) return new Outcome("done", call, ran, textOf(reply));
            messages.add(results(runTools(reply, ran)));
        }
    }

    /** Wrong: the count is the loop. Whatever the third reply holds is returned as the answer. */
    static Outcome byFixedCount(AnthropicClient client, String task, int turns) {
        List<MessageParam> messages = new ArrayList<>(List.of(user(task)));
        List<String> ran = new ArrayList<>();
        Message reply = null;
        for (int call = 1; call <= turns; call++) {
            reply = call(client, messages);
            if (reply.stopReason().get().asString().equals("tool_use")) messages.add(results(runTools(reply, ran)));
        }
        return new Outcome("done", turns, ran, textOf(reply));
    }

    static Outcome byFixedCount(AnthropicClient client, String task) {
        return byFixedCount(client, task, 3);
    }

    static Object[] scenarioA() {
        return new Object[] {
            message(List.of(text("All done with the analysis. Saving it now."), toolUse("toolu_01", "save_file", map("file", "report.txt"))), "tool_use"),
            message(List.of(text("Saved report.txt.")))};
    }

    static Object[] scenarioB() {
        List<Object> replies = new ArrayList<>();
        for (int n = 1; n <= 4; n++) replies.add(message(List.of(toolUse("toolu_0" + n, "lookup", map("n", n))), "tool_use"));
        replies.add(message(List.of(text("Looked up 4 items."))));
        return replies.toArray();
    }

    static void show(String label, Outcome o) {
        System.out.println(String.format("  %-26s status=%-9s model calls=%d  tools run=%d  text=%s", label, o.status(), o.calls(), o.ran().size(), py(o.answer())));
    }

    public static void main(String[] args) {
        System.out.println("A: the reply says 'All done' and also calls save_file");
        show("stop_reason loop", byStopReason(Scripted.client(scenarioA()).client(), "Write and save the report."));
        show("text-marker loop", byTextMarker(Scripted.client(scenarioA()).client(), "Write and save the report."));
        System.out.println("B: the task needs four lookups, then the model ends its turn");
        show("stop_reason loop, cap 10", byStopReason(Scripted.client(scenarioB()).client(), "Look up items 1 to 4."));
        show("stop_reason loop, cap 3", byStopReason(Scripted.client(scenarioB()).client(), "Look up items 1 to 4.", 3));
        show("fixed-count loop of 3", byFixedCount(Scripted.client(scenarioB()).client(), "Look up items 1 to 4."));
    }
}
