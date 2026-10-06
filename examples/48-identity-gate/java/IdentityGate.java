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
import com.anthropic.models.messages.ToolResultBlockParam;
import harness.Scripted;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
 * verification first in both runs: what differs is whether the code that runs the tools checks it.
 */
public final class IdentityGate {
    private static final System.Logger LOG = System.getLogger(IdentityGate.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You are a support agent. Verify the customer's identity before any refund.";

    static Tool tool(String name, String description, Map<String, Object> properties) {
        return Tool.builder().name(name).description(description)
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(properties)).required(new ArrayList<>(properties.keySet())).build()).build();
    }

    static final List<Tool> TOOLS = List.of(
        tool("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", map("code", map("type", "string"))),
        tool("lookup_order", "Look up one order of the verified customer.", map("order_id", map("type", "string"))),
        tool("process_refund", "Refund an amount in cents on a looked-up order.", map("order_id", map("type", "string"), "amount_cents", map("type", "integer"))));

    static final class Backend {
        final List<String> log = new ArrayList<>();
        boolean verified = false;

        String call(String name, Map<String, Object> args) {
            log.add(name);
            return switch (name) {
                case "verify_identity" -> {
                    verified = "1234".equals(args.get("code"));
                    yield verified ? "verified=yes" : "verified=no";
                }
                case "lookup_order" -> "order_id=O1; total_cents=5000";
                case "process_refund" -> "refund_id=R1";
                default -> throw new IllegalArgumentException(name);
            };
        }
    }

    /** What one tool call returned and whether it is an error. */
    record Result(String name, String content, boolean isError) {}

    /** The prerequisite, in code: nothing but verification runs before the customer is verified. */
    static Result gate(Backend backend, String name, Map<String, Object> args) {
        if (!name.equals("verify_identity") && !backend.verified) {
            return new Result(name, "BLOCKED identity_required: Verify the customer's identity before this action.", true);
        }
        return new Result(name, backend.call(name, args), false);
    }

    @SuppressWarnings("unchecked")
    static List<Result> run(AnthropicClient client, Backend backend, boolean gated) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content("Please refund 20.00 on order O1. My code is 1234.").build());
        List<Result> results = new ArrayList<>();
        while (true) {
            MessageCreateParams.Builder request = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(SYSTEM).messages(messages);
            TOOLS.forEach(request::addTool);
            Message reply = client.messages().create(request.build());
            messages.add(reply.toParam());
            if (!reply.stopReason().get().asString().equals("tool_use")) return results;
            List<ContentBlockParam> out = new ArrayList<>();
            for (ContentBlock block : reply.content()) {
                if (!block.isToolUse()) continue;
                var use = block.asToolUse();
                Map<String, Object> args = ObjectMappers.jsonMapper().convertValue(use._input(), Map.class);
                Result r = gated ? gate(backend, use.name(), args) : new Result(use.name(), backend.call(use.name(), args), false);
                results.add(r);
                ToolResultBlockParam.Builder b = ToolResultBlockParam.builder().toolUseId(use.id()).content(r.content());
                if (r.isError()) b.isError(true);
                out.add(ContentBlockParam.ofToolResult(b.build()));
            }
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(out).build());
        }
    }

    static Object[] replies(boolean gated) {
        Map<String, Object> first = message(List.of(text("Refunding now."), toolUse("toolu_01", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use");
        if (!gated) return new Object[] {first, message(List.of(text("Refund issued.")))};
        return new Object[] {first,
            message(List.of(toolUse("toolu_02", "verify_identity", map("code", "1234"))), "tool_use"),
            message(List.of(toolUse("toolu_03", "lookup_order", map("order_id", "O1"))), "tool_use"),
            message(List.of(toolUse("toolu_04", "process_refund", map("order_id", "O1", "amount_cents", 2000))), "tool_use"),
            message(List.of(text("Refund issued after verification.")))};
    }

    public static void main(String[] args) {
        System.out.println("system prompt in both runs: " + SYSTEM);
        for (Object[] mode : new Object[][] {{"prompt only", false}, {"code gate  ", true}}) {
            String label = (String) mode[0];
            boolean gated = (Boolean) mode[1];
            Scripted.Rig rig = Scripted.client(replies(gated));
            Backend backend = new Backend();
            List<Result> results = run(rig.client(), backend, gated);
            boolean before = backend.log.contains("verify_identity")
                ? backend.log.indexOf("process_refund") < backend.log.indexOf("verify_identity") : backend.log.contains("process_refund");
            System.out.println(label + ": backend calls = " + py(backend.log) + "; refund before verification: " + py(before));
            if (gated) System.out.println(label + ": first result sent back to the model: " + results.get(0).content() + " (is_error=" + py(results.get(0).isError()) + ")");
            for (var r : rig.http().requests) if (!r.get("system").asText().equals(SYSTEM)) throw new AssertionError("system prompt changed");
        }
    }
}
