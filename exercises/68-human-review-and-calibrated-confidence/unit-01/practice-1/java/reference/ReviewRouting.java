import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */
final class ReviewRouting {
    private ReviewRouting() {}

    static final List<String> IRREVERSIBLE = List.of("delete_records", "send_payment", "close_account");

    record Rec(String docType, String field, boolean correct) {}

    record Seg(String segment, int correct, int total, int percent) {}

    record Automation(boolean automate, List<String> failing, List<String> undersampled) {}

    record Labeled(int confidence, boolean correct) {}

    record Item(String id, String stratum, int rank) {}

    record Extraction(String id, int confidence, boolean conflict) {}

    record Routing(List<String> review, List<String> backlog, List<String> auto) {}

    private static int percent(int correct, int total) {
        return (200 * correct + total) / (2 * total);
    }

    static List<Seg> accuracyBy(List<Rec> records) {
        Map<String, int[]> groups = new TreeMap<>();
        for (Rec r : records) {
            int[] g = groups.computeIfAbsent(r.docType() + "/" + r.field(), k -> new int[2]);
            if (r.correct()) g[0]++;
            g[1]++;
        }
        int correct = 0;
        int total = 0;
        for (int[] g : groups.values()) {
            correct += g[0];
            total += g[1];
        }
        List<Seg> out = new ArrayList<>();
        out.add(new Seg("overall", correct, total, total > 0 ? percent(correct, total) : 0));
        for (Map.Entry<String, int[]> e : groups.entrySet()) out.add(new Seg(e.getKey(), e.getValue()[0], e.getValue()[1], percent(e.getValue()[0], e.getValue()[1])));
        return out;
    }

    static Automation canAutomate(List<Rec> records, int threshold, int minN) {
        List<String> failing = new ArrayList<>();
        List<String> undersampled = new ArrayList<>();
        List<Seg> segments = accuracyBy(records).subList(1, accuracyBy(records).size());
        for (Seg s : segments) {
            if (s.total() < minN) undersampled.add(s.segment());
            else if (s.percent() < threshold) failing.add(s.segment());
        }
        return new Automation(!segments.isEmpty() && failing.isEmpty() && undersampled.isEmpty(), failing, undersampled);
    }

    static Integer calibrateThreshold(List<Labeled> labeled, int target) {
        Set<Integer> levels = new java.util.TreeSet<>();
        for (Labeled l : labeled) levels.add(l.confidence());
        for (int t : levels) {
            int kept = 0;
            int right = 0;
            for (Labeled l : labeled) {
                if (l.confidence() >= t) {
                    kept++;
                    if (l.correct()) right++;
                }
            }
            if (100 * right >= target * kept) return t;
        }
        return null;
    }

    static List<String> stratifiedSample(List<Item> items, int perStratum) {
        Set<String> strata = new LinkedHashSet<>();
        for (Item i : items) strata.add(i.stratum());
        List<String> chosen = new ArrayList<>();
        for (String stratum : strata) {
            List<Item> members = new ArrayList<>();
            for (Item i : items) if (i.stratum().equals(stratum)) members.add(i);
            members.sort(Comparator.comparingInt(Item::rank).thenComparing(Item::id));
            for (int k = 0; k < Math.min(perStratum, members.size()); k++) chosen.add(members.get(k).id());
        }
        return chosen;
    }

    static Routing route(List<Extraction> extractions, int threshold, int capacity) {
        List<Extraction> candidates = new ArrayList<>();
        for (Extraction e : extractions) if (e.conflict() || e.confidence() < threshold) candidates.add(e);
        candidates.sort(Comparator.<Extraction>comparingInt(e -> e.conflict() ? 0 : e.confidence()).thenComparing(Extraction::id));
        List<String> queue = new ArrayList<>();
        for (Extraction e : candidates) queue.add(e.id());
        Set<String> flagged = new HashSet<>(queue);
        List<String> auto = new ArrayList<>();
        for (Extraction e : extractions) if (!flagged.contains(e.id())) auto.add(e.id());
        int cut = Math.min(capacity, queue.size());
        return new Routing(new ArrayList<>(queue.subList(0, cut)), new ArrayList<>(queue.subList(cut, queue.size())), auto);
    }

    static String checkpoint(String action, int amount) {
        return checkpoint(action, amount, 1000);
    }

    static String checkpoint(String action, int amount, int limit) {
        return IRREVERSIBLE.contains(action) || amount > limit ? "human" : "auto";
    }
}
