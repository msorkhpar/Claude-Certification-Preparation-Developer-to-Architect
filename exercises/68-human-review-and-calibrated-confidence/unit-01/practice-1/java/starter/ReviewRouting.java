import java.util.List;

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

    static List<Seg> accuracyBy(List<Rec> records) {
        // TODO: the overall accuracy and the accuracy of every docType/field segment, the overall one first and then the segments in alphabetical order.
        return null;
    }

    static Automation canAutomate(List<Rec> records, int threshold, int minN) {
        // TODO: every segment must have enough records and reach the threshold.
        return null;
    }

    static Integer calibrateThreshold(List<Labeled> labeled, int target) {
        // TODO: the lowest confidence whose auto-accepted items (confidence at or above it) reach the target precision, or null.
        return null;
    }

    static List<String> stratifiedSample(List<Item> items, int perStratum) {
        // TODO: the ids of the lowest-ranked items of every stratum.
        return null;
    }

    static Routing route(List<Extraction> extractions, int threshold, int capacity) {
        // TODO: low confidence and conflicts go to review, the weakest first, within the capacity.
        return null;
    }

    static String checkpoint(String action, int amount) {
        return checkpoint(action, amount, 1000);
    }

    static String checkpoint(String action, int amount, int limit) {
        // TODO: "human" or "auto" for an action.
        return null;
    }
}
