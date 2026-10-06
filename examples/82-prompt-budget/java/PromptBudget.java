import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
 *
 * <p>The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
 * then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
 * Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
 * the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
 * breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
 */
public final class PromptBudget {
    private static final System.Logger LOG = System.getLogger(PromptBudget.class.getName());
    record Module(String name, boolean isStatic, String text) {}

    /** blocks in order, the estimated tokens, the tokens of the static prefix and the index of the breakpoint (null when there is none). */
    record Prompt(List<Module> blocks, int tokens, int prefixTokens, Integer breakpoint) {}

    static final int MIN_CACHEABLE = 512; // tokens, Claude Sonnet 5.5

    static final String POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26);
    static final List<Module> MODULES = List.of(
        new Module("role", true, "You are the support assistant of Northwind Outfitters. Answer from the policy only."),
        new Module("policy", true, POLICY),
        new Module("customer", false, "Customer: {customer}. Tier: {tier}."),
        new Module("question", false, "Question: {question}"));

    /** The ceiling of characters over four. */
    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    private static String fill(String text, Map<String, String> variables) {
        String out = text;
        for (Map.Entry<String, String> v : variables.entrySet()) out = out.replace("{" + v.getKey() + "}", v.getValue());
        return out;
    }

    /** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
    static Prompt assemble(List<Module> modules, Map<String, String> variables) {
        LOG.log(System.Logger.Level.DEBUG, "assemble input: {0}", modules);
        List<Module> blocks = new ArrayList<>();
        for (Module m : modules) if (m.isStatic()) blocks.add(m);
        for (Module m : modules) if (!m.isStatic()) blocks.add(new Module(m.name(), false, fill(m.text(), variables)));
        int prefix = 0;
        int total = 0;
        int lastStatic = -1;
        for (int i = 0; i < blocks.size(); i++) {
            total += tokens(blocks.get(i).text());
            if (blocks.get(i).isStatic()) {
                prefix += tokens(blocks.get(i).text());
                lastStatic = i;
            }
        }
        return new Prompt(blocks, total, prefix, lastStatic >= 0 && prefix >= MIN_CACHEABLE ? lastStatic : null);
    }

    static String cachedPrefix(Prompt prompt) {
        if (prompt.breakpoint() == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= prompt.breakpoint(); i++) out.append(prompt.blocks().get(i).text());
        return out.toString();
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        Map<String, String> ana = Map.of("customer", "Ana", "tier", "gold", "question", "Can I return a gift card?");
        Prompt first = assemble(MODULES, ana);
        StringBuilder order = new StringBuilder();
        for (Module b : first.blocks()) order.append(order.length() == 0 ? "" : " > ").append(b.name());
        System.out.println("order: " + order);
        System.out.println("tokens: " + first.tokens() + " in all, " + first.prefixTokens() + " in the static prefix, minimum " + MIN_CACHEABLE);
        System.out.println("breakpoint after: " + first.blocks().get(first.breakpoint()).name());
        Prompt second = assemble(MODULES, Map.of("customer", "Ben", "tier", "basic", "question", "Where is my parcel?"));
        System.out.println("next request, other customer: prefix identical: " + py(cachedPrefix(second).equals(cachedPrefix(first))));
        List<Module> edited = new ArrayList<>();
        for (Module m : MODULES) edited.add(m.name().equals("policy") ? new Module(m.name(), true, m.text().replace("200", "300")) : m);
        System.out.println("after a policy edit: prefix identical: " + py(cachedPrefix(assemble(edited, ana)).equals(cachedPrefix(first))));
        List<Module> noPolicy = new ArrayList<>(MODULES);
        noPolicy.remove(1);
        Prompt shortPrompt = assemble(noPolicy, Map.of("customer", "Ana", "tier", "gold", "question", "Hi"));
        System.out.println("without the policy: breakpoint " + (shortPrompt.breakpoint() == null ? "None" : shortPrompt.breakpoint()) + " because " + shortPrompt.prefixTokens() + " tokens is under " + MIN_CACHEABLE);
    }
}
