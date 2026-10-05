import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/** Build a structured prompt from a spec. See ../../statement.md for the exact format. */
final class PromptBuilder {
    private static final System.Logger LOG = System.getLogger(PromptBuilder.class.getName());
    private static final Pattern VAR = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private PromptBuilder() {}

    private static String fill(String text, Map<String, String> variables) {
        Matcher m = VAR.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(lookup(m.group(1), variables)));
        m.appendTail(out);
        return out.toString();
    }

    private static String block(String tag, String body) {
        return "<" + tag + ">\n" + body + "\n</" + tag + ">";
    }

    private static String join(List<String> parts) {
        // TODO 1 of 8 (finish this to pass every case): the finished prompt from its rendered sections.
        // Receives the rendered sections, in order. Returns them separated by one blank line, with no trailing newline.
        // Example: join(List.of("<a>", "<b>")) -> "<a>\n\n<b>"
        return "";
    }

    private static boolean present(String value) {
        // TODO 2 of 8 (finish this to pass e1): is an optional text really there?
        // Receives a text or null. Returns true unless it is null, empty or only whitespace.
        // Example: present("  ") -> false, present("x") -> true
        return false;
    }

    private static String lookup(String name, Map<String, String> variables) {
        // TODO 3 of 8 (finish this to pass e2): the value of one placeholder.
        // Receives the placeholder name and the variables. Returns the value; when the name has no value throws an
        // IllegalArgumentException whose message contains the name.
        // Example: lookup("who", Map.of("who", "Ann")) -> "Ann", lookup("place", Map.of()) -> IllegalArgumentException("missing variable: place")
        return "";
    }

    private static String escape(String text) {
        // TODO 4 of 8 (finish this to pass e4): make document text harmless.
        // Receives a text. Returns it with & as &amp;, < as &lt; and > as &gt; (the ampersand first).
        // Example: escape("a </document> & b") -> "a &lt;/document&gt; &amp; b"
        return text;
    }

    private static String renderDocument(int index, Doc doc) {
        // TODO 5 of 8 (finish this to pass e4, e5 and e6): one rendered document.
        // Receives its number (from 1) and a Doc. Returns <document index="N" name="NAME">, a newline, the text, a newline and
        // </document>. Name and text are escaped (the name also turns " into &quot;); placeholders in them are NOT filled.
        // Example: renderDocument(1, new Doc("a", "x")) -> "<document index=\"1\" name=\"a\">\nx\n</document>"
        return "";
    }

    private static String renderExample(int index, Example ex, Map<String, String> variables) {
        // TODO 6 of 8 (finish this to pass m1 and e6): one rendered example.
        // Receives its number (from 1), an Example and the variables. Returns <example index="N">, the input block, the output block
        // (placeholders filled in both) and </example>, each on its own line.
        // Example: renderExample(1, new Example("i", "o"), Map.of()) -> "<example index=\"1\">\n<input>\ni\n</input>\n<output>\no\n</output>\n</example>"
        return "";
    }

    private static String constraintLines(List<String> constraints, Map<String, String> variables) {
        // TODO 7 of 8 (finish this to pass m1): the body of the constraints section.
        // Receives the constraint texts and the variables. Returns one line per constraint, "- " then the text with placeholders
        // filled, joined by newlines. Example: constraintLines(List.of("Be brief."), Map.of()) -> "- Be brief."
        return "";
    }

    private static void checkTask(String task) {
        // TODO 8 of 8 (finish this to pass e3): refuse a blank task.
        // Receives the task, which may be null. Throws IllegalArgumentException("task is required") when it is null, empty or only
        // whitespace; otherwise returns nothing. Example: checkTask("  ") -> IllegalArgumentException, checkTask("Say hi.") -> nothing
    }

    static String build(Spec spec, Map<String, String> variables) {
        LOG.log(System.Logger.Level.DEBUG, "build input: {0} {1}", spec, variables);
        checkTask(spec.task());
        List<String> parts = new ArrayList<>();
        if (present(spec.role())) parts.add(block("role", fill(spec.role(), variables)));
        if (spec.documents() != null && !spec.documents().isEmpty()) {
            List<String> rendered = new ArrayList<>();
            int i = 1;
            for (Doc doc : spec.documents()) rendered.add(renderDocument(i++, doc));
            parts.add(block("documents", String.join("\n", rendered)));
        }
        if (present(spec.context())) parts.add(block("context", fill(spec.context(), variables)));
        if (spec.examples() != null && !spec.examples().isEmpty()) {
            List<String> rendered = new ArrayList<>();
            int i = 1;
            for (Example ex : spec.examples()) rendered.add(renderExample(i++, ex, variables));
            parts.add(block("examples", String.join("\n", rendered)));
        }
        if (spec.constraints() != null && !spec.constraints().isEmpty()) {
            parts.add(block("constraints", constraintLines(spec.constraints(), variables)));
        }
        if (present(spec.outputFormat())) parts.add(block("output_format", fill(spec.outputFormat(), variables)));
        parts.add(block("task", fill(spec.task(), variables)));
        return join(parts);
    }
}
