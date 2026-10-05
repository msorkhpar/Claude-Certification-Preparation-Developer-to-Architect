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
    private static final System.Logger LOG = System.getLogger(ReviewRouting.class.getName());
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
        LOG.log(System.Logger.Level.DEBUG, "accuracyBy input: {0}", records);
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
        // TODO 1 of 7 (finish this to pass m1, e1): the segment rows. After the overall row, add one row for each
        //   segment ("doc_type/field"), sorted by name, with its correct count, its total and the rounded percent.
        //   Example: invoice/total 8 of 10 -> {segment: invoice/total, correct 8, total 10, percent 80}.
        for (Map.Entry<String, int[]> e : groups.entrySet()) out.add(new Seg(e.getKey(), e.getValue()[0], e.getValue()[1], out.get(0).percent()));
        return out;
    }

    static Automation canAutomate(List<Rec> records, int threshold, int minN) {
        List<String> failing = new ArrayList<>();
        List<String> undersampled = new ArrayList<>();
        List<Seg> segments = accuracyBy(records).subList(1, accuracyBy(records).size());
        for (Seg s : segments) {
            // TODO 2 of 7 (finish this to pass e2): the sort of the segments. For each segment (not the overall row):
            //   when its total is below min_n it is undersampled; otherwise when its percent is below the threshold it is
            //   failing. Example: min_n 5, segment with 4 records -> undersampled; 5 records at 70 percent, threshold 90
            //   -> failing.
        }
        return new Automation(!segments.isEmpty() && failing.isEmpty() && undersampled.isEmpty(), failing, undersampled);
    }

    static Integer calibrateThreshold(List<Labeled> labeled, int target) {
        Set<Integer> levels = new java.util.TreeSet<>();
        for (Labeled l : labeled) levels.add(l.confidence());
        // TODO 3 of 7 (finish this to pass e3, e4): the threshold. Receives the labelled items (confidence, correct) and
        //   the target precision in percent. Try each distinct confidence from the lowest; return the first for which the
        //   items at or above it are right at least target percent of the time; return none when no level does. Example:
        //   target 90 and no level reaches it -> none.
        return 0;
    }

    static List<String> stratifiedSample(List<Item> items, int perStratum) {
        Set<String> strata = new LinkedHashSet<>();
        for (Item i : items) strata.add(i.stratum());
        List<String> chosen = new ArrayList<>();
        for (String stratum : strata) {
            List<Item> members = new ArrayList<>();
            for (Item i : items) if (i.stratum().equals(stratum)) members.add(i);
            members.sort(Comparator.comparingInt(Item::rank).thenComparing(Item::id));
            // TODO 4 of 7 (finish this to pass e5): the sample of one stratum. `members` are the stratum's items sorted
            //   by rank then id. Take only the first per_stratum of them and add their ids. Example: 5 items, per_stratum
            //   2 -> the 2 best ranked.
            for (Item m : members) chosen.add(m.id());
        }
        return chosen;
    }

    static Routing route(List<Extraction> extractions, int threshold, int capacity) {
        List<Extraction> candidates = new ArrayList<>();
        // TODO 5 of 7 (finish this to pass e6): the review queue. Keep the extractions that have a conflict or a
        //   confidence below the threshold, ordered with conflicts first, then by confidence (lowest first), then by id.
        //   Example: threshold 80, a conflict, a 60 and a 90 -> the conflict, then the 60.
        candidates.addAll(extractions);
        List<String> queue = new ArrayList<>();
        for (Extraction e : candidates) queue.add(e.id());
        Set<String> flagged = new HashSet<>(queue);
        List<String> auto = new ArrayList<>();
        for (Extraction e : extractions) if (!flagged.contains(e.id())) auto.add(e.id());
        // TODO 6 of 7 (finish this to pass e7): the capacity cut. Split the queue: the first `capacity` ids go to
        //   review, the others to the backlog, both in order. Example: queue [a, b, c], capacity 2 -> review [a, b],
        //   backlog [c].
        return new Routing(new ArrayList<>(queue), new ArrayList<>(), auto);
    }

    static String checkpoint(String action, int amount) {
        return checkpoint(action, amount, 1000);
    }

    static String checkpoint(String action, int amount, int limit) {
        // TODO 7 of 7 (finish this to pass e8): the checkpoint. Receives the action, the amount and the limit. Return
        //   human when the action is in IRREVERSIBLE or the amount is above the limit, otherwise auto. Example:
        //   close_account for 10 -> human.
        return "auto";
    }
}
