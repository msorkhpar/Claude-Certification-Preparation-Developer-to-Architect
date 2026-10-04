import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    static long daysUntil(String today, String when) {
        return ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(when));
    }

    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    static List<String> retirementStatus(List<Model> models, String today) {
        List<Model> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.comparingLong((Model m) -> daysUntil(today, m.date())).thenComparing(Model::name).thenComparing(Model::tentative));
        List<String> out = new ArrayList<>();
        for (Model m : sorted) {
            long days = daysUntil(today, m.date());
            String level = days < 0 ? "retired" : days <= 14 ? "urgent" : days <= 60 ? "migrate now" : "watch";
            out.add(m.name() + ": " + days + " days, " + level + (m.tentative() ? " (tentative)" : ""));
        }
        return out;
    }

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

    static Verdict gate(List<Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
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

    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        if (requests < minRequests) return "hold at " + stage;
        if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0";
        if (stage == STAGES[STAGES.length - 1]) return "complete";
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i] == stage) return "advance to " + STAGES[i + 1];
        throw new IllegalArgumentException("unknown stage " + stage);
    }
}
