import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class BuiltinToolsTest {
    private static final String TEXT = "a = 1\nb = 1\n";

    @Test
    void editIsAnExactReplacementThatNeedsOneMatch() {
        assertEquals(new BuiltinTools.EditResult(true, "a = 2\nb = 1\n", 1, null), BuiltinTools.edit(TEXT, "a = 1", "a = 2"));
        assertEquals(new BuiltinTools.EditResult(false, null, null, "old_string appears 2 times"), BuiltinTools.edit(TEXT, "= 1", "= 2"));
        assertEquals(new BuiltinTools.EditResult(false, null, null, "old_string not found"), BuiltinTools.edit(TEXT, "z", "y"));
        assertFalse(BuiltinTools.edit(TEXT, "a = .", "x").ok()); // no regex
    }

    @Test
    void replaceAllChangesEveryMatch() {
        assertEquals(new BuiltinTools.EditResult(true, "a = 2\nb = 2\n", 2, null), BuiltinTools.edit(TEXT, "= 1", "= 2", true));
    }

    @Test
    void thePlanWidensTheAnchorBeforeItRewritesTheFile() {
        assertEquals(new BuiltinTools.Plan("edit", "a = 1"), BuiltinTools.planEdit(TEXT, "a = 1"));
        assertEquals(new BuiltinTools.Plan("edit", "b = 1"), BuiltinTools.planEdit(TEXT, "= 1", false, List.of("b = 1")));
        assertEquals(new BuiltinTools.Plan("replace_all", "= 1"), BuiltinTools.planEdit(TEXT, "= 1", true, List.of()));
        assertEquals(new BuiltinTools.Plan("read_write", null), BuiltinTools.planEdit(TEXT, "= 1", false, List.of("= 1")));
        assertEquals(new BuiltinTools.Plan("read_again", null), BuiltinTools.planEdit(TEXT, "z"));
    }

    @Test
    void searchToolsAreDefaultOnWindowsOnlyAndNamedBackElsewhere() {
        assertEquals(List.of("Read", "Write", "Edit", "Bash", "Grep", "Glob"), BuiltinTools.toolSet("windows"));
        assertEquals(List.of("Read", "Write", "Edit", "Bash"), BuiltinTools.toolSet("linux"));
        assertEquals(List.of("Read", "Write", "Edit", "Bash", "Grep", "Glob"), BuiltinTools.toolSet("linux", null, List.of("Glob"), List.of()));
        assertEquals(List.of("Read", "Grep"), BuiltinTools.toolSet("macos", List.of("Read", "Grep"), List.of(), List.of()));
        assertEquals(List.of("Read", "Write", "Edit", "Grep", "Glob"), BuiltinTools.toolSet("wsl", null, List.of(), List.of("Bash")));
    }

    @Test
    void aRuleCoversTheToolsItsNameImplies() {
        assertEquals(List.of("Read", "Grep", "Glob"), BuiltinTools.coveredBy("Read(x)"));
        assertEquals(List.of("Edit", "Write"), BuiltinTools.coveredBy("Edit(x)"));
        assertEquals(List.of(), BuiltinTools.coveredBy("Write(x)"));
        assertEquals(List.of("Bash"), BuiltinTools.coveredBy("Bash(ls *)"));
    }

    @Test
    void aCallIsCheckedUnderTheRuleNameOfItsFamily() {
        assertEquals(List.of("Read", "Read", "Read", "Edit", "Edit", "Bash"), List.of("Read", "Grep", "Glob", "Edit", "Write", "Bash").stream().map(BuiltinTools::ruleTool).toList());
    }
}
