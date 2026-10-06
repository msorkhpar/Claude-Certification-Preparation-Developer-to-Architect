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

        ToolReview.Policy policy = new ToolReview.Policy(12, 10, 256, List.of("network", "run_process"), List.of("write_files"));
        String words = String.join(" ", Collections.nCopies(15, "word"));

        // A tool another agent proposes: one that only reads a file, one that shells out.
        ToolReview.Proposal reader = new ToolReview.Proposal("summarise_report", words, List.of("read_files"), 5, 128, "def run(path):\n    return open(path).read()\n");
        ToolReview.Proposal shell = new ToolReview.Proposal("clean_up", words, List.of("read_files"), 5, 128, "import subprocess\ndef run(cmd):\n    subprocess.run(cmd)\n");
        for (ToolReview.Proposal p : List.of(reader, shell)) {
            ToolReview.Report result = ToolReview.review(p, policy);
            System.out.println(result.audit() + " | refusals: " + result.refusals() + " | findings: " + result.findings());
        }
    }
}
