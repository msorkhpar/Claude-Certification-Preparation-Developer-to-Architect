import java.util.List;
import java.util.function.Function;
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

        List<Map<String, String>> files = List.of(Map.of("path", "api.py", "text", "def get(): ..."),
            Map.of("path", "db.py", "text", "def query(): ..."), Map.of("path", "ui.py", "text", "def show(): ..."));
        Map<String, Map<String, Object>> script = Map.of(
            "api", Map.of("findings", List.of("api: no auth"), "summary", "api calls db.query(id)"),
            "db", Map.of("findings", List.of(), "summary", "db.query takes a name"),
            "ui", Map.of("findings", List.of("ui: unused"), "summary", "ui shows rows"));

        // A scripted stand-in for the model reviewing one file (or one part of it).
        Decompose.FilePass filePass = (path, text, part, parts) -> script.get(path.split("\\.")[0]);
        // The second look: it reads only the summaries, and finds what no single file shows.
        Function<List<Map<String, String>>, List<String>> crossPass = summaries -> List.of("api passes id but db expects a name");

        // Each file is reviewed alone, then the cross pass reads their summaries.
        Map<String, Object> result = Decompose.reviewChanges(files, filePass, crossPass);

        if (result != null && result.get("files") instanceof Map<?, ?> reviewed) {
            reviewed.forEach((path, review) -> System.out.println(path + " -> " + ((Map<?, ?>) review).get("findings") + " | parts: " + ((Map<?, ?>) review).get("parts")));
        }
        System.out.println("cross findings: " + (result == null ? null : result.get("cross")));
        System.out.println("failed: " + (result == null ? null : result.get("failed") + " | skipped: " + result.get("skipped")));
    }
}
