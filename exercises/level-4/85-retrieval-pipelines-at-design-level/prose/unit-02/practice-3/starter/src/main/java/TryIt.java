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

        // Two documents, split into chunks that carry their title and section.
        String annual = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.";
        String monthly = "# Monthly plan\n## Cancellation\nYou can cancel at any time.";
        List<Pipeline.Chunk> chunks = new ArrayList<>(Pipeline.chunkSections("annual", annual));
        chunks.addAll(Pipeline.chunkSections("monthly", monthly));
        for (Pipeline.Chunk chunk : chunks) System.out.println("chunk: " + chunk.id() + " | " + chunk.text());

        // Search ranks the chunks that share the most (and the most specific) words with the question.
        System.out.println("search: " + Pipeline.search(chunks, "can I cancel within 14 days", 2));
        System.out.println("search, annual only: " + Pipeline.search(chunks, "cancel", 2, Set.of("annual")));

        // The annual document changes: reindex replaces its chunks and leaves the other alone.
        String changed = annual.replace("14 days", "30 days");
        Pipeline.Reindexed result = Pipeline.reindex(chunks, Map.of("annual", changed, "monthly", monthly));
        System.out.println("reindex report: " + (result == null ? null : result.report()));
    }
}
