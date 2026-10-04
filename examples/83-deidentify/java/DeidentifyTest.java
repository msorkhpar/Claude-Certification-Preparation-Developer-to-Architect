import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeidentifyTest {
    @Test
    void theSameValueGetsTheSameTokenAndEachKindIsCountedOnItsOwn() {
        Map<String, String> vault = new LinkedHashMap<>();
        assertEquals("<EMAIL_1> <EMAIL_2> <EMAIL_1> <MEMBER_1>", Deidentify.tokenise("a@x.io b@x.io a@x.io M-123456", vault));
        assertEquals(3, vault.size());
    }

    @Test
    void restorePutsEveryValueBack() {
        Map<String, String> vault = new LinkedHashMap<>();
        String original = "write to a@x.io about M-123456";
        assertEquals(original, Deidentify.restore(Deidentify.tokenise(original, vault), vault));
    }

    @Test
    void textWithoutIdentifiersIsUnchangedAndTheVaultStaysEmpty() {
        Map<String, String> vault = new LinkedHashMap<>();
        assertEquals("nothing to find", Deidentify.tokenise("nothing to find", vault));
        assertTrue(vault.isEmpty());
    }

    @Test
    void aNameIsNotFoundByThesePatterns() {
        assertEquals("Jane Doe", Deidentify.tokenise("Jane Doe", new LinkedHashMap<>()));
    }

    @Test
    void theAuditEntryHoldsSizesAndCountsOnly() {
        Map<String, String> vault = new LinkedHashMap<>();
        Deidentify.tokenise("a@x.io", vault);
        Deidentify.Audit entry = Deidentify.auditEntry("r1", "a@x.io", vault);
        assertEquals(new Deidentify.Audit("r1", 6, 1, false), entry);
        assertFalse(entry.toString().contains("a@x.io"));
    }
}
