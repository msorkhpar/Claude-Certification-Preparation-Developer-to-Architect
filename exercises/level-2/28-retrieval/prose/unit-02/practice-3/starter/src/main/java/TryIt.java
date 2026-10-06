import java.util.List;
import java.util.Map;
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

        // A tiny corpus like the test fixture: two documents, cut into windows of 14 words that overlap by 4.
        List<Doc> corpus = List.of(
            new Doc("refunds", "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the "
                + "original payment method within five business days. Digital goods are not refundable after download."),
            new Doc("shipping", "Shipping: orders over 50 euros ship free of charge. Standard delivery takes three to five business "
                + "days, express delivery takes one day."));
        List<Chunk> chunks = Retrieval.buildChunks(corpus, 14, 4);
        System.out.println("chunks: " + chunks.stream().map(Chunk::id).toList());

        String query = "free shipping threshold";
        for (String mode : List.of("bm25", "embedding", "hybrid")) {
            System.out.println(mode + " top 3: " + Retrieval.retrieve(chunks, query, mode, 3));
        }

        // Recall@3 over one question whose answer we know.
        System.out.println("hybrid recall@3: " + (chunks.isEmpty() ? null : Retrieval.evaluate(chunks, List.of(new Query(query, List.of("shipping"))), "hybrid", 3)));
    }
}
