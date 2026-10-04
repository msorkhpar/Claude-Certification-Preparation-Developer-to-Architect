import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Tool;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
 * request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
 */
public final class Hub {
    static final String MODEL = "claude-sonnet-5-5";
    static final Tool PLAN_TOOL = Tool.builder().name("plan")
        .description("Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.")
        .inputSchema(Tool.InputSchema.builder()
            .properties(JsonValue.from(map("subtasks", map("type", "array", "items", map("type", "object",
                "properties", map("scope", map("type", "string"), "brief", map("type", "string")), "required", List.of("scope", "brief"))))))
            .required(List.of("subtasks")).build())
        .build();
    static final String SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note.";

    record Subtask(String scope, String brief) {}

    static final List<Subtask> SUBTASKS = List.of(
        new Subtask("chips", "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."),
        new Subtask("cars", "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."),
        new Subtask("rates", "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."));
    static final List<String> REPORTS = List.of(
        "CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
        "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
        "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements.");

    static String textOf(Message reply) {
        StringBuilder out = new StringBuilder();
        for (ContentBlock b : reply.content()) if (b.isText()) out.append(b.asText().text());
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    static List<Subtask> plan(AnthropicClient client, String question) {
        // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
        Message reply = client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).addTool(PLAN_TOOL)
            .addUserMessage(question + "\nRecord your plan by calling the plan tool.").build());
        for (ContentBlock b : reply.content()) {
            if (!b.isToolUse()) continue;
            Map<String, Object> input = ObjectMappers.jsonMapper().convertValue(b.asToolUse()._input(), Map.class);
            List<Subtask> out = new ArrayList<>();
            for (Map<String, String> s : (List<Map<String, String>>) input.get("subtasks")) out.add(new Subtask(s.get("scope"), s.get("brief")));
            return out;
        }
        throw new IllegalStateException("no plan call");
    }

    /** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
    static String runSubagent(AnthropicClient client, String brief) {
        return textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).system(SUBAGENT_SYSTEM).addUserMessage(brief).build()));
    }

    static String synthesize(AnthropicClient client, String question, List<String[]> findings) {
        StringBuilder listing = new StringBuilder();
        for (String[] f : findings) listing.append(listing.length() == 0 ? "" : "\n").append("[").append(f[0]).append("] ").append(f[1]);
        return textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800)
            .addUserMessage("Question: " + question + "\nFindings:\n" + listing + "\nWrite one answer that uses every finding.").build()));
    }

    static String tag(String report) {
        return report.split(":")[0];
    }

    public static void main(String[] args) {
        String question = "How did the 2024 supply picture change for chips, cars and interest rates?";
        List<Object> replies = new ArrayList<>();
        replies.add(message(List.of(text("Three independent parts."), toolUse("toolu_01", "plan", map("subtasks", SUBTASKS.stream().map(s -> map("scope", s.scope(), "brief", s.brief())).toList()))), "tool_use"));
        for (String r : REPORTS) replies.add(message(List.of(text(r))));
        replies.add(message(List.of(text("Chip supply recovered first, car output followed, and rates stayed high."))));
        Scripted.Rig rig = Scripted.client(replies.toArray());
        List<Subtask> subtasks = plan(rig.client(), question);
        System.out.println("plan: " + subtasks.size() + " subtasks -> " + py(subtasks.stream().map(Subtask::scope).toList()));
        List<String[]> findings = new ArrayList<>();
        int number = 0;
        for (Subtask task : subtasks) {
            number++;
            String report = runSubagent(rig.client(), task.brief());
            findings.add(new String[] {task.scope(), report});
            var request = rig.http().requests.get(number);
            String body = request.toString();
            final String current = report;
            boolean others = REPORTS.stream().anyMatch(r -> !r.equals(current) && body.contains(tag(r)));
            System.out.println("subagent " + number + " request: " + request.get("messages").size() + " message, system prompt of the role: "
                + py(request.get("system").asText().equals(SUBAGENT_SYSTEM)) + ", holds the coordinator's question: " + py(body.contains(question))
                + ", holds another report: " + py(others));
        }
        String answer = synthesize(rig.client(), question, findings);
        String synthesis = rig.http().requests.get(4).toString();
        System.out.println("synthesis request: reports included: " + REPORTS.stream().filter(r -> synthesis.contains(tag(r))).count() + " of " + REPORTS.size());
        System.out.println("model calls: " + rig.http().requests.size() + " | one plan, three subagents, one synthesis");
        System.out.println("answer: " + answer);
    }
}
