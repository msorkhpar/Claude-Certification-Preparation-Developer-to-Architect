import java.util.Map;
import java.util.function.Function;

/** A tool the model may call: its definition (name, description, input schema) and the handler that runs it. */
record Tool(String name, String description, Map<String, Object> inputSchema, Function<Map<String, Object>, Object> handler) {}
