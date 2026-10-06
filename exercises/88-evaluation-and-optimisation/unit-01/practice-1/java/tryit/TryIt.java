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

        // Results of an evaluation as (segment, correct) rows; a wrong refund costs more than a wrong order status.
        Map<String, Integer> costs = Map.of("order status", 1, "refund", 20, "policy", 5);
        List<EvalKit.Result> results = new ArrayList<>();
        for (int i = 0; i < 30; i++) results.add(new EvalKit.Result("order status", true));
        for (int i = 0; i < 5; i++) results.add(new EvalKit.Result("refund", true));
        for (int i = 0; i < 3; i++) results.add(new EvalKit.Result("refund", false));
        for (int i = 0; i < 9; i++) results.add(new EvalKit.Result("policy", true));
        results.add(new EvalKit.Result("policy", false));
        for (EvalKit.Line line : EvalKit.segmentTable(results, costs)) System.out.println("segment: " + line);

        // The same cases under the old and the new prompt: a gain in one segment must not hide a loss in a protected one.
        List<EvalKit.Paired> pairs = List.of(new EvalKit.Paired("refund", true, false), new EvalKit.Paired("policy", false, true),
            new EvalKit.Paired("policy", false, true), new EvalKit.Paired("order status", true, true));
        System.out.println("shadow gate: " + EvalKit.shadowGate(pairs, Set.of("refund")));
        System.out.println("A/B verdict: " + EvalKit.abVerdict(100, 200, 160, 200));

        // The cheapest model that is accurate and fast enough: (name, accuracy, p95 latency, cost).
        System.out.println("model: " + EvalKit.chooseModel(List.of(new EvalKit.Option("small", 88, 900, 1), new EvalKit.Option("medium", 94, 1500, 3),
            new EvalKit.Option("large", 97, 4000, 9)), 90, 2000));
    }
}
