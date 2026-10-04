import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EscalationTest {
    private fun action(c: Case, maxAttempts: Int = 2) = decide(c, maxAttempts)!!.action

    private fun rec(vararg kv: Pair<String, String>): Map<String, String> = linkedMapOf(*kv)

    @Test
    fun m1_aCustomerWhoAsksForAPersonIsEscalatedAtOnceEvenWhenTheAgentCouldResolveIt() {
        assertEquals(Decision("escalate", "customer asked for a person", false), decide(Case(askedForPerson = true, confidence = 95)))
    }

    @Test
    fun e1_frustrationAloneDoesNotEscalateAndTheReplyAcknowledgesIt() {
        assertEquals(Decision("resolve", "within capability", true), decide(Case(sentiment = "frustrated")))
        assertFalse(decide(Case())!!.acknowledge)
    }

    @Test
    fun e2_aRequestThePolicyDoesNotCoverIsEscalatedAndACoveredOneIsResolved() {
        assertEquals(Decision("escalate", "policy does not cover the request", false), decide(Case(policyCovers = false)))
        assertEquals("resolve", action(Case(policyCovers = true)))
    }

    @Test
    fun e3_severalMatchingRecordsNeedAClarifyingQuestionAndNeverAGuess() {
        assertEquals(Decision("clarify", "ambiguous customer match", false), decide(Case(matches = 3)))
        assertEquals("resolve", action(Case(matches = 1)))
        assertEquals("resolve", action(Case(matches = 0)))
    }

    @Test
    fun e4_anExplicitRequestForAPersonOutranksAnAmbiguousMatch() {
        assertEquals("escalate", action(Case(askedForPerson = true, matches = 4)))
        assertEquals("escalate", action(Case(askedForPerson = true, policyCovers = false)))
    }

    @Test
    fun e5_noProgressAfterTheAttemptLimitEscalatesAndBelowItDoesNot() {
        assertEquals(Decision("escalate", "no progress", false), decide(Case(attemptsWithoutProgress = 2)))
        assertEquals("resolve", action(Case(attemptsWithoutProgress = 1)))
        assertEquals("resolve", action(Case(attemptsWithoutProgress = 3), 4))
    }

    @Test
    fun e6_sentimentAndConfidenceScoresNeverChangeTheDecision() {
        for (sentiment in listOf("calm", "frustrated", "angry")) for (confidence in listOf(5, 50, 99)) assertEquals("resolve", action(Case(sentiment = sentiment, confidence = confidence)))
        assertEquals("clarify", action(Case(sentiment = "angry", confidence = 1, matches = 2)))
    }

    @Test
    fun e7_theClarifyingQuestionNamesOnlyTheFieldsThatTellTheMatchesApart() {
        assertEquals(listOf("email"), clarifyingFields(listOf(rec("id" to "c1", "name" to "Ana Ruiz", "email" to "ana@example.com", "zip" to "10115"), rec("id" to "c2", "name" to "Ana Ruiz", "email" to "ana.r@example.com", "zip" to "10115"))))
        assertEquals(listOf("name", "zip"), clarifyingFields(listOf(rec("id" to "c1", "name" to "Ana", "zip" to "1"), rec("id" to "c2", "name" to "Bo", "zip" to "2"), rec("id" to "c3", "name" to "Ana", "zip" to "3"))))
        assertEquals(emptyList<String>(), clarifyingFields(listOf(rec("id" to "c1", "name" to "Ana"))))
    }

    @Test
    fun e8_theHandOffCarriesTheStructuredFactsAndNoTranscriptAndRefusesACaseWithoutAnId() {
        val c = HandoffCase("C-77", "refund over the limit", "duplicate charge", "$129.50", listOf("verified identity", "checked order"), "approve the refund", "user: hello ... 40 turns ...")
        assertEquals("Customer: C-77\nIssue: refund over the limit\nRoot cause: duplicate charge\nAmount: $129.50\nActions taken: verified identity; checked order\nRecommended action: approve the refund", handoffText(c))
        assertFalse("40 turns" in handoffText(c)!!)
        assertEquals("Customer: C-1\nIssue: late parcel\nRoot cause: unknown\nAmount: unknown\nActions taken: none\nRecommended action: review the case", handoffText(HandoffCase("C-1", "late parcel")))
        assertThrows(IllegalArgumentException::class.java) { handoffText(HandoffCase(null, "late parcel")) }
    }
}
