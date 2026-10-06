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

        // The tool functions of the server are plain static methods, so you can call them here without starting a client.
        // (The tests start the server as a separate process and connect the SDK's client to it.)
        var saved = NotesServer.addNote(Map.of("title", "Plan", "text", "ship it"));
        System.out.println("add_note: " + ((io.modelcontextprotocol.spec.McpSchema.TextContent) saved.content().get(0)).text());
        var found = NotesServer.searchNotes(Map.of("query", "ship"));
        System.out.println("search_notes: " + ((io.modelcontextprotocol.spec.McpSchema.TextContent) found.content().get(0)).text());
        System.out.println("count resource: " + NotesServer.countText(NotesServer.NOTES.size()));
        System.out.println("note 1 resource: " + NotesServer.noteText("1"));
    }
}
