import java.util.List;
import java.util.Map;

/** Assemble a streamed Messages reply from its events. See ../../statement.md for the contract. */
final class Assemble {
    private Assemble() {}

    static Map<String, Object> assemble(List<Map<String, Object>> events) {
        // TODO: fold the events into the message a non-streaming call would have returned.
        return Map.of();
    }
}
