import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Reply;
import harness.Scripted;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Twelve classification calls with the async SDK: unbounded, then bounded by a semaphore.
 *
 * <p>The transport is scripted: every request takes 50 ms inside it, the reply is a label built from
 * the ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
 * Each ticket runs on its own virtual thread; the semaphore is what bounds them (the Java counterpart of asyncio.Semaphore).
 */
public final class Bounded {
    static final String MODEL = "claude-sonnet-5-5";
    static final List<String> TICKETS = IntStream.rangeClosed(1, 12).mapToObj(n -> "ticket " + n).toList();

    /** A scripted reply computed from the request body. */
    static final Function<JsonNode, Object> RESPONDER = body -> {
        String ticket = body.at("/messages/0/content").asText();
        if (ticket.equals("ticket 7")) return Reply.json(429, map("type", "error", "error", map("type", "rate_limit_error", "message", "slow down")));
        return message(List.of(text("label for " + ticket)));
    };

    /** One ticket's outcome: a label or the error that ended it (a failure does not cancel the others). */
    record Outcome(String label, Throwable error) {}

    static CompletableFuture<String> classify(AnthropicClientAsync client, String ticket) {
        MessageCreateParams params = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).addUserMessage(ticket).build();
        return client.messages().create(params).thenApply(reply -> reply.content().get(0).asText().text());
    }

    /** The outcomes in input order, and the most requests that were inside the transport at once. */
    record Run(List<Outcome> results, int peak) {}

    static Run runAll(Integer limit) {
        Scripted.AsyncRig rig = Scripted.asyncClient(Duration.ofMillis(50), 0, TICKETS.stream().map(t -> (Object) RESPONDER).toArray());
        Semaphore gate = limit == null ? null : new Semaphore(limit);
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Outcome>> tasks = new ArrayList<>();
            for (String ticket : TICKETS) {
                tasks.add(CompletableFuture.supplyAsync(() -> {
                    if (gate != null) gate.acquireUninterruptibly();
                    try {
                        return new Outcome(classify(rig.client(), ticket).join(), null);
                    } catch (CompletionException e) {
                        return new Outcome(null, e.getCause());
                    } finally {
                        if (gate != null) gate.release();
                    }
                }, virtual));
            }
            return new Run(tasks.stream().map(CompletableFuture::join).toList(), rig.http().maxInFlight());
        }
    }

    public static void main(String[] args) {
        Run last = null;
        for (Object[] mode : new Object[][] {{"unbounded", null}, {"bounded by 4", 4}}) {
            last = runAll((Integer) mode[1]);
            List<Integer> failed = new ArrayList<>();
            for (int i = 0; i < last.results().size(); i++) if (last.results().get(i).error() != null) failed.add(i + 1);
            System.out.println(mode[0] + ": peak in flight " + last.peak() + ", " + (last.results().size() - failed.size()) + " answered, failed tickets " + failed);
        }
        List<Outcome> r = last.results();
        System.out.println("results keep input order: " + r.get(0).label() + " | " + r.get(5).label() + " | " + r.get(6).error().getClass().getSimpleName() + " | " + r.get(7).label());
    }
}
