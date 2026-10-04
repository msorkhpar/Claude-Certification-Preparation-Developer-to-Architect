import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
 *
 * <p>The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
 * security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
 * This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
 * that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
 * the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
 */
public final class Deidentify {
    record Kind(String label, Pattern pattern) {}

    /** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
    record Audit(String requestId, int chars, int tokensIssued, boolean promptStored) {}

    static final List<Kind> PATTERNS = List.of(
        new Kind("EMAIL", Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")),
        new Kind("MEMBER", Pattern.compile("\\bM-\\d{6}\\b")));

    /** Replaces every match by a token; the same value always gets the same token. {@code vault} maps value to token and stays local. */
    static String tokenise(String text, Map<String, String> vault) {
        String out = text;
        for (Kind kind : PATTERNS) {
            Matcher m = kind.pattern().matcher(out);
            StringBuilder next = new StringBuilder();
            while (m.find()) {
                String value = m.group();
                if (!vault.containsKey(value)) {
                    String prefix = "<" + kind.label() + "_";
                    long same = vault.values().stream().filter(t -> t.startsWith(prefix)).count();
                    vault.put(value, prefix + (same + 1) + ">");
                }
                m.appendReplacement(next, Matcher.quoteReplacement(vault.get(value)));
            }
            m.appendTail(next);
            out = next.toString();
        }
        return out;
    }

    static String restore(String text, Map<String, String> vault) {
        String out = text;
        for (Map.Entry<String, String> e : vault.entrySet()) out = out.replace(e.getValue(), e.getKey());
        return out;
    }

    static Audit auditEntry(String requestId, String original, Map<String, String> vault) {
        return new Audit(requestId, original.length(), vault.size(), false);
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        String original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice.";
        Map<String, String> vault = new LinkedHashMap<>();
        String sent = tokenise(original, vault);
        System.out.println("sent to the model: " + sent);
        String reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.";
        System.out.println("model reply (illustrative): " + reply);
        System.out.println("restored for the user: " + restore(reply, vault));
        Audit entry = auditEntry("req-001", original, vault);
        System.out.println("audit entry: request_id=" + entry.requestId() + " chars=" + entry.chars() + " tokens_issued=" + entry.tokensIssued() + " prompt_stored=" + py(entry.promptStored()));
        System.out.println("raw values in the entry: " + py(vault.keySet().stream().anyMatch(v -> entry.toString().contains(v))));
        System.out.println("gap: the name survives tokenising: " + py(sent.contains("Jane Doe")));
    }
}
