import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Diagnose a failure from a trace. See ../../statement.md. */
final class Diagnose {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
    static Map<String, Object> diagnose(List<Map<String, Object>> trace) {
        return map("index", -1, "type", "ok", "origin", "none", "recovery", "none", "recovered", false);
    }
}
