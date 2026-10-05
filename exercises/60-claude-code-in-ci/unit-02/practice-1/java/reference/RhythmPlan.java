import java.util.List;
import java.util.Map;

/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */
final class RhythmPlan {
    private static final System.Logger LOG = System.getLogger(RhythmPlan.class.getName());
    private RhythmPlan() {}

    record Choice(String mechanism, String reason, int intervalMinutes) {}

    private static final List<String> TRIGGERS = List.of("interval", "event", "condition", "once", "background");
    private static final int CLOUD_MIN_SECONDS = 3600; // a routine never runs more often than hourly
    private static final int LOCAL_MIN_SECONDS = 60; // a desktop task or a loop never runs more often than every minute
    private static final int LOOP_EXPIRY_DAYS = 7; // a recurring task of a session expires after seven days

    private static boolean flag(Map<String, Object> job, String key, boolean fallback) {
        return job.get(key) instanceof Boolean b ? b : fallback;
    }

    private static int number(Map<String, Object> job, String key, int fallback) {
        return job.get(key) instanceof Number n ? n.intValue() : fallback;
    }

    private static int minutesOf(int seconds) {
        return Math.ceilDiv(seconds, 60);
    }

    private static Choice cloud(int seconds, String reason) {
        if (seconds < CLOUD_MIN_SECONDS) throw new IllegalArgumentException("a routine runs at most once an hour");
        return new Choice("routine", reason, minutesOf(seconds));
    }

    private static Choice local(int seconds) {
        if (seconds < LOCAL_MIN_SECONDS) throw new IllegalArgumentException("a desktop task runs at most once a minute");
        return new Choice("desktop-task", "durable-and-local", minutesOf(seconds));
    }

    static Choice choose(Map<String, Object> job) {
        LOG.log(System.Logger.Level.DEBUG, "choose input: {0}", job);
        String trigger = job.get("trigger") == null ? "interval" : String.valueOf(job.get("trigger"));
        int seconds = number(job, "interval_seconds", 0);
        int days = number(job, "lasts_days", 1);
        boolean machineOff = flag(job, "machine_off", false);
        boolean localFiles = flag(job, "local_files", false);
        boolean sessionOpen = flag(job, "session_open", true);
        if (!TRIGGERS.contains(trigger)) throw new IllegalArgumentException("unknown trigger: " + trigger);
        if (seconds < 0) throw new IllegalArgumentException("interval_seconds cannot be negative");
        if (days < 1) throw new IllegalArgumentException("lasts_days starts at 1");
        if (machineOff && localFiles) throw new IllegalArgumentException("a cloud run starts from a fresh clone and sees no local files");
        if (flag(job, "ci", false)) return new Choice("headless-run", "no-person-present", 0);
        if (trigger.equals("condition")) return new Choice("goal", "until-condition-holds", 0);
        if (trigger.equals("background")) return new Choice("background-task", "work-while-it-runs", 0);
        if (trigger.equals("event")) {
            if (flag(job, "repo_event", false) || machineOff) return new Choice("routine", "react-to-event-unattended", 0);
            return new Choice("monitor", "push-not-poll", 0);
        }
        if (trigger.equals("once")) {
            if (machineOff) return new Choice("routine", "survives-closed-machine", 0);
            if (!sessionOpen) return new Choice("desktop-task", "durable-and-local", 0);
            return new Choice("one-shot-task", "single-fire", 0);
        }
        if (machineOff) return cloud(seconds, "survives-closed-machine");
        if (days > LOOP_EXPIRY_DAYS) return localFiles ? local(seconds) : cloud(seconds, "outlives-seven-days");
        if (!sessionOpen) return local(seconds);
        if (seconds == 0) return new Choice("loop-self-paced", "pace-by-what-is-seen", 0);
        return new Choice("loop-fixed", "fixed-cadence", Math.max(1, minutesOf(seconds)));
    }
}
