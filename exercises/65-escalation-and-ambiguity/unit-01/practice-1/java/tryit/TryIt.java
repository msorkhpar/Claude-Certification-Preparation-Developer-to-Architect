import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // A customer who asks for a person is escalated at once; a calm one the agent can resolve is not.
        Escalation.Case asked = new Escalation.Case(true, 1, true, 0, "calm", 95);
        System.out.println("asked for a person: " + Escalation.decide(asked));
        Escalation.Case calm = new Escalation.Case(false, 1, true, 0, "calm", 95);
        System.out.println("calm and covered: " + Escalation.decide(calm));

        // Two customers match the name: ask only for the field that tells them apart.
        List<Map<String, String>> matches = List.of(
            Map.of("id", "c1", "name", "Ana Ruiz", "email", "ana@example.com", "zip", "10115"),
            Map.of("id", "c2", "name", "Ana Ruiz", "email", "ana.r@example.com", "zip", "10115"));
        System.out.println("ask for: " + Escalation.clarifyingFields(matches));

        // The hand-off carries the facts, not the transcript.
        Escalation.HandoffCase handoff = new Escalation.HandoffCase("C-77", "refund over the limit", "duplicate charge", "$129.50",
            List.of("verified identity", "checked order"), "approve the refund", "user: hello ... 40 turns ...");
        System.out.println(Escalation.handoffText(handoff));
    }
}
