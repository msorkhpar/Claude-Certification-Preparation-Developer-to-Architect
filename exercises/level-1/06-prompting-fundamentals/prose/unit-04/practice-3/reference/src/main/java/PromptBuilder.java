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
        return String.join("\n\n", parts);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String lookup(String name, Map<String, String> variables) {
        if (!variables.containsKey(name)) throw new IllegalArgumentException("missing variable: " + name);
        return variables.get(name);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String renderDocument(int index, Doc doc) {
        String name = escape(doc.name()).replace("\"", "&quot;");
        return "<document index=\"" + index + "\" name=\"" + name + "\">\n" + escape(doc.text()) + "\n</document>";
    }

    private static String renderExample(int index, Example ex, Map<String, String> variables) {
        return "<example index=\"" + index + "\">\n"
            + block("input", fill(ex.input(), variables)) + "\n"
            + block("output", fill(ex.output(), variables)) + "\n</example>";
    }

    private static String constraintLines(List<String> constraints, Map<String, String> variables) {
        return constraints.stream().map(c -> "- " + fill(c, variables)).collect(Collectors.joining("\n"));
    }

    private static void checkTask(String task) {
        if (!present(task)) throw new IllegalArgumentException("task is required");
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
