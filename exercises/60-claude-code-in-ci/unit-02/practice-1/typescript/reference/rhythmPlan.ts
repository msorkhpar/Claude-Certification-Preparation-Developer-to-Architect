import { logger } from "../logger.ts";
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
  return Math.ceil(seconds / 60);
}

function cloud(seconds: number, reason: string): Plan {
  if (seconds < CLOUD_MIN_SECONDS) throw new Error("a routine runs at most once an hour");
  return pick("routine", reason, minutesOf(seconds));
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
  if (!TRIGGERS.includes(trigger)) throw new Error(`unknown trigger: ${trigger}`);
  if (seconds < 0) throw new Error("interval_seconds cannot be negative");
  if (days < 1) throw new Error("lasts_days starts at 1");
  if (machineOff && localFiles) throw new Error("a cloud run starts from a fresh clone and sees no local files");
  if (job.ci ?? false) return pick("headless-run", "no-person-present");
  if (trigger === "condition") return pick("goal", "until-condition-holds");
  if (trigger === "background") return pick("background-task", "work-while-it-runs");
  if (trigger === "event") {
    if ((job.repo_event ?? false) || machineOff) return pick("routine", "react-to-event-unattended");
    return pick("monitor", "push-not-poll");
  }
  if (trigger === "once") {
    if (machineOff) return pick("routine", "survives-closed-machine");
    if (!sessionOpen) return pick("desktop-task", "durable-and-local");
    return pick("one-shot-task", "single-fire");
  }
  if (machineOff) return cloud(seconds, "survives-closed-machine");
  if (days > LOOP_EXPIRY_DAYS) return localFiles ? local(seconds) : cloud(seconds, "outlives-seven-days");
  if (!sessionOpen) return local(seconds);
  if (seconds === 0) return pick("loop-self-paced", "pace-by-what-is-seen");
  return pick("loop-fixed", "fixed-cadence", Math.max(1, minutesOf(seconds)));
}
