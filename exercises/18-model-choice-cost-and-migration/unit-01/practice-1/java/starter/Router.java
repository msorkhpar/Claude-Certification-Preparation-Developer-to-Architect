import java.util.List;
import java.util.Map;

/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */
final class Router {
    private Router() {}

    static double requestCost(Map<String, Object> model, Map<String, Object> usage, boolean batch) {
        // TODO: the cost of one request in micro-dollars, rounded to 6 decimals.
        return Double.NaN;
    }

    static String route(List<Map<String, Object>> catalog, Map<String, Object> task) {
        // TODO: the id of the cheapest model that can take the task; throw NoModelError when none can.
        return null;
    }
}
