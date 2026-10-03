import java.util.Map;

/** The given types: the injected transport of the conversation, and what a reply carries. */
interface Send {
    Map<String, Object> send(Map<String, Object> body);
}

record Reply(String text, String stopReason, boolean truncated) {}
