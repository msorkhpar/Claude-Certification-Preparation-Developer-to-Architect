import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * <p>The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
public final class EscalationRules {
    /** name, sentiment, asked for a person, policy silent, what a careful person would do */
    record Case(String name, String sentiment, boolean asked, boolean policySilent, String truth) {}

    record Account(String id, int lastOrder) {}

    record Example(String text, String decision, String why) {}

    static final List<Case> CASES = List.of(
        new Case("price match with another shop", "calm", false, true, "escalate"),
        new Case("wrong colour, standard exchange", "angry", false, false, "resolve"),
        new Case("calm request to speak to a person", "calm", true, false, "escalate"),
        new Case("password reset", "frustrated", false, false, "resolve"),
        new Case("refund for an item bought elsewhere", "calm", false, true, "escalate"),
        new Case("angry, wants a person now", "angry", true, false, "escalate"));

    static String bySentiment(Case c) {
        return c.sentiment().equals("calm") ? "resolve" : "escalate";
    }

    static String byCriteria(Case c) {
        return c.asked() || c.policySilent() ? "escalate" : "resolve";
    }

    static List<Integer> errors(Function<Case, String> rule) {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < CASES.size(); i++) if (!rule.apply(CASES.get(i)).equals(CASES.get(i).truth())) wrong.add(i + 1);
        return wrong;
    }

    /** The heuristic the guide rejects: choose the account with the latest order. */
    static String pickMostRecent(List<Account> matches) {
        return matches.stream().max(Comparator.comparingInt(Account::lastOrder)).orElseThrow().id();
    }

    /** What the guide asks for: no choice, a request for something that tells the matches apart. */
    static String askForIdentifier(int matches, List<String> fields) {
        return "I found " + matches + " accounts for that name. Please give me one of: " + String.join(", ", fields) + ".";
    }

    /** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
    static String escalationSection(List<String> criteria, List<Example> examples) {
        List<String> lines = new ArrayList<>(List.of("Escalate to a person when:"));
        for (String c : criteria) lines.add("- " + c);
        lines.add("");
        lines.add("Examples:");
        for (Example e : examples) lines.add("Customer: \"" + e.text() + "\" -> " + e.decision() + " (" + e.why() + ")");
        return String.join("\n", lines);
    }

    private static String join(List<Integer> numbers) {
        List<String> parts = new ArrayList<>();
        for (int n : numbers) parts.add(String.valueOf(n));
        return String.join(", ", parts);
    }

    public static void main(String[] args) {
        for (int i = 0; i < CASES.size(); i++) {
            Case c = CASES.get(i);
            System.out.println("case " + (i + 1) + " (" + c.name() + "): sentiment rule " + bySentiment(c) + ", criteria " + byCriteria(c) + ", careful person " + c.truth());
        }
        System.out.println("sentiment rule routed " + errors(EscalationRules::bySentiment).size() + " of " + CASES.size() + " wrongly: cases " + join(errors(EscalationRules::bySentiment)));
        System.out.println("criteria routed " + errors(EscalationRules::byCriteria).size() + " of " + CASES.size() + " wrongly");
        List<Account> matches = List.of(new Account("c1", 20260901), new Account("c2", 20260915));
        System.out.println("heuristic: the agent acts on " + pickMostRecent(matches) + " although the customer may be c1");
        System.out.println(askForIdentifier(matches.size(), List.of("the email on the account", "the postcode")));
        System.out.println(escalationSection(List.of("the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"),
            List.of(new Example("Can you match the price on another site?", "escalate", "the policy only covers our own prices"),
                new Example("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"))));
    }
}
