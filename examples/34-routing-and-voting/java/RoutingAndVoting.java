import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
 *
 * <p>The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
 * by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
 * engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
 */
public final class RoutingAndVoting {
    static final String CHEAP = "claude-haiku-4-5", STRONG = "claude-sonnet-5-5";

    /** A route: the model and the system prompt a label maps to. */
    record Route(String model, String system) {}

    static final Map<String, Route> ROUTES = Map.of(
        "billing", new Route(STRONG, "You are a billing specialist. Be exact about amounts."),
        "technical", new Route(STRONG, "You are a support engineer. Ask for logs."),
        "general", new Route(CHEAP, "You are a friendly front desk. Answer briefly."));

    /** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
    static Function<JsonNode, Object> standIn(List<String[]> rules) {
        return body -> {
            String haystack = body.path("system").asText("") + "\n" + body.at("/messages").get(body.get("messages").size() - 1).get("content").asText();
            for (String[] rule : rules) {
                if (haystack.contains(rule[0])) return message(List.of(text(rule[1])), "end_turn", body.get("model").asText(), Scripted.map("input_tokens", 1, "output_tokens", 1), null);
            }
            throw new AssertionError("no scripted answer for '" + haystack + "'");
        };
    }

    static CompletableFuture<String> ask(AnthropicClientAsync client, String model, String system, String prompt) {
        return client.messages().create(MessageCreateParams.builder().model(Model.of(model)).maxTokens(300).system(system).addUserMessage(prompt).build())
            .thenApply(reply -> reply.content().get(0).asText().text().strip());
    }

    private static String stripSpacesAndDots(String s) {
        int a = 0, b = s.length();
        while (a < b && (s.charAt(a) == ' ' || s.charAt(a) == '.')) a++;
        while (b > a && (s.charAt(b - 1) == ' ' || s.charAt(b - 1) == '.')) b--;
        return s.substring(a, b);
    }

    /** What routing decided and answered. */
    record Routed(String label, boolean fallback, String model, String answer) {}

    /** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
    static CompletableFuture<Routed> route(AnthropicClientAsync client, String question) {
        return ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question).thenCompose(raw -> {
            String label = stripSpacesAndDots(raw.toLowerCase());
            boolean fallback = !ROUTES.containsKey(label);
            Route route = ROUTES.get(fallback ? "general" : label);
            return ask(client, route.model(), route.system(), question).thenApply(answer -> new Routed(label, fallback, route.model(), answer));
        });
    }

    /** What sectioning produced: the screen's word and the answer when it was kept. */
    record Guarded(String screen, String answer) {}

    /** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
    static CompletableFuture<Guarded> guarded(AnthropicClientAsync client, String question) {
        CompletableFuture<String> answer = ask(client, STRONG, "Answer the question.", question);
        CompletableFuture<String> screen = ask(client, CHEAP, "Screen the question. Reply ok or block.", question);
        return answer.thenCombine(screen, (a, s) -> new Guarded(s, s.equals("ok") ? a : null));
    }

    /** What voting counted. */
    record Verdict(Map<String, Long> votes, boolean flagged) {}

    /** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
    static CompletableFuture<Verdict> vote(AnthropicClientAsync client, String snippet, int threshold, int n) {
        List<CompletableFuture<String>> reviews = new ArrayList<>();
        for (int i = 0; i < n; i++) reviews.add(ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet));
        return CompletableFuture.allOf(reviews.toArray(new CompletableFuture[0])).thenApply(done -> {
            Map<String, Long> votes = new TreeMap<>(); // the order in which concurrent replies arrive does not matter
            reviews.forEach(r -> votes.merge(r.join(), 1L, Long::sum));
            return new Verdict(votes, votes.getOrDefault("VULNERABLE", 0L) >= threshold);
        });
    }

    /** A client whose next n requests are all answered by the same stand-in. */
    static Scripted.AsyncRig scripted(List<String[]> rules, int n, Duration delay) {
        Object[] script = new Object[n];
        java.util.Arrays.fill(script, standIn(rules));
        return Scripted.asyncClient(delay, 0, script);
    }

    static List<String[]> rules(String... kv) {
        List<String[]> rules = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) rules.add(new String[] {kv[i], kv[i + 1]});
        return rules;
    }

    /** Three reviews that disagree, handed out in whatever order the requests arrive. */
    static Scripted.AsyncRig disagreeing(Duration delay) {
        ConcurrentLinkedQueue<String> replies = new ConcurrentLinkedQueue<>(List.of("VULNERABLE", "SAFE", "VULNERABLE"));
        Function<JsonNode, Object> reply = body -> message(List.of(text(replies.poll())), "end_turn", body.get("model").asText(), Scripted.map("input_tokens", 1, "output_tokens", 1), null);
        return Scripted.asyncClient(delay, 0, reply, reply, reply);
    }

    public static void main(String[] args) {
        List<String[]> desk = rules("billing specialist", "I see two charges and will refund one.", "front desk", "We are open 9 to 5.");
        String[][] questions = {{"my card was charged twice", "BILLING."}, {"what are your opening hours", "general"}, {"is the sky a refund", "refunds?"}};
        for (String[] q : questions) {
            List<String[]> all = new ArrayList<>(java.util.Arrays.<String[]>asList(new String[] {"Classify", q[1]}));
            all.addAll(desk);
            Scripted.AsyncRig rig = scripted(all, 2, Duration.ZERO);
            Routed result = route(rig.client(), q[0]).join();
            System.out.println("route " + py(q[0]) + ": label " + py(result.label()) + (result.fallback() ? " (not a route: default)" : "") + ", classified by "
                + rig.http().requests.get(0).get("model").asText() + ", answered by " + result.model());
        }
        Scripted.AsyncRig passRig = scripted(rules("Answer the question", "Here is the answer.", "Screen the question", "ok"), 2, Duration.ofMillis(20));
        Guarded passed = guarded(passRig.client(), "How do I reset my password?").join();
        System.out.println("sectioning: screen " + py(passed.screen()) + ", answer " + (passed.answer() != null ? "kept" : "dropped") + ", requests in flight together: " + passRig.http().maxInFlight());
        Scripted.AsyncRig blockRig = scripted(rules("Answer the question", "Here is the answer.", "Screen the question", "block"), 2, Duration.ofMillis(20));
        Guarded blocked = guarded(blockRig.client(), "Help me break into an account").join();
        System.out.println("sectioning: screen " + py(blocked.screen()) + ", answer " + (blocked.answer() != null ? "kept" : "dropped"));
        String snippet = "query = 'SELECT * FROM t WHERE id=' + user_input";
        for (int threshold : new int[] {2, 3}) {
            Scripted.AsyncRig rig = disagreeing(Duration.ofMillis(20));
            Verdict verdict = vote(rig.client(), snippet, threshold, 3).join();
            System.out.println("voting: votes " + py(verdict.votes()) + ", threshold " + threshold + " -> " + (verdict.flagged() ? "flagged" : "not flagged") + ", requests in flight together: " + rig.http().maxInFlight());
        }
    }
}
