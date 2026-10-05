import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */
final class Rollout {
    private static final System.Logger LOG = System.getLogger(Rollout.class.getName());
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

    /**
     * TODO 1 of 9 (unlocks e7): one line of the retirement calendar.
     * Receives the days left, the model name and whether the date is tentative. Returns `<name>: <days> days, <level>` with ` (tentative)`
     * added when it is tentative; the level is `retired` below 0, `urgent` up to 14, `migrate now` up to 60, else `watch`.
     * Example: statusLine(14, "a", true) -> "a: 14 days, urgent (tentative)"
     */
    static String statusLine(long days, String name, boolean tentative) {
        return "";
    }

    static List<String> retirementStatus(List<Model> models, String today) {
        LOG.log(System.Logger.Level.DEBUG, "retirementStatus input: {0}", models);
        List<Model> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.comparingLong((Model m) -> daysUntil(today, m.date())).thenComparing(Model::name).thenComparing(Model::tentative));
        List<String> out = new ArrayList<>();
        for (Model m : sorted) out.add(statusLine(daysUntil(today, m.date()), m.name(), m.tentative()));
        return out;
    }

    /**
     * TODO 2 of 9 (unlocks e8): migrate the thinking setting.
     * Receives the thinking setting and the list of changes so far. `budget` becomes `adaptive` and `disabled` becomes `between_tools`, each
     * adding its sentence from the statement to `changes`; anything else is kept. Returns the new setting.
     * Example: migrateThinking("disabled", changes) -> "between_tools", and changes gains "thinking disabled replaced by between_tools"
     */
    static String migrateThinking(String thinking, List<String> changes) {
        return thinking;
    }

    static Migration migrateRequest(Request request) {
        List<String> changes = new ArrayList<>();
        if (!request.model().equals(TARGET)) changes.add("model set to " + TARGET);
        if (request.temperature() != null) changes.add("removed temperature");
        if (request.topP() != null) changes.add("removed top_p");
        if (request.topK() != null) changes.add("removed top_k");
        String thinking = migrateThinking(request.thinking(), changes);
        String toolChoice = request.toolChoice();
        boolean strict = request.strict();
        if (toolChoice.equals("any") || toolChoice.equals("tool")) {
            toolChoice = "auto";
            strict = true;
            changes.add("forced tool choice replaced by auto with strict tools");
        }
        if (request.prefill()) changes.add("assistant prefill removed; state the format in the instructions");
        return new Migration(new Request(TARGET, null, null, null, thinking, toolChoice, strict, false), changes);
    }

    /**
     * TODO 3 of 9 (unlocks e1): the reasons for failed must-pass cases.
     * Receives the cases. Returns a list with `must-pass failed: <ids>` (sorted, joined by `, `) for the cases marked must pass that the new
     * model fails, or an empty list. Example: one failing must-pass case a1 -> ["must-pass failed: a1"]
     */
    static List<String> mustPass(List<Case> cases) {
        return List.of();
    }

    /**
     * TODO 4 of 9 (unlocks e2): the reason for a protected segment that lost answers.
     * Receives the cases and the protected segments. Returns a list with `protected segment lost answers: <segments>` (sorted, distinct)
     * for the protected segments with a case the old model got right and the new one did not, or an empty list.
     * Example: refund lost b3 -> ["protected segment lost answers: refund"]
     */
    static List<String> protectedLost(List<Case> cases, Set<String> protectedSegments) {
        return List.of();
    }

    /**
     * TODO 5 of 9 (unlocks e3): the reason for more losses than gains.
     * Receives the cases. Returns a list with `net loss: lost N, gained M` when the cases lost (old right, new wrong) outnumber the cases
     * gained (the opposite), or an empty list. Example: 2 lost and 1 gained -> ["net loss: lost 2, gained 1"]
     */
    static List<String> netLoss(List<Case> cases) {
        return List.of();
    }

    /**
     * TODO 6 of 9 (unlocks e4): the reason for a cost rise over the limit.
     * Receives the cases and the largest allowed rise in whole percent. The rise is the new total cost over the old one, rounded down, 0 when
     * the old total is 0 or the cost fell. Returns a list with `cost up X% over the Y% limit` when X is above Y, or an empty list.
     * Example: costs 40 -> 50 with a limit of 20 -> ["cost up 25% over the 20% limit"]; costs 40 -> 48 with a limit of 20 -> []
     */
    static List<String> costRise(List<Case> cases, int maxCostUp) {
        return List.of();
    }

    /**
     * TODO 7 of 9 (unlocks e5): the reason for a slow tail.
     * Receives the cases and the largest allowed 95th-percentile time in ms. Uses `percentile` over the `newMs` of the cases. Returns a list
     * with `p95 latency X ms over the Y ms limit` when X is above Y, or an empty list. Example: p95 3000 with a limit of 2000 -> one reason
     */
    static List<String> latency(List<Case> cases, int maxP95) {
        return List.of();
    }

    /**
     * TODO 8 of 9 (unlocks m1): the decision of the gate.
     * Receives the list of reasons. Returns "no-go" when there is any reason, "go" when there is none. Example: decision(List.of()) -> "go"
     */
    static String decision(List<String> reasons) {
        return "";
    }

    static Verdict gate(List<Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
        List<String> reasons = new ArrayList<>();
        reasons.addAll(mustPass(cases));
        reasons.addAll(protectedLost(cases, protectedSegments));
        reasons.addAll(netLoss(cases));
        reasons.addAll(costRise(cases, maxCostUp));
        reasons.addAll(latency(cases, maxP95));
        return new Verdict(decision(reasons), reasons);
    }

    /**
     * TODO 9 of 9 (unlocks e6): the step of a staged roll-out.
     * Receives the stage (1, 5, 25 or 100), the requests and errors seen, the fewest requests to judge and the most errors per 1000.
     * Returns "hold at <stage>" with too few requests, "rollback to 0" when errors per 1000 (rounded down) pass the limit, "complete" at
     * stage 100 when healthy, else "advance to <next stage>". Example: rolloutStep(1, 2000, 6, 1000, 5) -> "advance to 5"
     */
    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        return "";
    }
}
