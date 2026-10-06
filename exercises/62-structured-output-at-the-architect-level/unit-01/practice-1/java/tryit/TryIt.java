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

        String doc = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you.";
        Map<String, Object> good = good();

        // A stand-in for the model, like the tests use: it always answers with the same record.
        Map<String, Object> result = Extraction.extractDocument(doc, (document, feedback) -> {
            System.out.println("model called, feedback: " + feedback);
            return good();
        }, List.of(), 2);
        System.out.println("status: " + (result == null ? null : result.get("status")));
        System.out.println("attempts: " + (result == null ? null : result.get("attempts")));

        // validate() on its own: a record whose calculated total disagrees with its line items.
        Map<String, Object> bad = good();
        bad.put("calculated_total", 99.0);
        System.out.println("errors: " + Extraction.validate(bad, doc));
    }

    static Map<String, Object> good() {
        Map<String, Object> provenance = new LinkedHashMap<>();
        provenance.put("vendor", "Invoice from Acme Tools");
        provenance.put("currency", "120.50 EUR");
        provenance.put("stated_total", "Total due: 120.50 EUR");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vendor", "Acme Tools");
        m.put("currency", "EUR");
        m.put("currency_detail", null);
        m.put("line_items", List.of(100.0, 20.5));
        m.put("stated_total", 120.5);
        m.put("calculated_total", 120.5);
        m.put("conflict_detected", false);
        m.put("provenance", provenance);
        return m;
    }
}
