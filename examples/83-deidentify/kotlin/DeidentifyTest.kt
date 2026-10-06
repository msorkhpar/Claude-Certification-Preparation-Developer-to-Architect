import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeidentifyTest {
    @Test
    fun theSameValueGetsTheSameTokenAndEachKindIsCountedOnItsOwn() {
        val vault = linkedMapOf<String, String>()
        assertEquals("<EMAIL_1> <EMAIL_2> <EMAIL_1> <MEMBER_1>", tokenise("a@x.io b@x.io a@x.io M-123456", vault))
        assertEquals(3, vault.size)
    }

    @Test
    fun restorePutsEveryValueBack() {
        val vault = linkedMapOf<String, String>()
        val original = "write to a@x.io about M-123456"
        assertEquals(original, restore(tokenise(original, vault), vault))
    }

    @Test
    fun textWithoutIdentifiersIsUnchangedAndTheVaultStaysEmpty() {
        val vault = linkedMapOf<String, String>()
        assertEquals("nothing to find", tokenise("nothing to find", vault))
        assertTrue(vault.isEmpty())
    }

    @Test
    fun aNameIsNotFoundByThesePatterns() {
        assertEquals("Jane Doe", tokenise("Jane Doe", linkedMapOf()))
    }

    @Test
    fun theAuditEntryHoldsSizesAndCountsOnly() {
        val vault = linkedMapOf<String, String>()
        tokenise("a@x.io", vault)
        val entry = auditEntry("r1", "a@x.io", vault)
        assertEquals(Audit("r1", 6, 1, false), entry)
        assertFalse(entry.toString().contains("a@x.io"))
    }
}
