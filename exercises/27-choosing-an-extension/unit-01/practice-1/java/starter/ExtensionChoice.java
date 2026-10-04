import java.util.Map;

/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
final class ExtensionChoice {
    private ExtensionChoice() {}

    record Choice(String mechanism, String reason) {}

    static Choice choose(Map<String, Object> situation) {
        // TODO: the mechanism and the reason code for a situation (a map whose missing keys take the defaults in the statement).
        return null;
    }
}
