import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/** Build a structured prompt from a spec. Reference solution. */
final class PromptBuilder {
    private static final Pattern VAR = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private PromptBuilder() {}

    private static String fill(String text, Map<String, String> variables) {
        Matcher m = VAR.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            if (!variables.containsKey(name)) throw new IllegalArgumentException("missing variable: " + name);
            m.appendReplacement(out, Matcher.quoteReplacement(variables.get(name)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String block(String tag, String body) {
        return "<" + tag + ">\n" + body + "\n</" + tag + ">";
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    static String build(Spec spec, Map<String, String> variables) {
        if (!present(spec.task())) throw new IllegalArgumentException("task is required");
        List<String> parts = new ArrayList<>();
        parts.add(block("task", fill(spec.task(), variables)));
        if (present(spec.role())) parts.add(block("role", fill(spec.role(), variables)));
        if (spec.documents() != null && !spec.documents().isEmpty()) {
            List<String> rendered = new ArrayList<>();
            int i = 1;
            for (Doc doc : spec.documents()) {
                String name = escape(doc.name()).replace("\"", "&quot;");
                rendered.add("<document index=\"" + i++ + "\" name=\"" + name + "\">\n" + escape(doc.text()) + "\n</document>");
            }
            parts.add(block("documents", String.join("\n", rendered)));
        }
        if (present(spec.context())) parts.add(block("context", fill(spec.context(), variables)));
        if (spec.examples() != null && !spec.examples().isEmpty()) {
            List<String> rendered = new ArrayList<>();
            int i = 1;
            for (Example ex : spec.examples()) {
                rendered.add("<example index=\"" + i++ + "\">\n"
                    + block("input", fill(ex.input(), variables)) + "\n"
                    + block("output", fill(ex.output(), variables)) + "\n</example>");
            }
            parts.add(block("examples", String.join("\n", rendered)));
        }
        if (spec.constraints() != null && !spec.constraints().isEmpty()) {
            String lines = spec.constraints().stream().map(c -> "- " + fill(c, variables)).collect(Collectors.joining("\n"));
            parts.add(block("constraints", lines));
        }
        if (present(spec.outputFormat())) parts.add(block("output_format", fill(spec.outputFormat(), variables)));
        return String.join("\n\n", parts);
    }
}
