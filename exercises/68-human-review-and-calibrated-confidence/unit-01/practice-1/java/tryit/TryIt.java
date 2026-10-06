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

        // Accuracy per document type and field, next to the overall figure.
        List<ReviewRouting.Rec> records = new ArrayList<>();
        for (int i = 0; i < 90; i++) records.add(new ReviewRouting.Rec("invoice", "total", true));
        for (int i = 0; i < 8; i++) records.add(new ReviewRouting.Rec("receipt", "date", true));
        for (int i = 0; i < 2; i++) records.add(new ReviewRouting.Rec("receipt", "date", false));
        System.out.println("accuracy: " + ReviewRouting.accuracyBy(records));

        // The lowest confidence at which the model still reaches the target accuracy, from labelled outcomes.
        List<ReviewRouting.Labeled> labeled = List.of(new ReviewRouting.Labeled(95, true), new ReviewRouting.Labeled(90, true),
            new ReviewRouting.Labeled(85, true), new ReviewRouting.Labeled(80, false), new ReviewRouting.Labeled(60, false));
        System.out.println("threshold for 90% accuracy: " + ReviewRouting.calibrateThreshold(labeled, 90));

        // Which extractions a person reviews: conflicts and low confidence first, up to the capacity.
        List<ReviewRouting.Extraction> extractions = List.of(new ReviewRouting.Extraction("x1", 95, false), new ReviewRouting.Extraction("x2", 60, false),
            new ReviewRouting.Extraction("x3", 90, true), new ReviewRouting.Extraction("x4", 70, false));
        System.out.println("routing: " + ReviewRouting.route(extractions, 80, 2));
        System.out.println("checkpoints: " + ReviewRouting.checkpoint("send_payment", 5) + " " + ReviewRouting.checkpoint("update_note", 50));
    }
}
