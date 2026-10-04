import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import harness.Scripted;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoopsTest {
    @Test
    void theStopReasonLoopRunsTheToolOfAReplyThatSaysDone() {
        Loops.Outcome o = Loops.byStopReason(Scripted.client(Loops.scenarioA()).client(), "t");
        assertEquals(new Loops.Outcome("done", 2, List.of("save_file"), "Saved report.txt."), o);
    }

    @Test
    void theTextMarkerLoopStopsBeforeTheToolRuns() {
        Loops.Outcome o = Loops.byTextMarker(Scripted.client(Loops.scenarioA()).client(), "t");
        assertEquals("done", o.status());
        assertEquals(1, o.calls());
        assertEquals(List.of(), o.ran());
    }

    @Test
    void aBackstopBelowTheRealNeedGetsItsOwnStatus() {
        Loops.Outcome o = Loops.byStopReason(Scripted.client(Loops.scenarioB()).client(), "t", 3);
        assertEquals("max_turns", o.status());
        assertEquals(3, o.calls());
        assertEquals(3, o.ran().size());
    }

    @Test
    void aBackstopAboveTheNeedIsNeverReached() {
        Loops.Outcome o = Loops.byStopReason(Scripted.client(Loops.scenarioB()).client(), "t", 10);
        assertEquals("done", o.status());
        assertEquals(5, o.calls());
    }

    @Test
    void theFixedCountLoopReportsDoneWithoutAFinalAnswer() {
        Loops.Outcome o = Loops.byFixedCount(Scripted.client(Loops.scenarioB()).client(), "t");
        assertEquals("done", o.status());
        assertEquals(3, o.ran().size());
        assertEquals("", o.answer());
    }

    @Test
    void anEndTurnThatAnnouncesAToolCallIsStillTheEnd() {
        Scripted.Rig rig = Scripted.client(message(List.of(text("Let me call the lookup tool next."))));
        Loops.Outcome o = Loops.byStopReason(rig.client(), "t");
        assertEquals("done", o.status());
        assertEquals(1, o.calls());
        assertEquals(1, rig.http().requests.size());
    }
}
