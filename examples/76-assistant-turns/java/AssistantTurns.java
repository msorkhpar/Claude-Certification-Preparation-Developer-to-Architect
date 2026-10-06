import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The turn logic of a conversational assistant in miniature: a message is routed in code before the model sees it, the window keeps the pinned facts and the newest turns
 * that fit, and a memory that crosses sessions is read per customer and marked when it is old.
 *
 * <p>The words are made up and the routing is a plain list of phrases: this example is about where each decision lives, not about what a model would say. The shapes (a route,
 * a window, a recalled fact with a status) are this course's design, not an Anthropic interface.
 */
public final class AssistantTurns {
    private static final System.Logger LOG = System.getLogger(AssistantTurns.class.getName());
    record Saved(String key, String value, String saved) {}

    record Window(List<String> kept, int dropped, List<String> facts) {}

    record Recalled(String key, String value, String status) {}

    static final List<String> RISK = List.of("hurt myself", "end my life", "emergency");
    static final List<String> ASKS_FOR_PERSON = List.of("human", "a person", "an agent");
    static final Map<String, List<Saved>> STORE = Map.of("ada", List.of(new Saved("address", "12 Elm Road", "2026-09-20"), new Saved("plan", "Plus", "2025-12-01")));

    /** Decided in code, in this order: a signal of risk, a request for a person, a stalled conversation, otherwise the model answers. */
    static String route(String message, int misses) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", message);
        String text = message.toLowerCase();
        if (RISK.stream().anyMatch(text::contains)) return "handoff:safety";
        if (ASKS_FOR_PERSON.stream().anyMatch(text::contains)) return "handoff:requested";
        if (misses >= 2) return "handoff:stalled";
        return "answer";
    }

    /** The pinned facts always stay; of the turns, the newest ones that fit the budget (counted in words) stay, as one block at the end. */
    static Window window(List<String> turns, int budget, List<String> facts) {
        List<String> kept = new ArrayList<>();
        int used = 0;
        for (int i = turns.size() - 1; i >= 0; i--) {
            int words = turns.get(i).split("\\s+").length;
            if (used + words > budget) break;
            kept.add(0, turns.get(i));
            used += words;
        }
        return new Window(kept, turns.size() - kept.size(), facts);
    }

    /** The facts saved for this customer only, each marked current or to be verified when it is older than the limit. */
    static List<Recalled> recall(String user, String today, int maxAgeDays) {
        return STORE.getOrDefault(user, List.of()).stream().map(s -> {
            long age = ChronoUnit.DAYS.between(LocalDate.parse(s.saved()), LocalDate.parse(today));
            return new Recalled(s.key(), s.value(), age <= maxAgeDays ? "current" : "verify");
        }).toList();
    }

    public static void main(String[] args) {
        String[] messages = {"Where is my parcel?", "I want to talk to a human", "I feel like I might hurt myself", "what?"};
        int[] misses = {0, 0, 0, 2};
        for (int i = 0; i < messages.length; i++) {
            System.out.println("route '" + messages[i] + "'" + (misses[i] > 0 ? " after " + misses[i] + " misses" : "") + ": " + route(messages[i], misses[i]));
        }
        List<String> turns = List.of("Hello", "My parcel has not arrived", "It was due on Monday", "Can you check the order", "Order 1234 please");
        List<String> facts = List.of("order 1234: parcel due 2026-09-28", "address: 12 Elm Road");
        Window w = window(turns, 12, facts);
        System.out.println("window: " + turns.size() + " turns, budget 12 words -> kept " + w.kept().size() + ", dropped " + w.dropped() + ", facts kept " + w.facts().size());
        for (String user : List.of("ada", "bob")) {
            String found = recall(user, "2026-10-04", 30).stream().map(r -> r.key() + "=" + r.value() + " (" + r.status() + ")").collect(Collectors.joining(", "));
            System.out.println("recall " + user + " on 2026-10-04: " + (found.isEmpty() ? "nothing stored" : found));
        }
    }
}
