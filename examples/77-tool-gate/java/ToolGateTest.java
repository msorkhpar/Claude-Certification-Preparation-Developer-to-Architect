import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolGateTest {
    @Test
    void aDeniedPermissionRefusesTheToolWhateverElseItAsksFor() {
        assertEquals(new ToolGate.Decision("refused", List.of("network")), ToolGate.decide(List.of("read_files", "network")));
        assertEquals(new ToolGate.Decision("refused", List.of("run_process", "network")), ToolGate.decide(List.of("run_process", "network")));
    }

    @Test
    void aWriteWaitsForAPersonAndAReadRunsByItself() {
        assertEquals(new ToolGate.Decision("needs_approval", List.of("write_files")), ToolGate.decide(List.of("read_files", "write_files")));
        assertEquals(new ToolGate.Decision("auto", List.of()), ToolGate.decide(List.of("read_files")));
    }

    @Test
    void aResultIsCheckedForFieldsTypesAndSizeBeforeTheAgentUsesIt() {
        assertEquals(List.of(), ToolGate.checkOutput(Map.of("headline", "ok", "rows", 3)));
        assertEquals(List.of("missing: rows"), ToolGate.checkOutput(Map.of("headline", "ok")));
        assertEquals(List.of("type: rows"), ToolGate.checkOutput(Map.of("headline", "ok", "rows", "3")));
        assertEquals(List.of("too large"), ToolGate.checkOutput(Map.of("headline", "x".repeat(201), "rows", 3)));
        assertEquals(List.of(), ToolGate.checkOutput(Map.of("headline", "x".repeat(200), "rows", 3)));
    }
}
