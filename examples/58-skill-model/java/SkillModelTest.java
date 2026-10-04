import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillModelTest {
    private static final Map<String, Object> META = Map.of("allowed-tools", "Bash(git tag *) Bash(git push origin *)", "disallowed-tools", "Edit Write(src/**)");

    @Test
    void aCommandAndASkillWithOneNameCreateOneSlashCommand() {
        assertEquals("deploy", SkillModel.commandName(".claude/commands/deploy.md", Map.of()));
        assertEquals("deploy", SkillModel.commandName(".claude/skills/deploy/SKILL.md", Map.of()));
        assertEquals("ship", SkillModel.commandName(".claude/skills/deploy/SKILL.md", Map.of("name", "ship")));
    }

    @Test
    void theHigherLevelWinsASharedName() {
        assertEquals("e", SkillModel.winner(Map.of("project", "p", "personal", "u", "enterprise", "e")));
        assertEquals("u", SkillModel.winner(Map.of("project", "p", "personal", "u")));
        assertEquals("p", SkillModel.winner(Map.of("project", "p")));
        assertNull(SkillModel.winner(Map.of()));
    }

    private static Map<String, Boolean> who(boolean you, boolean claude, boolean description) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("you", you);
        m.put("claude", claude);
        m.put("description_in_context", description);
        return m;
    }

    @Test
    void whoCanStartASkill() {
        assertEquals(who(true, true, true), SkillModel.invocation(Map.of()));
        assertEquals(who(true, false, false), SkillModel.invocation(Map.of("disable-model-invocation", true)));
        assertEquals(who(false, true, true), SkillModel.invocation(Map.of("user-invocable", false)));
    }

    @Test
    void allowedToolsPreApprovesPatternsAndABareNameApprovesEverything() {
        assertTrue(SkillModel.preApproved(META, "Bash", "git tag v1"));
        assertTrue(SkillModel.preApproved(META, "Bash", "git push origin v1"));
        assertFalse(SkillModel.preApproved(META, "Bash", "git push --force"));
        assertFalse(SkillModel.preApproved(META, "Bash", "rm -rf build"));
        assertTrue(SkillModel.preApproved(Map.of("allowed-tools", "Bash"), "Bash", "rm -rf build"));
        assertTrue(SkillModel.preApproved(Map.of("allowed-tools", "Read, Grep"), "Grep"));
    }

    @Test
    void onlyABareDisallowedNameRemovesATool() {
        assertTrue(SkillModel.removed(META, "Edit"));
        assertFalse(SkillModel.removed(META, "Write"));
        assertFalse(SkillModel.removed(Map.of("disallowed-tools", "Edit(src/**)"), "Edit"));
        assertEquals("removed", SkillModel.toolStatus(META, "Edit"));
        assertEquals("pre-approved", SkillModel.toolStatus(META, "Bash", "git tag v1"));
        assertEquals("permission settings decide", SkillModel.toolStatus(META, "Read"));
    }

    @Test
    void argumentsFillPlaceholdersInShellStyle() {
        assertEquals("pr 123 by ana", SkillModel.render("pr $0 by $1", "123 ana"));
        assertEquals("first=hello world all=\"hello world\" second", SkillModel.render("first=$ARGUMENTS[0] all=$ARGUMENTS", "\"hello world\" second"));
        assertEquals("tag v2 on main", SkillModel.render("tag $version on $branch", "v2 main", List.of("version", "branch")));
        assertEquals("only $1", SkillModel.render("only $1", "a"));
        assertEquals("tag v2 on ", SkillModel.render("tag $version on $branch", "v2", List.of("version", "branch")));
    }

    @Test
    void inputThatNoPlaceholderReceivesIsAppended() {
        assertEquals("Review the change.\nARGUMENTS: 123\n", SkillModel.render("Review the change.\n", "123"));
        assertEquals("Review the change.\n", SkillModel.render("Review the change.\n", ""));
        assertEquals("Tag v1\n", SkillModel.render("Tag $version\n", "v1", List.of("version")));
    }

    @Test
    void aForkedSkillRunsInASubagentThatDefaultsToGeneralPurpose() {
        assertEquals("general-purpose", SkillModel.forkAgent(Map.of("context", "fork")));
        assertEquals("Explore", SkillModel.forkAgent(Map.of("context", "fork", "agent", "Explore")));
        assertNull(SkillModel.forkAgent(Map.of()));
    }

    @Test
    void theFrontmatterIsSplitFromTheBody() throws IOException {
        SkillModel.Parsed p = SkillModel.parse("---\nname: x\narguments: [a, b]\n---\nHello $a\n");
        assertEquals(Map.of("name", "x", "arguments", List.of("a", "b")), p.meta());
        assertEquals("Hello $a\n", p.body());
    }
}
