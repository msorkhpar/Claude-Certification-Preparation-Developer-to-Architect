import java.util.List;
import java.util.Map;
import java.util.Set;

/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */
final class LaunchReview {
    private LaunchReview() {}

    /** TODO: the findings "<severity> <domain> <rule>", high first, then by domain, then by rule id. */
    static List<String> launchReview(Set<String> f, Map<String, Integer> n) {
        return List.of();
    }

    /** TODO: reject for a high finding, revise for a medium one, otherwise approve. */
    static String verdict(List<String> findings) {
        return "";
    }

    /** TODO: the number of findings in each domain, P1 to P7. */
    static List<Integer> scorecard(List<String> findings) {
        return List.of();
    }

    /** TODO: 100 minus the review cost as a percent of the error cost, rounded up, never below 0; 0 when an error costs nothing. */
    static int neededAccuracy(int errorCost, int reviewCost) {
        return -1;
    }
}
