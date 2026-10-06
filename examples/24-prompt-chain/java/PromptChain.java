import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A two-step prompt chain with versioned templates, against a scripted model.
 *
 * <p>The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
 */
public final class PromptChain {
    private static final System.Logger LOG = System.getLogger(PromptChain.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    /** A versioned template: a system prompt and a user prompt with {{name}} placeholders. */
    record Template(String system, String user) {}

    static final Map<String, Template> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("extract-quotes@2", new Template(
            "You answer questions about company documents and use only what the documents say.",
            "{{documents}}\n\nFirst quote the passages that bear on the question, each in <quote> tags inside <quotes>. If nothing bears on it, write <quotes></quotes>.\n\n<question>{{question}}</question>"));
        TEMPLATES.put("answer-from-quotes@1", new Template(
            "You answer from quoted evidence. If the quotes do not settle the question, say what is missing.",
            "{{quotes}}\n\nAnswer the question in one or two sentences, and say which quote supports each claim.\n\n<question>{{question}}</question>"));
    }

    static final List<String[]> DOCUMENTS = List.of(
        new String[] {"travel-policy.txt",
            "Flights above 400 dollars need approval from a manager before booking. Economy class is the default for flights under six hours."},
        new String[] {"expenses-faq.txt",
            "Meals are reimbursed up to 60 dollars a day when travelling. Receipts are required for every claim over 25 dollars."});
    static final String QUESTION = "Does a 450 dollar economy flight need approval?";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /** Fill {{name}} placeholders in one pass; a value is data and is never read as a template. */
    static String render(String template, Map<String, String> values) {
        TreeSet<String> missing = new TreeSet<>();
        Matcher scan = PLACEHOLDER.matcher(template);
        while (scan.find()) if (!values.containsKey(scan.group(1))) missing.add(scan.group(1));
        if (!missing.isEmpty()) throw new NoSuchElementException("unfilled variables: " + py(List.copyOf(missing)));
        return PLACEHOLDER.matcher(template).replaceAll(m -> Matcher.quoteReplacement(values.get(m.group(1))));
    }

    static String documentsBlock(List<String[]> documents) {
        StringBuilder body = new StringBuilder();
        int i = 1;
        for (String[] d : documents) {
            body.append("<document index=\"").append(i++).append("\">\n<source>").append(d[0]).append("</source>\n<document_content>\n").append(d[1]).append("\n</document_content>\n</document>\n");
        }
        return "<documents>\n" + body + "</documents>";
    }

    static Message step(AnthropicClient client, String version, Map<String, String> values) {
        Template template = TEMPLATES.get(version);
        return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(500).system(template.system())
            .addUserMessage(render(template.user(), values)).build());
    }

    static final List<Object> REPLIES = List.of(
        message(List.of(text("<quotes>\n<quote>Flights above 400 dollars need approval from a manager before booking.</quote>\n</quotes>"))),
        message(List.of(text("Yes: at 450 dollars it is above the 400 dollar limit, so it needs a manager's approval first (quote 1)."))));

    static Map<String, String> values(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(REPLIES.toArray());
        Message first = step(rig.client(), "extract-quotes@2", values("documents", documentsBlock(DOCUMENTS), "question", QUESTION));
        String quotes = first.content().get(0).asText().text();
        Message second = step(rig.client(), "answer-from-quotes@1", values("quotes", quotes, "question", QUESTION));
        List<JsonNode> requests = rig.http().requests;
        String one = requests.get(0).at("/messages/0/content").asText();
        String two = requests.get(1).at("/messages/0/content").asText();
        System.out.println("templates used: " + py(List.of("extract-quotes@2", "answer-from-quotes@1")));
        System.out.println("step 1: documents come before the question: " + py(one.indexOf("<documents>") < one.indexOf("<question>")));
        System.out.println("step 1: the prompt ends with the question: " + py(one.stripTrailing().endsWith("</question>")));
        System.out.println("step 2: the full documents are not resent: " + py(!two.contains("<documents>")) + " (" + one.length() + " characters then " + two.length() + ")");
        System.out.println("last message of each request is a user turn (no prefill): "
            + py(requests.stream().map(r -> r.get("messages").get(r.get("messages").size() - 1).get("role").asText()).toList()));
        TreeSet<String> sampling = new TreeSet<>();
        for (String field : List.of("temperature", "top_p", "top_k")) if (requests.get(0).has(field)) sampling.add(field);
        System.out.println("sampling parameters sent: " + (sampling.isEmpty() ? "none" : py(List.copyOf(sampling))));
        System.out.println("system prompts differ per step: " + py(!requests.get(0).get("system").equals(requests.get(1).get("system"))));
        System.out.println("data is not read as a template: " + py(render(TEMPLATES.get("answer-from-quotes@1").user(), values("quotes", "{{question}} stays", "question", "Q?")).split("\\{\\{question\\}\\} stays", -1).length - 1 == 1));
        try {
            render(TEMPLATES.get("answer-from-quotes@1").user(), values("quotes", quotes));
        } catch (NoSuchElementException err) {
            System.out.println("a missing variable is an error: " + err.getMessage());
        }
        System.out.println("step 1 output: " + quotes.replace("\n", " "));
        System.out.println("step 2 output: " + second.content().get(0).asText().text());
    }
}
