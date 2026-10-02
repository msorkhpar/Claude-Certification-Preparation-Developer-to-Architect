import java.util.*;
import java.util.function.Function;

final class Agent {
    interface Model { Map<String, Object> call(List<Map<String, Object>> messages); }

    static String run(Model model, Map<String, Function<Map<String, Object>, Object>> tools, String userText, int maxTurns) {
        return null; // replace with the loop
    }
}
