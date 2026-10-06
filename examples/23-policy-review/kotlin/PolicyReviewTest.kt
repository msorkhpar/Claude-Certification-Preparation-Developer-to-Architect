import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PolicyReviewTest {
    @Test
    fun theBroadPolicyHasAWildcardActionAndAWildcardResource() {
        assertEquals(listOf("action", "resource"), reviewPolicy(parse(BROAD)).map { it.split(": ")[1].split(" ")[0] })
    }

    @Test
    fun theNarrowPolicyHasNoFindings() {
        assertEquals(emptyList<String>(), reviewPolicy(parse(NARROW)))
    }

    @Test
    fun aPredefinedRoleWithDeployIsFlaggedTwiceAndTheCustomRolePasses() {
        assertEquals(2, reviewRole(parse(ROLES.getValue("predefined"))).size)
        assertEquals(emptyList<String>(), reviewRole(parse(ROLES.getValue("custom"))))
    }
}
