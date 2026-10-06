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

    static String statusLine(long days, String name, boolean tentative) {
        String level = days < 0 ? "retired" : days <= 14 ? "urgent" : days <= 60 ? "migrate now" : "watch";
        return name + ": " + days + " days, " + level + (tentative ? " (tentative)" : "");
    }

    static List<String> retirementStatus(List<Model> models, String today) {
        LOG.log(System.Logger.Level.DEBUG, "retirementStatus input: {0}", models);
        List<Model> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.comparingLong((Model m) -> daysUntil(today, m.date())).thenComparing(Model::name).thenComparing(Model::tentative));
        List<String> out = new ArrayList<>();
        for (Model m : sorted) out.add(statusLine(daysUntil(today, m.date()), m.name(), m.tentative()));
        return out;
    }

    static String migrateThinking(String thinking, List<String> changes) {
        if (thinking.equals("budget")) {
            changes.add("thinking budget replaced by adaptive thinking; sweep the effort");
            return "adaptive";
        }
        if (thinking.equals("disabled")) {
            changes.add("thinking disabled replaced by between_tools");
            return "between_tools";
        }
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

    static List<String> mustPass(List<Case> cases) {
        TreeSet<String> failed = new TreeSet<>();
        for (Case c : cases) if (c.mustPass() && !c.newOk()) failed.add(c.id());
        return failed.isEmpty() ? List.of() : List.of("must-pass failed: " + String.join(", ", failed));
    }

    static List<String> protectedLost(List<Case> cases, Set<String> protectedSegments) {
        TreeSet<String> hit = new TreeSet<>();
        for (Case c : cases) if (c.oldOk() && !c.newOk() && protectedSegments.contains(c.segment())) hit.add(c.segment());
        return hit.isEmpty() ? List.of() : List.of("protected segment lost answers: " + String.join(", ", hit));
    }

    static List<String> netLoss(List<Case> cases) {
        long lost = cases.stream().filter(c -> c.oldOk() && !c.newOk()).count();
        long gained = cases.stream().filter(c -> c.newOk() && !c.oldOk()).count();
        return lost > gained ? List.of("net loss: lost " + lost + ", gained " + gained) : List.of();
    }

    static List<String> costRise(List<Case> cases, int maxCostUp) {
        int oldTotal = cases.stream().mapToInt(Case::oldCost).sum();
        int newTotal = cases.stream().mapToInt(Case::newCost).sum();
        int up = oldTotal > 0 && newTotal > oldTotal ? (newTotal - oldTotal) * 100 / oldTotal : 0;
        return up > maxCostUp ? List.of("cost up " + up + "% over the " + maxCostUp + "% limit") : List.of();
    }

    static List<String> latency(List<Case> cases, int maxP95) {
        int p95 = percentile(cases.stream().map(Case::newMs).toList(), 95);
        return p95 > maxP95 ? List.of("p95 latency " + p95 + " ms over the " + maxP95 + " ms limit") : List.of();
    }

    static String decision(List<String> reasons) {
        return reasons.isEmpty() ? "go" : "no-go";
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

    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        if (requests < minRequests) return "hold at " + stage;
        if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0";
        if (stage == STAGES[STAGES.length - 1]) return "complete";
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i] == stage) return "advance to " + STAGES[i + 1];
        throw new IllegalArgumentException("unknown stage " + stage);
    }
}
