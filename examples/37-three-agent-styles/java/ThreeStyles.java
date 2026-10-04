import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
 *
 * <p>The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
 * functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
 * sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
 */
public final class ThreeStyles {
    static final String TICKET = "I was charged twice for invoice 1042.";
    static final Map<String, String> INVOICES = Map.of("1042", "paid twice on 2026-09-30");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
    static final class Scripted implements Function<String, String> {
        final List<String> queue;
        final List<String> seen = new ArrayList<>();

        Scripted(String... replies) {
            queue = new ArrayList<>(List.of(replies));
        }

        @Override
        public String apply(String prompt) {
            seen.add(prompt);
            return queue.remove(0);
        }
    }

    // 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

    record Checkpoint(String node, Map<String, String> state) {}

    record GraphRun(List<String> path, Map<String, String> state, List<Checkpoint> checkpoints) {}

    static GraphRun runGraph(Function<String, String> model, String ticket, int resumeFrom, List<Checkpoint> given) {
        Map<String, Function<Map<String, String>, Map<String, String>>> nodes = Map.of(
            "classify", s -> Map.of("topic", model.apply("Classify as billing or other: " + s.get("ticket")).strip().toLowerCase(java.util.Locale.ROOT)),
            "lookup", s -> Map.of("invoice", INVOICES.getOrDefault(s.get("ticket").split("invoice ")[1].replaceAll("\\.+$", ""), "unknown")),
            "draft", s -> Map.of("reply", model.apply("Write a reply. Topic: " + s.get("topic") + ". Invoice: " + s.getOrDefault("invoice", "none"))));
        Map<String, Function<Map<String, String>, String>> edges = Map.of(
            "classify", s -> s.get("topic").equals("billing") ? "lookup" : "draft",
            "lookup", s -> "draft",
            "draft", s -> null);
        List<Checkpoint> checkpoints = given != null ? given : new ArrayList<>(List.of(new Checkpoint("classify", Map.of("ticket", ticket))));
        String node = checkpoints.get(resumeFrom).node();
        Map<String, String> state = checkpoints.get(resumeFrom).state();
        while (checkpoints.size() > resumeFrom + 1) checkpoints.remove(checkpoints.size() - 1);
        List<String> path = new ArrayList<>();
        while (node != null) {
            Map<String, String> next = new LinkedHashMap<>(state);
            next.putAll(nodes.get(node).apply(state));
            state = next;
            path.add(node);
            node = edges.get(node).apply(state);
            if (node != null) checkpoints.add(new Checkpoint(node, new LinkedHashMap<>(state)));
        }
        return new GraphRun(path, state, checkpoints);
    }

    // 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

    record AgentRun(String reply, List<String> trace, String stopped) {}

    @SuppressWarnings("unchecked")
    static AgentRun runAgent(Function<String, String> model, String ticket, int maxSteps) throws Exception {
        Map<String, Function<String, String>> tools = Map.of("lookup_invoice", arg -> INVOICES.getOrDefault(arg, "unknown"));
        List<String> trace = new ArrayList<>();
        String observation = "";
        for (int i = 0; i < maxSteps; i++) {
            Map<String, Object> step = JSON.readValue(model.apply("Ticket: " + ticket + "\nTools: " + py(tools.keySet().stream().sorted().toList())
                + "\nLast result: " + observation + "\nReply JSON: a tool call or a final reply."), Map.class);
            if (step.containsKey("final")) return new AgentRun((String) step.get("final"), trace, null);
            observation = tools.get((String) step.get("tool")).apply((String) step.get("arg"));
            trace.add(step.get("tool") + "(" + step.get("arg") + ") -> " + observation);
        }
        return new AgentRun(null, trace, "max_steps");
    }

    // 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

    /** The checked reply, or the reason it was refused. */
    record Checked(Map<String, Object> data, String error) {}

    record TypedRun(Map<String, Object> data, int attempts, String error) {}

    static Checked validate(String raw) {
        Map<String, Object> data;
        try {
            data = JSON.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new Checked(null, "the reply is not JSON");
        }
        if (!(data.get("topic") instanceof String)) return new Checked(null, "field topic must be str");
        if (!(data.get("refund_cents") instanceof Integer)) return new Checked(null, "field refund_cents must be int");
        return new Checked(data, null);
    }

    static TypedRun runTyped(Function<String, String> model, String ticket, int retries) {
        String prompt = "Return JSON with topic and refund_cents for: " + ticket;
        int attempts = 0;
        while (true) {
            attempts++;
            Checked checked = validate(model.apply(prompt));
            if (checked.data() != null) return new TypedRun(checked.data(), attempts, null);
            if (attempts > retries) return new TypedRun(null, attempts, checked.error());
            prompt = prompt + "\nYour last reply was refused: " + checked.error() + ". Fix it.";
        }
    }

    /** Python's repr of strings, lists and maps, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v == null) return "None";
        if (v instanceof String s) {
            String q = s.contains("'") && !s.contains("\"") ? "\"" : "'";
            return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q;
        }
        if (v instanceof Map<?, ?> m) return m.entrySet().stream().map(e -> py(e.getKey()) + ": " + py(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
        if (v instanceof List<?> l) return l.stream().map(ThreeStyles::py).collect(Collectors.joining(", ", "[", "]"));
        return String.valueOf(v);
    }

    public static void main(String[] args) throws Exception {
        Scripted m = new Scripted("billing", "We refunded the duplicate charge on invoice 1042.");
        GraphRun graph = runGraph(m, TICKET, 0, null);
        System.out.println("graph: " + String.join(" -> ", graph.path()) + " | model calls: " + m.seen.size() + " | checkpoints: " + graph.checkpoints().size());
        Scripted again = new Scripted("We refunded the duplicate charge on invoice 1042.");
        GraphRun resumed = runGraph(again, TICKET, 2, new ArrayList<>(graph.checkpoints()));
        System.out.println("graph resumed from checkpoint 2: " + String.join(" -> ", resumed.path()) + " | model calls: " + again.seen.size()
            + " | same reply: " + (resumed.state().get("reply").equals(graph.state().get("reply")) ? "True" : "False"));
        Scripted a = new Scripted("{\"tool\": \"lookup_invoice\", \"arg\": \"1042\"}", "{\"final\": \"Refunded the second payment.\"}");
        AgentRun agent = runAgent(a, TICKET, 5);
        System.out.println("agent: " + py(agent.trace()) + " -> " + agent.reply() + " | model calls: " + a.seen.size());
        Scripted loop = new Scripted("{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}", "{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}", "{\"tool\": \"lookup_invoice\", \"arg\": \"1\"}");
        System.out.println("agent that never finishes: " + runAgent(loop, TICKET, 3).stopped() + " after " + loop.seen.size() + " calls");
        Scripted t = new Scripted("{\"topic\": \"billing\", \"refund_cents\": \"4999\"}", "{\"topic\": \"billing\", \"refund_cents\": 4999}");
        TypedRun typed = runTyped(t, TICKET, 1);
        String[] lines = t.seen.get(1).split("\n");
        System.out.println("typed: " + py(typed.data()) + " | attempts: " + typed.attempts() + " | second prompt ends: " + lines[lines.length - 1]);
        TypedRun bad = runTyped(new Scripted("no json", "still no json"), TICKET, 1);
        System.out.println("typed, never valid: " + bad.error() + " after " + bad.attempts() + " attempts");
    }
}
