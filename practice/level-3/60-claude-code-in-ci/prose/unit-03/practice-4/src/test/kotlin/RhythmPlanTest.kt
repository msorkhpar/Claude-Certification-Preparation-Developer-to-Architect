import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RhythmPlanTest {
    private fun decide(job: Map<String, Any>): Choice {
        val value: Choice? = choose(job.toMap(LinkedHashMap()))
        assertNotNull(value, "choose returned nothing")
        return value!!
    }

    private fun refused(job: Map<String, Any>): Boolean = try {
        decide(job)
        false
    } catch (error: IllegalArgumentException) {
        true
    }

    /** Every combination of the axes, each added to the base job. */
    private fun sweep(base: Map<String, Any>, vararg axes: Pair<String, List<Any>>): List<Map<String, Any>> {
        var combos: List<Map<String, Any>> = listOf(base)
        for ((key, values) in axes) combos = combos.flatMap { c -> values.map { v -> c + (key to v) } }
        return combos
    }

    private fun pick(mechanism: String, reason: String, minutes: Int = 0) = Choice(mechanism, reason, minutes)

    // The bank of the page: id, job, expected mechanism, reason code and interval in minutes.
    private data class Row(val id: String, val job: Map<String, Any>, val mechanism: String, val reason: String, val minutes: Int)

    private val BANK = listOf(
        Row("r01", emptyMap(), "loop-self-paced", "pace-by-what-is-seen", 0),
        Row("r02", mapOf("interval_seconds" to 300), "loop-fixed", "fixed-cadence", 5),
        Row("r03", mapOf("interval_seconds" to 30), "loop-fixed", "fixed-cadence", 1),
        Row("r04", mapOf("trigger" to "once"), "one-shot-task", "single-fire", 0),
        Row("r05", mapOf("trigger" to "once", "machine_off" to true), "routine", "survives-closed-machine", 0),
        Row("r06", mapOf("interval_seconds" to 86400, "machine_off" to true), "routine", "survives-closed-machine", 1440),
        Row("r07", mapOf("interval_seconds" to 1800, "lasts_days" to 30, "local_files" to true), "desktop-task", "durable-and-local", 30),
        Row("r08", mapOf("interval_seconds" to 7200, "lasts_days" to 30), "routine", "outlives-seven-days", 120),
        Row("r09", mapOf("trigger" to "event"), "monitor", "push-not-poll", 0),
        Row("r10", mapOf("trigger" to "event", "repo_event" to true), "routine", "react-to-event-unattended", 0),
        Row("r11", mapOf("trigger" to "condition"), "goal", "until-condition-holds", 0),
        Row("r12", mapOf("trigger" to "background"), "background-task", "work-while-it-runs", 0),
        Row("r13", mapOf("ci" to true, "interval_seconds" to 600), "headless-run", "no-person-present", 0),
        Row("r14", mapOf("interval_seconds" to 900, "session_open" to false, "local_files" to true), "desktop-task", "durable-and-local", 15)
    )

    @Test
    fun m1_everyJobOfTheBankGetsItsMechanismItsReasonAndItsInterval() {
        val wrong = BANK.filter { decide(it.job) != pick(it.mechanism, it.reason, it.minutes) }.map { it.id }
        assertEquals(emptyList<String>(), wrong, "these jobs got the wrong mechanism, reason or interval")
    }

    @Test
    fun e1_aPipelineJobIsAHeadlessRunWhateverElseIsTrue() {
        for (j in sweep(mapOf("ci" to true), "trigger" to listOf("interval", "event", "condition", "once", "background"), "machine_off" to listOf(false, true), "lasts_days" to listOf(1, 30), "repo_event" to listOf(false, true), "session_open" to listOf(true, false)))
            assertEquals(pick("headless-run", "no-person-present", 0), decide(j), j.toString())
        assertEquals(pick("headless-run", "no-person-present", 0), decide(mapOf("ci" to true, "interval_seconds" to 600)), "{\"ci\": true, \"interval_seconds\": 600}")
        assertEquals(pick("headless-run", "no-person-present", 0), decide(mapOf("ci" to true, "interval_seconds" to 90, "local_files" to true)), "{\"ci\": true, \"interval_seconds\": 90, \"local_files\": true}")
    }

    @Test
    fun e2_aConditionOrABackgroundCommandDecidesBeforeAnEventAndAnEventIsWatchedUnlessItMustRunUnattended() {
        for (j in sweep(mapOf("trigger" to "condition"), "machine_off" to listOf(false, true), "repo_event" to listOf(false, true), "interval_seconds" to listOf(0, 600), "lasts_days" to listOf(1, 30)))
            assertEquals(pick("goal", "until-condition-holds", 0), decide(j), j.toString())
        for (j in sweep(mapOf("trigger" to "background"), "machine_off" to listOf(false, true), "repo_event" to listOf(false, true), "interval_seconds" to listOf(0, 600), "lasts_days" to listOf(1, 30)))
            assertEquals(pick("background-task", "work-while-it-runs", 0), decide(j), j.toString())
        assertEquals(pick("monitor", "push-not-poll", 0), decide(mapOf("trigger" to "event")), "{\"trigger\": \"event\"}")
        assertEquals(pick("monitor", "push-not-poll", 0), decide(mapOf("trigger" to "event", "interval_seconds" to 600)), "{\"trigger\": \"event\", \"interval_seconds\": 600}")
        assertEquals(pick("routine", "react-to-event-unattended", 0), decide(mapOf("trigger" to "event", "repo_event" to true)), "{\"trigger\": \"event\", \"repo_event\": true}")
        assertEquals(pick("routine", "react-to-event-unattended", 0), decide(mapOf("trigger" to "event", "machine_off" to true)), "{\"trigger\": \"event\", \"machine_off\": true}")
        assertEquals(pick("routine", "react-to-event-unattended", 0), decide(mapOf("trigger" to "event", "repo_event" to true, "machine_off" to true)), "{\"trigger\": \"event\", \"repo_event\": true, \"machine_off\": true}")
    }

    @Test
    fun e3_aOneOffJobFiresInTheSessionUnlessTheMachineIsOffOrTheSessionIsClosed() {
        assertEquals(pick("one-shot-task", "single-fire", 0), decide(mapOf("trigger" to "once")), "{\"trigger\": \"once\"}")
        assertEquals(pick("one-shot-task", "single-fire", 0), decide(mapOf("trigger" to "once", "interval_seconds" to 120, "lasts_days" to 30)), "{\"trigger\": \"once\", \"interval_seconds\": 120, \"lasts_days\": 30}")
        assertEquals(pick("routine", "survives-closed-machine", 0), decide(mapOf("trigger" to "once", "machine_off" to true)), "{\"trigger\": \"once\", \"machine_off\": true}")
        assertEquals(pick("desktop-task", "durable-and-local", 0), decide(mapOf("trigger" to "once", "session_open" to false)), "{\"trigger\": \"once\", \"session_open\": false}")
        assertEquals(pick("routine", "survives-closed-machine", 0), decide(mapOf("trigger" to "once", "session_open" to false, "machine_off" to true)), "{\"trigger\": \"once\", \"session_open\": false, \"machine_off\": true}")
        assertEquals(pick("routine", "survives-closed-machine", 0), decide(mapOf("trigger" to "once", "machine_off" to true, "lasts_days" to 30)), "{\"trigger\": \"once\", \"machine_off\": true, \"lasts_days\": 30}")
    }

    @Test
    fun e4_aCloudScheduleIsRefusedBelowOneHourAndADesktopScheduleBelowOneMinute() {
        assertTrue(refused(mapOf("interval_seconds" to 3599, "machine_off" to true)), "{\"interval_seconds\": 3599, \"machine_off\": true} must be an error")
        assertTrue(refused(mapOf("interval_seconds" to 60, "machine_off" to true)), "{\"interval_seconds\": 60, \"machine_off\": true} must be an error")
        assertTrue(refused(mapOf("machine_off" to true)), "{\"machine_off\": true} must be an error")
        assertTrue(refused(mapOf("interval_seconds" to 1800, "lasts_days" to 30)), "{\"interval_seconds\": 1800, \"lasts_days\": 30} must be an error")
        assertTrue(refused(mapOf("lasts_days" to 30)), "{\"lasts_days\": 30} must be an error")
        assertTrue(refused(mapOf("interval_seconds" to 59, "lasts_days" to 30, "local_files" to true)), "{\"interval_seconds\": 59, \"lasts_days\": 30, \"local_files\": true} must be an error")
        assertTrue(refused(mapOf("lasts_days" to 30, "local_files" to true)), "{\"lasts_days\": 30, \"local_files\": true} must be an error")
        assertTrue(refused(mapOf("interval_seconds" to 30, "session_open" to false)), "{\"interval_seconds\": 30, \"session_open\": false} must be an error")
        assertTrue(refused(mapOf("session_open" to false)), "{\"session_open\": false} must be an error")
        assertEquals(pick("routine", "survives-closed-machine", 60), decide(mapOf("interval_seconds" to 3600, "machine_off" to true)), "{\"interval_seconds\": 3600, \"machine_off\": true}")
        assertEquals(pick("routine", "survives-closed-machine", 90), decide(mapOf("interval_seconds" to 5400, "machine_off" to true)), "{\"interval_seconds\": 5400, \"machine_off\": true}")
        assertEquals(pick("desktop-task", "durable-and-local", 1), decide(mapOf("interval_seconds" to 60, "lasts_days" to 30, "local_files" to true)), "{\"interval_seconds\": 60, \"lasts_days\": 30, \"local_files\": true}")
        assertEquals(pick("desktop-task", "durable-and-local", 2), decide(mapOf("interval_seconds" to 61, "session_open" to false)), "{\"interval_seconds\": 61, \"session_open\": false}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), decide(mapOf("interval_seconds" to 30)), "{\"interval_seconds\": 30}")
    }

    @Test
    fun e5_aFixedLoopRoundsSecondsUpToWholeMinutesAndNoIntervalMeansSelfPaced() {
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), decide(mapOf("interval_seconds" to 1)), "{\"interval_seconds\": 1}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), decide(mapOf("interval_seconds" to 59)), "{\"interval_seconds\": 59}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 1), decide(mapOf("interval_seconds" to 60)), "{\"interval_seconds\": 60}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 2), decide(mapOf("interval_seconds" to 61)), "{\"interval_seconds\": 61}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 5), decide(mapOf("interval_seconds" to 300)), "{\"interval_seconds\": 300}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 60), decide(mapOf("interval_seconds" to 3600)), "{\"interval_seconds\": 3600}")
        assertEquals(pick("loop-fixed", "fixed-cadence", 1440), decide(mapOf("interval_seconds" to 86400)), "{\"interval_seconds\": 86400}")
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), decide(mapOf("interval_seconds" to 0)), "{\"interval_seconds\": 0}")
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), decide(mapOf("trigger" to "interval", "lasts_days" to 7)), "{\"trigger\": \"interval\", \"lasts_days\": 7}")
    }

    @Test
    fun e6_aRecurringLoopLastsSevenDaysSoALongerJobNeedsADurableHome() {
        assertEquals(pick("loop-fixed", "fixed-cadence", 10), decide(mapOf("interval_seconds" to 600, "lasts_days" to 7)), "{\"interval_seconds\": 600, \"lasts_days\": 7}")
        assertEquals(pick("desktop-task", "durable-and-local", 10), decide(mapOf("interval_seconds" to 600, "lasts_days" to 8, "local_files" to true)), "{\"interval_seconds\": 600, \"lasts_days\": 8, \"local_files\": true}")
        assertEquals(pick("routine", "outlives-seven-days", 60), decide(mapOf("interval_seconds" to 3600, "lasts_days" to 8)), "{\"interval_seconds\": 3600, \"lasts_days\": 8}")
        assertEquals(pick("routine", "survives-closed-machine", 60), decide(mapOf("interval_seconds" to 3600, "lasts_days" to 8, "machine_off" to true)), "{\"interval_seconds\": 3600, \"lasts_days\": 8, \"machine_off\": true}")
        assertEquals(pick("routine", "outlives-seven-days", 60), decide(mapOf("interval_seconds" to 3600, "lasts_days" to 365, "session_open" to false)), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"session_open\": false}")
        assertEquals(pick("desktop-task", "durable-and-local", 60), decide(mapOf("interval_seconds" to 3600, "lasts_days" to 365, "local_files" to true, "session_open" to false)), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"local_files\": true, \"session_open\": false}")
        assertTrue(refused(mapOf("lasts_days" to 8)), "{\"lasts_days\": 8} must be an error")
        assertTrue(refused(mapOf("lasts_days" to 8, "local_files" to true)), "{\"lasts_days\": 8, \"local_files\": true} must be an error")
    }

    @Test
    fun e7_anUnknownValueIsAnErrorACloudRunCannotSeeLocalFilesAndAMissingKeyTakesItsDefault() {
        assertEquals(pick("loop-self-paced", "pace-by-what-is-seen", 0), decide(emptyMap()), "{}")
        assertTrue(refused(mapOf("trigger" to "cron")), "{\"trigger\": \"cron\"} must be an error")
        assertTrue(refused(mapOf("trigger" to "")), "{\"trigger\": \"\"} must be an error")
        assertTrue(refused(mapOf("interval_seconds" to -1)), "{\"interval_seconds\": -1} must be an error")
        assertTrue(refused(mapOf("lasts_days" to 0)), "{\"lasts_days\": 0} must be an error")
        assertTrue(refused(mapOf("machine_off" to true, "local_files" to true, "interval_seconds" to 7200)), "{\"machine_off\": true, \"local_files\": true, \"interval_seconds\": 7200} must be an error")
        assertTrue(refused(mapOf("trigger" to "once", "machine_off" to true, "local_files" to true)), "{\"trigger\": \"once\", \"machine_off\": true, \"local_files\": true} must be an error")
        assertTrue(refused(mapOf("ci" to true, "trigger" to "cron")), "{\"ci\": true, \"trigger\": \"cron\"} must be an error")
        assertTrue(refused(mapOf("ci" to true, "interval_seconds" to -5)), "{\"ci\": true, \"interval_seconds\": -5} must be an error")
        assertTrue(refused(mapOf("ci" to true, "machine_off" to true, "local_files" to true)), "{\"ci\": true, \"machine_off\": true, \"local_files\": true} must be an error")
    }
}
