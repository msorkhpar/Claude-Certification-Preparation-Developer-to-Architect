import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyReviewTest {
    @Test
    void theBroadPolicyHasAWildcardActionAndAWildcardResource() {
        List<String> kinds = PolicyReview.reviewPolicy(PolicyReview.parse(PolicyReview.BROAD)).stream().map(f -> f.split(": ")[1].split(" ")[0]).toList();
        assertEquals(List.of("action", "resource"), kinds);
    }

    @Test
    void theNarrowPolicyHasNoFindings() {
        assertEquals(List.of(), PolicyReview.reviewPolicy(PolicyReview.parse(PolicyReview.NARROW)));
    }

    @Test
    void aPredefinedRoleWithDeployIsFlaggedTwiceAndTheCustomRolePasses() {
        assertEquals(2, PolicyReview.reviewRole(PolicyReview.parse(PolicyReview.ROLES.get("predefined"))).size());
        assertEquals(List.of(), PolicyReview.reviewRole(PolicyReview.parse(PolicyReview.ROLES.get("custom"))));
    }
}
