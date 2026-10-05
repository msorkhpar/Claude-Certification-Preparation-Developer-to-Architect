import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
public class RolloutGate {
    private static final System.Logger LOG = System.getLogger(RolloutGate.class.getName());
    record Case(String id, String segment, boolean mustPass, boolean oldOk, boolean newOk, int oldCost, int newCost, int newMs) {}

    record Request(String model, Double temperature, Double topP, Double topK, String thinking, String toolChoice, boolean strict, boolean prefill) {}

    record Model(String name, String when, boolean tentative) {}

    record Migration(Request request, List<String> changes) {}

    record Verdict(String decision, List<String> reasons) {}

    static final int[] STAGES = {1, 5, 25, 100};
    static final String TARGET = "claude-sonnet-5-5";

    static long daysUntil(String today, String when) {
        return ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(when));
    }

    static String level(long days) {
        if (days < 0) return "retired";
        if (days <= 14) return "urgent";
        return days <= 60 ? "migrate now" : "watch";
    }

    /** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
    static List<String> retirementStatus(List<Model> models, String today) {
        List<Model> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.comparingLong((Model m) -> daysUntil(today, m.when())).thenComparing(Model::name).thenComparing(Model::tentative));
        List<String> out = new ArrayList<>();
        for (Model m : sorted) {
            long days = daysUntil(today, m.when());
            out.add(m.name() + ": " + days + " days, " + level(days) + (m.tentative() ? " (tentative)" : ""));
        }
        return out;
    }

    /** The settings the target model refuses are removed or replaced, and each change is named. */
    static Migration migrateRequest(Request request) {
        List<String> changes = new ArrayList<>();
        if (!request.model().equals(TARGET)) changes.add("model set to " + TARGET);
        if (request.temperature() != null) changes.add("removed temperature");
        if (request.topP() != null) changes.add("removed top_p");
        if (request.topK() != null) changes.add("removed top_k");
        String thinking = request.thinking();
        if (thinking.equals("budget")) {
            thinking = "adaptive";
            changes.add("thinking budget replaced by adaptive thinking; sweep the effort");
        } else if (thinking.equals("disabled")) {
            thinking = "between_tools";
            changes.add("thinking disabled replaced by between_tools");
        }
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

    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    /** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
    static Verdict gate(List<Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
        LOG.log(System.Logger.Level.DEBUG, "gate input: {0}", cases);
        List<String> reasons = new ArrayList<>();
        TreeSet<String> failed = new TreeSet<>();
        TreeSet<String> hit = new TreeSet<>();
        int lost = 0;
        int gained = 0;
        int oldTotal = 0;
        int newTotal = 0;
        List<Integer> times = new ArrayList<>();
        for (Case c : cases) {
            if (c.mustPass() && !c.newOk()) failed.add(c.id());
            if (c.oldOk() && !c.newOk()) {
                lost++;
                if (protectedSegments.contains(c.segment())) hit.add(c.segment());
            }
            if (c.newOk() && !c.oldOk()) gained++;
            oldTotal += c.oldCost();
            newTotal += c.newCost();
            times.add(c.newMs());
        }
        if (!failed.isEmpty()) reasons.add("must-pass failed: " + String.join(", ", failed));
        if (!hit.isEmpty()) reasons.add("protected segment lost answers: " + String.join(", ", hit));
        if (lost > gained) reasons.add("net loss: lost " + lost + ", gained " + gained);
        int up = oldTotal > 0 && newTotal > oldTotal ? (newTotal - oldTotal) * 100 / oldTotal : 0;
        if (up > maxCostUp) reasons.add("cost up " + up + "% over the " + maxCostUp + "% limit");
        int p95 = percentile(times, 95);
        if (p95 > maxP95) reasons.add("p95 latency " + p95 + " ms over the " + maxP95 + " ms limit");
        return new Verdict(reasons.isEmpty() ? "go" : "no-go", reasons);
    }

    /** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        if (requests < minRequests) return "hold at " + stage;
        if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0";
        if (stage == STAGES[STAGES.length - 1]) return "complete";
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i] == stage) return "advance to " + STAGES[i + 1];
        throw new IllegalArgumentException("unknown stage " + stage);
    }

    static List<Case> suite() {
        List<Case> rows = new ArrayList<>();
        int[] billing = {900, 950, 1000, 1100, 1200};
        for (int i = 1; i <= 5; i++) rows.add(new Case("b" + i, "billing", true, true, true, 4, 5, billing[i - 1]));
        int[] refund = {1500, 1600, 1700, 1800, 2100};
        for (int i = 1; i <= 5; i++) rows.add(new Case("r" + i, "refund", i <= 2, true, i != 4, 6, 8, refund[i - 1]));
        for (int i = 1; i <= 10; i++) rows.add(new Case("f" + i, "faq", false, !(i == 8 || i == 9 || i == 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i / 2)));
        return rows;
    }

    public static void main(String[] args) {
        List<Model> models = List.of(new Model("claude-haiku-4-5-20251001", "2026-10-15", true), new Model("claude-sonnet-4-5-20250929", "2026-11-30", false), new Model("claude-opus-4-1-20250805", "2026-08-05", false));
        System.out.println("retirement calendar on 2026-10-04:");
        for (String line : retirementStatus(models, "2026-10-04")) System.out.println("  " + line);
        Request old = new Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true);
        Migration migration = migrateRequest(old);
        System.out.println("request for " + migration.request().model() + ": " + migration.changes().size() + " changes");
        for (String change : migration.changes()) System.out.println("  " + change);
        List<Case> cases = suite();
        Verdict first = gate(cases, Set.of("refund"), 25, 2000);
        System.out.println("gate on " + cases.size() + " cases: " + first.decision());
        for (String reason : first.reasons()) System.out.println("  " + reason);
        List<Case> fixed = new ArrayList<>();
        for (Case c : cases) fixed.add(c.id().equals("r4") ? new Case(c.id(), c.segment(), c.mustPass(), c.oldOk(), true, c.oldCost(), c.newCost(), c.newMs()) : c);
        Verdict second = gate(fixed, Set.of("refund"), 40, 2000);
        System.out.println("gate after the refund fix, cost limit 40%: " + second.decision() + ", " + second.reasons().size() + " reasons");
        int[][] observations = {{1, 2000, 6}, {5, 300, 0}, {5, 10000, 20}, {25, 50000, 400}, {100, 50000, 10}};
        for (int[] o : observations) System.out.println("roll-out at " + o[0] + "% with " + o[1] + " requests and " + o[2] + " errors: " + rolloutStep(o[0], o[1], o[2], 1000, 5));
    }
}
