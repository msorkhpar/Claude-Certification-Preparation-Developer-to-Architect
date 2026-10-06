/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */

private val log = System.getLogger("rhythm_plan")

data class Choice(val mechanism: String, val reason: String, val intervalMinutes: Int)

private val TRIGGERS = listOf("interval", "event", "condition", "once", "background")
private const val CLOUD_MIN_SECONDS = 3600 // a routine never runs more often than hourly
private const val LOCAL_MIN_SECONDS = 60 // a desktop task or a loop never runs more often than every minute
private const val LOOP_EXPIRY_DAYS = 7 // a recurring task of a session expires after seven days

private fun flag(job: Map<String, Any>, key: String, fallback: Boolean) = job[key] as? Boolean ?: fallback

private fun number(job: Map<String, Any>, key: String, fallback: Int) = (job[key] as? Number)?.toInt() ?: fallback

private fun minutesOf(seconds: Int) = Math.ceilDiv(seconds, 60)

private fun cloud(seconds: Int, reason: String): Choice {
    require(seconds >= CLOUD_MIN_SECONDS) { "a routine runs at most once an hour" }
    return Choice("routine", reason, minutesOf(seconds))
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
    require(trigger in TRIGGERS) { "unknown trigger: $trigger" }
    require(seconds >= 0) { "interval_seconds cannot be negative" }
    require(days >= 1) { "lasts_days starts at 1" }
    require(!(machineOff && localFiles)) { "a cloud run starts from a fresh clone and sees no local files" }
    if (flag(job, "ci", false)) return Choice("headless-run", "no-person-present", 0)
    if (trigger == "condition") return Choice("goal", "until-condition-holds", 0)
    if (trigger == "background") return Choice("background-task", "work-while-it-runs", 0)
    if (trigger == "event") {
        if (flag(job, "repo_event", false) || machineOff) return Choice("routine", "react-to-event-unattended", 0)
        return Choice("monitor", "push-not-poll", 0)
    }
    if (trigger == "once") {
        if (machineOff) return Choice("routine", "survives-closed-machine", 0)
        if (!sessionOpen) return Choice("desktop-task", "durable-and-local", 0)
        return Choice("one-shot-task", "single-fire", 0)
    }
    if (machineOff) return cloud(seconds, "survives-closed-machine")
    if (days > LOOP_EXPIRY_DAYS) return if (localFiles) local(seconds) else cloud(seconds, "outlives-seven-days")
    if (!sessionOpen) return local(seconds)
    if (seconds == 0) return Choice("loop-self-paced", "pace-by-what-is-seen", 0)
    return Choice("loop-fixed", "fixed-cadence", maxOf(1, minutesOf(seconds)))
}
