import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RhythmPlanTest {
    private static RhythmPlan.Choice choose(Map<String, Object> job) {
        RhythmPlan.Choice value = RhythmPlan.choose(new LinkedHashMap<>(job));
        assertNotNull(value, "choose returned nothing");
        return value;
    }

    private static boolean refused(Map<String, Object> job) {
        try {
            choose(job);
        } catch (IllegalArgumentException error) {
            return true;
        }
        return false;
    }

    private static RhythmPlan.Choice pick(String mechanism, String reason, int minutes) {
        return new RhythmPlan.Choice(mechanism, reason, minutes);
    }

    private static Map<String, Object> s(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) m.put((String) keyValues[i], keyValues[i + 1]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    private static Object[] axis(String key, Object... values) {
        Object[] out = new Object[values.length + 1];
        out[0] = key;
        System.arraycopy(values, 0, out, 1, values.length);
        return out;
    }

    /** Every combination of the axes, each added to the base job. */
    private static List<Map<String, Object>> sweep(Map<String, Object> base, Object[]... axes) {
        List<Map<String, Object>> combos = new ArrayList<>(List.of(base));
        for (Object[] axis : axes) {
            List<Map<String, Object>> next = new ArrayList<>();
            for (Map<String, Object> c : combos)
                for (int i = 1; i < axis.length; i++) {
                    Map<String, Object> m = new LinkedHashMap<>(c);
                    m.put((String) axis[0], axis[i]);
                    next.add(m);
                }
            combos = next;
        }
        return combos;
    }

    // The bank of the page: id, job, expected mechanism, reason code and interval in minutes.
    private static final Object[][] BANK = {
        {"r01", s(), "loop-self-paced", "pace-by-what-is-seen", 0},
        {"r02", s("interval_seconds", 300), "loop-fixed", "fixed-cadence", 5},
        {"r03", s("interval_seconds", 30), "loop-fixed", "fixed-cadence", 1},
        {"r04", s("trigger", "once"), "one-shot-task", "single-fire", 0},
        {"r05", s("trigger", "once", "machine_off", true), "routine", "survives-closed-machine", 0},
        {"r06", s("interval_seconds", 86400, "machine_off", true), "routine", "survives-closed-machine", 1440},
        {"r07", s("interval_seconds", 1800, "lasts_days", 30, "local_files", true), "desktop-task", "durable-and-local", 30},
        {"r08", s("interval_seconds", 7200, "lasts_days", 30), "routine", "outlives-seven-days", 120},
        {"r09", s("trigger", "event"), "monitor", "push-not-poll", 0},
        {"r10", s("trigger", "event", "repo_event", true), "routine", "react-to-event-unattended", 0},
        {"r11", s("trigger", "condition"), "goal", "until-condition-holds", 0},
        {"r12", s("trigger", "background"), "background-task", "work-while-it-runs", 0},
        {"r13", s("ci", true, "interval_seconds", 600), "headless-run", "no-person-present", 0},
        {"r14", s("interval_seconds", 900, "session_open", false, "local_files", true), "desktop-task", "durable-and-local", 15},
    };

    @Test
    void m1_everyJobOfTheBankGetsItsMechanismItsReasonAndItsInterval() {
        List<String> wrong = new ArrayList<>();
        for (Object[] row : BANK) if (!pick((String) row[2], (String) row[3], (int) row[4]).equals(choose(map(row[1])))) wrong.add((String) row[0]);
        assertEquals(List.of(), wrong, "these jobs got the wrong mechanism, reason or interval");
    }

    @Test
    void e1_aPipelineJobIsAHeadlessRunWhateverElseIsTrue() {
        for (Map<String, Object> j : sweep(s("ci", true), axis("trigger", "interval", "event", "condition", "once", "background"), axis("machine_off", false, true), axis("lasts_days", 1, 30), axis("repo_event", false, true), axis("session_open", true, false)))
            assertEquals(pick("headless-run", "no-person-present", 0), choose(j), j.toString());
        assertEquals(pick("headless-run", "no-person-present", 0), choose(s("ci", true, "interval_seconds", 600)), "{\"ci\": true, \"interval_seconds\": 600}");
        assertEquals(pick("headless-run", "no-person-present", 0), choose(s("ci", true, "interval_seconds", 90, "local_files", true)), "{\"ci\": true, \"interval_seconds\": 90, \"local_files\": true}");
    }

    @Test
    void e2_aConditionOrABackgroundCommandDecidesBeforeAnEventAndAnEventIsWatchedUnlessItMustRunUnattended() {
        for (Map<String, Object> j : sweep(s("trigger", "condition"), axis("machine_off", false, true), axis("repo_event", false, true), axis("interval_seconds", 0, 600), axis("lasts_days", 1, 30)))
            assertEquals(pick("goal", "until-condition-holds", 0), choose(j), j.toString());
        for (Map<String, Object> j : sweep(s("trigger", "background"), axis("machine_off", false, true), axis("repo_event", false, true), axis("interval_seconds", 0, 600), axis("lasts_days", 1, 30)))
            assertEquals(pick("background-task", "work-while-it-runs", 0), choose(j), j.toString());
        assertEquals(pick("monitor", "push-not-poll", 0), choose(s("trigger", "event")), "{\"trigger\": \"event\"}");
        assertEquals(pick("monitor", "push-not-poll", 0), choose(s("trigger", "event", "interval_seconds", 600)), "{\"trigger\": \"event\", \"interval_seconds\": 600}");
        assertEquals(pick("routine", "react-to-event-unattended", 0), choose(s("trigger", "event", "repo_event", true)), "{\"trigger\": \"event\", \"repo_event\": true}");
        assertEquals(pick("routine", "react-to-event-unattended", 0), choose(s("trigger", "event", "machine_off", true)), "{\"trigger\": \"event\", \"machine_off\": true}");
        assertEquals(pick("routine", "react-to-event-unattended", 0), choose(s("trigger", "event", "repo_event", true, "machine_off", true)), "{\"trigger\": \"event\", \"repo_event\": true, \"machine_off\": true}");
    }

    @Test
    void e3_aOneOffJobFiresInTheSessionUnlessTheMachineIsOffOrTheSessionIsClosed() {
        assertEquals(pick("one-shot-task", "single-fire", 0), choose(s("trigger", "once")), "{\"trigger\": \"once\"}");
        assertEquals(pick("one-shot-task", "single-fire", 0), choose(s("trigger", "once", "interval_seconds", 120, "lasts_days", 30)), "{\"trigger\": \"once\", \"interval_seconds\": 120, \"lasts_days\": 30}");
        assertEquals(pick("routine", "survives-closed-machine", 0), choose(s("trigger", "once", "machine_off", true)), "{\"trigger\": \"once\", \"machine_off\": true}");
        assertEquals(pick("desktop-task", "durable-and-local", 0), choose(s("trigger", "once", "session_open", false)), "{\"trigger\": \"once\", \"session_open\": false}");
        assertEquals(pick("routine", "survives-closed-machine", 0), choose(s("trigger", "once", "session_open", false, "machine_off", true)), "{\"trigger\": \"once\", \"session_open\": false, \"machine_off\": true}");
        assertEquals(pick("routine", "survives-closed-machine", 0), choose(s("trigger", "once", "machine_off", true, "lasts_days", 30)), "{\"trigger\": \"once\", \"machine_off\": true, \"lasts_days\": 30}");
    }

    @Test
    void e4_aCloudScheduleIsRefusedBelowOneHourAndADesktopScheduleBelowOneMinute() {
        assertTrue(refused(s("interval_seconds", 3599, "machine_off", true)), "{\"interval_seconds\": 3599, \"machine_off\": true} must be an error");
        assertTrue(refused(s("interval_seconds", 60, "machine_off", true)), "{\"interval_seconds\": 60, \"machine_off\": true} must be an error");
        assertTrue(refused(s("machine_off", true)), "{\"machine_off\": true} must be an error");
        assertTrue(refused(s("interval_seconds", 1800, "lasts_days", 30)), "{\"interval_seconds\": 1800, \"lasts_days\": 30} must be an error");
        assertTrue(refused(s("lasts_days", 30)), "{\"lasts_days\": 30} must be an error");
        assertTrue(refused(s("interval_seconds", 59, "lasts_days", 30, "local_files", true)), "{\"interval_seconds\": 59, \"lasts_days\": 30, \"local_files\": true} must be an error");
        assertTrue(refused(s("lasts_days", 30, "local_files", true)), "{\"lasts_days\": 30, \"local_files\": true} must be an error");
        assertTrue(refused(s("interval_seconds", 30, "session_open", false)), "{\"interval_seconds\": 30, \"session_open\": false} must be an error");
        assertTrue(refused(s("session_open", false)), "{\"session_open\": false} must be an error");
        assertEquals(pick("routine", "survives-closed-machine", 60), choose(s("interval_seconds", 3600, "machine_off", true)), "{\"interval_seconds\": 3600, \"machine_off\": true}");
        assertEquals(pick("routine", "survives-closed-machine", 90), choose(s("interval_seconds", 5400, "machine_off", true)), "{\"interval_seconds\": 5400, \"machine_off\": true}");
        assertEquals(pick("desktop-task", "durable-and-local", 1), choose(s("interval_seconds", 60, "lasts_days", 30, "local_files", true)), "{\"interval_seconds\": 60, \"lasts_days\": 30, \"local_files\": true}");
        assertEquals(pick("desktop-task", "durable-and-local", 2), choose(s("interval_seconds", 61, "session_open", false)), "{\"interval_seconds\": 61, \"session_open\": false}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), choose(s("interval_seconds", 30)), "{\"interval_seconds\": 30}");
    }

    @Test
    void e5_aFixedLoopRoundsSecondsUpToWholeMinutesAndNoIntervalMeansSelfPaced() {
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), choose(s("interval_seconds", 1)), "{\"interval_seconds\": 1}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), choose(s("interval_seconds", 59)), "{\"interval_seconds\": 59}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), choose(s("interval_seconds", 60)), "{\"interval_seconds\": 60}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 2), choose(s("interval_seconds", 61)), "{\"interval_seconds\": 61}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 5), choose(s("interval_seconds", 300)), "{\"interval_seconds\": 300}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 60), choose(s("interval_seconds", 3600)), "{\"interval_seconds\": 3600}");
        assertEquals(pick("loop-fixed", "fixed-cadence", 1440), choose(s("interval_seconds", 86400)), "{\"interval_seconds\": 86400}");
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), choose(s("interval_seconds", 0)), "{\"interval_seconds\": 0}");
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), choose(s("trigger", "interval", "lasts_days", 7)), "{\"trigger\": \"interval\", \"lasts_days\": 7}");
    }

    @Test
    void e6_aRecurringLoopLastsSevenDaysSoALongerJobNeedsADurableHome() {
        assertEquals(pick("loop-fixed", "fixed-cadence", 10), choose(s("interval_seconds", 600, "lasts_days", 7)), "{\"interval_seconds\": 600, \"lasts_days\": 7}");
        assertEquals(pick("desktop-task", "durable-and-local", 10), choose(s("interval_seconds", 600, "lasts_days", 8, "local_files", true)), "{\"interval_seconds\": 600, \"lasts_days\": 8, \"local_files\": true}");
        assertEquals(pick("routine", "outlives-seven-days", 60), choose(s("interval_seconds", 3600, "lasts_days", 8)), "{\"interval_seconds\": 3600, \"lasts_days\": 8}");
        assertEquals(pick("routine", "survives-closed-machine", 60), choose(s("interval_seconds", 3600, "lasts_days", 8, "machine_off", true)), "{\"interval_seconds\": 3600, \"lasts_days\": 8, \"machine_off\": true}");
        assertEquals(pick("routine", "outlives-seven-days", 60), choose(s("interval_seconds", 3600, "lasts_days", 365, "session_open", false)), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"session_open\": false}");
        assertEquals(pick("desktop-task", "durable-and-local", 60), choose(s("interval_seconds", 3600, "lasts_days", 365, "local_files", true, "session_open", false)), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"local_files\": true, \"session_open\": false}");
        assertTrue(refused(s("lasts_days", 8)), "{\"lasts_days\": 8} must be an error");
        assertTrue(refused(s("lasts_days", 8, "local_files", true)), "{\"lasts_days\": 8, \"local_files\": true} must be an error");
    }

    @Test
    void e7_anUnknownValueIsAnErrorACloudRunCannotSeeLocalFilesAndAMissingKeyTakesItsDefault() {
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), choose(s()), "{}");
        assertTrue(refused(s("trigger", "cron")), "{\"trigger\": \"cron\"} must be an error");
        assertTrue(refused(s("trigger", "")), "{\"trigger\": \"\"} must be an error");
        assertTrue(refused(s("interval_seconds", -1)), "{\"interval_seconds\": -1} must be an error");
        assertTrue(refused(s("lasts_days", 0)), "{\"lasts_days\": 0} must be an error");
        assertTrue(refused(s("machine_off", true, "local_files", true, "interval_seconds", 7200)), "{\"machine_off\": true, \"local_files\": true, \"interval_seconds\": 7200} must be an error");
        assertTrue(refused(s("trigger", "once", "machine_off", true, "local_files", true)), "{\"trigger\": \"once\", \"machine_off\": true, \"local_files\": true} must be an error");
        assertTrue(refused(s("ci", true, "trigger", "cron")), "{\"ci\": true, \"trigger\": \"cron\"} must be an error");
        assertTrue(refused(s("ci", true, "interval_seconds", -5)), "{\"ci\": true, \"interval_seconds\": -5} must be an error");
        assertTrue(refused(s("ci", true, "machine_off", true, "local_files", true)), "{\"ci\": true, \"machine_off\": true, \"local_files\": true} must be an error");
    }
}
