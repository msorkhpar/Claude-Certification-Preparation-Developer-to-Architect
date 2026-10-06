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

        // A stand-in for a search subagent, like the tests use: it times out once, then answers.
        ErrorFlow.Outcome news = ErrorFlow.searchWithRecovery("news", (query, attempt) ->
            attempt == 1 ? new ErrorFlow.Reply("error", "timeout", List.of(), List.of()) : new ErrorFlow.Reply("ok", null, List.of("a", "b"), List.of()));
        System.out.println("transient failure: " + news);

        // A search the agent may not run: the failure is not transient.
        ErrorFlow.Outcome filings = ErrorFlow.searchWithRecovery("filings", (query, attempt) -> new ErrorFlow.Reply("error", "permission", List.of(), List.of()));
        System.out.println("permission failure: " + filings);

        // What the coordinator does with each outcome, and what the final report admits.
        Map<String, ErrorFlow.Outcome> results = new LinkedHashMap<>();
        results.put("news", news);
        results.put("filings", filings);
        System.out.println("plan: " + ErrorFlow.coordinatorPlan(results));
        System.out.println(ErrorFlow.coverageNote(results, List.of("news", "filings", "patents")));
    }
}
