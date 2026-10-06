/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */

private val log = System.getLogger("rhythm_plan")

data class Choice(val mechanism: String, val reason: String, val intervalMinutes: Int)

private val TRIGGERS = listOf("interval", "event", "condition", "once", "background")
private const val CLOUD_MIN_SECONDS = 3600 // a routine never runs more often than hourly
private const val LOCAL_MIN_SECONDS = 60 // a desktop task or a loop never runs more often than every minute
private const val LOOP_EXPIRY_DAYS = 7 // a recurring task of a session expires after seven days

private fun flag(job: Map<String, Any>, key: String, fallback: Boolean) = job[key] as? Boolean ?: fallback

private fun number(job: Map<String, Any>, key: String, fallback: Int) = (job[key] as? Number)?.toInt() ?: fallback

// TODO 6 of 8 (finish this to pass e5): the rounding. Receives seconds and returns whole minutes, rounding up. Example:
//   59 -> 1, 60 -> 1, 61 -> 2.
private fun minutesOf(seconds: Int) = seconds / 60

private fun cloud(seconds: Int, reason: String): Choice {
    // TODO 5 of 8 (finish this to pass e4): the floor of a cloud routine. Refuse with an error ("a routine runs at most
    //   once an hour") when the interval is below CLOUD_MIN_SECONDS. Example: 1800 seconds -> refused; 3600 -> allowed.
    // TODO 1 of 8 (finish this to pass m1): the plan of a cloud routine. Return the plan with mechanism routine, the
    //   given reason and the interval in minutes (the helper that rounds seconds up). Example: 7200 seconds -> routine,
    //   120 minutes.
    return Choice("routine", reason, 0)
}

private fun local(seconds: Int): Choice {
    require(seconds >= LOCAL_MIN_SECONDS) { "a desktop task runs at most once a minute" }
    return Choice("desktop-task", "durable-and-local", minutesOf(seconds))
}

fun choose(job: Map<String, Any>): Choice {
    log.log(System.Logger.Level.DEBUG, "choose input: {0}", job)
    val trigger = job["trigger"]?.toString() ?: "interval"
    val seconds = number(job, "interval_seconds", 0)
    val days = number(job, "lasts_days", 1)
    val machineOff = flag(job, "machine_off", false)
    val localFiles = flag(job, "local_files", false)
    val sessionOpen = flag(job, "session_open", true)
    // TODO 8 of 8 (finish this to pass e7): the refusals. Refuse with an error when the trigger is not one of TRIGGERS,
    //   when the interval is negative, when lasts_days is below 1, and when the job needs the machine off and local files
    //   at once (a cloud run starts from a fresh clone). Example: trigger weekly -> refused.
    // TODO 2 of 8 (finish this to pass e1): the pipeline rule. When the job runs in CI, return mechanism headless-run
    //   with the reason no-person-present, whatever its trigger, interval or machine. Example: ci true with machine_off
    //   true -> headless-run.
    if (trigger == "condition") return Choice("goal", "until-condition-holds", 0)
    if (trigger == "background") return Choice("background-task", "work-while-it-runs", 0)
    // TODO 3 of 8 (finish this to pass e2): the event rows. For an event trigger: a routine with the reason react-to-
    //   event-unattended when the job is a repository event or the machine is off, otherwise a monitor with the reason
    //   push-not-poll. Example: event with the machine on and no repo event -> monitor.
    if (trigger == "once") {
        if (machineOff) return Choice("routine", "survives-closed-machine", 0)
        // TODO 4 of 8 (finish this to pass e3): the one-off rows. For a once trigger on a machine that stays on, when
        //   the session is closed return a desktop-task with the reason durable-and-local; with the session open it fires
        //   there as a one-shot-task. Example: once, session closed -> desktop-task.
        return Choice("one-shot-task", "single-fire", 0)
    }
    if (machineOff) return cloud(seconds, "survives-closed-machine")
    // TODO 7 of 8 (finish this to pass e6): the expiry of a loop. When the job lasts more than LOOP_EXPIRY_DAYS days,
    //   use a local desktop task if it needs local files, otherwise a cloud routine with the reason outlives-seven-days.
    //   Example: 10 days, no local files -> routine.
    if (!sessionOpen) return local(seconds)
    if (seconds == 0) return Choice("loop-self-paced", "pace-by-what-is-seen", 0)
    return Choice("loop-fixed", "fixed-cadence", maxOf(1, minutesOf(seconds)))
}
