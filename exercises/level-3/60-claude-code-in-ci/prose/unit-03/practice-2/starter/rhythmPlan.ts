import { logger } from "./logger.ts";
const log = logger("rhythm_plan");
/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */

const TRIGGERS = ["interval", "event", "condition", "once", "background"];
const CLOUD_MIN_SECONDS = 3600; // a routine never runs more often than hourly
const LOCAL_MIN_SECONDS = 60; // a desktop task or a loop never runs more often than every minute
const LOOP_EXPIRY_DAYS = 7; // a recurring task of a session expires after seven days

type Plan = { mechanism: string; reason: string; interval_minutes: number };

function pick(mechanism: string, reason: string, interval_minutes = 0): Plan {
  return { mechanism, reason, interval_minutes };
}

function minutesOf(seconds: number): number {
  // TODO 6 of 8 (finish this to pass e5): the rounding. Receives seconds and returns whole minutes, rounding up.
  //   Example: 59 -> 1, 60 -> 1, 61 -> 2.
  return Math.floor(seconds / 60);
}

function cloud(seconds: number, reason: string): Plan {
  // TODO 5 of 8 (finish this to pass e4): the floor of a cloud routine. Refuse with an error ("a routine runs at most
  //   once an hour") when the interval is below CLOUD_MIN_SECONDS. Example: 1800 seconds -> refused; 3600 -> allowed.
  // TODO 1 of 8 (finish this to pass m1): the plan of a cloud routine. Return the plan with mechanism routine, the given
  //   reason and the interval in minutes (the helper that rounds seconds up). Example: 7200 seconds -> routine, 120
  //   minutes.
  return pick("routine", reason);
}

function local(seconds: number): Plan {
  if (seconds < LOCAL_MIN_SECONDS) throw new Error("a desktop task runs at most once a minute");
  return pick("desktop-task", "durable-and-local", minutesOf(seconds));
}

export function choose(job: Record<string, any>): Plan {
  log.debug("choose input", job);
  const trigger = job.trigger ?? "interval";
  const seconds = job.interval_seconds ?? 0;
  const days = job.lasts_days ?? 1;
  const machineOff = job.machine_off ?? false;
  const localFiles = job.local_files ?? false;
  const sessionOpen = job.session_open ?? true;
  // TODO 8 of 8 (finish this to pass e7): the refusals. Refuse with an error when the trigger is not one of TRIGGERS,
  //   when the interval is negative, when lasts_days is below 1, and when the job needs the machine off and local files at
  //   once (a cloud run starts from a fresh clone). Example: trigger weekly -> refused.
  // TODO 2 of 8 (finish this to pass e1): the pipeline rule. When the job runs in CI, return mechanism headless-run with
  //   the reason no-person-present, whatever its trigger, interval or machine. Example: ci true with machine_off true ->
  //   headless-run.
  if (trigger === "condition") return pick("goal", "until-condition-holds");
  if (trigger === "background") return pick("background-task", "work-while-it-runs");
  // TODO 3 of 8 (finish this to pass e2): the event rows. For an event trigger: a routine with the reason react-to-
  //   event-unattended when the job is a repository event or the machine is off, otherwise a monitor with the reason push-
  //   not-poll. Example: event with the machine on and no repo event -> monitor.
  if (trigger === "once") {
    if (machineOff) return pick("routine", "survives-closed-machine");
    // TODO 4 of 8 (finish this to pass e3): the one-off rows. For a once trigger on a machine that stays on, when the
    //   session is closed return a desktop-task with the reason durable-and-local; with the session open it fires there as
    //   a one-shot-task. Example: once, session closed -> desktop-task.
    return pick("one-shot-task", "single-fire");
  }
  if (machineOff) return cloud(seconds, "survives-closed-machine");
  // TODO 7 of 8 (finish this to pass e6): the expiry of a loop. When the job lasts more than LOOP_EXPIRY_DAYS days, use
  //   a local desktop task if it needs local files, otherwise a cloud routine with the reason outlives-seven-days.
  //   Example: 10 days, no local files -> routine.
  if (!sessionOpen) return local(seconds);
  if (seconds === 0) return pick("loop-self-paced", "pace-by-what-is-seen");
  return pick("loop-fixed", "fixed-cadence", Math.max(1, minutesOf(seconds)));
}
