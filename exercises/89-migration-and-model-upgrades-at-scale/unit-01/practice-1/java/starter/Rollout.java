import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */
final class Rollout {
    private Rollout() {}

    /** One case of the regression suite, run on the old and the new model. */
    record Case(String id, String segment, boolean mustPass, boolean oldOk, boolean newOk, int oldCost, int newCost, int newMs) {}

    /** The settings of a request that matter to a migration; null means the setting is absent. */
    record Request(String model, Double temperature, Double topP, Double topK, String thinking, String toolChoice, boolean strict, boolean prefill) {}

    /** A model, the date of its retirement (ISO) and whether that date may still move later. */
    record Model(String name, String date, boolean tentative) {}

    /** The migrated request and the changes made to it. */
    record Migration(Request request, List<String> changes) {}

    /** The gate's decision (go or no-go) and its reasons. */
    record Verdict(String decision, List<String> reasons) {}

    static final int[] STAGES = {1, 5, 25, 100};
    static final String TARGET = "claude-sonnet-5-5";

    /** Whole days from one ISO date to another, negative when it has passed (written for you). */
    static long daysUntil(String today, String when) {
        return ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(when));
    }

    /** Nearest-rank percentile, 0 for no values (written for you). */
    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    static List<String> retirementStatus(List<Model> models, String today) {
        // TODO: "<name>: <days> days, <level>" per model, the nearest retirement first, with " (tentative)" when the date may move.
        return null;
    }

    static Migration migrateRequest(Request request) {
        // TODO: the new request and the list of changes: drop what the target refuses, replace what it changes, and name each change.
        return null;
    }

    static Verdict gate(List<Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
        // TODO: the decision (go or no-go) and the reasons: must-pass, protected segments, net loss, cost and tail, in that order.
        return null;
    }

    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        // TODO: "hold at S", "rollback to 0", "complete" or "advance to N" for one observed stage.
        return null;
    }
}
