import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "rhythmPlan.ts")).href);

type Plan = { mechanism: string; reason: string; interval_minutes: number };

function choose(job: Record<string, any>): Plan {
  const value = solution.choose({ ...job });
  assert.ok(value !== null && value !== undefined, "choose returned nothing");
  return value;
}

function refused(job: Record<string, any>): boolean {
  try {
    choose(job);
  } catch (error) {
    if (error instanceof assert.AssertionError) throw error;
    return true;
  }
  return false;
}

/** Every combination of the axes, each added to the base job. */
function sweep(axes: Array<[string, any[]]>, base: Record<string, any>): Array<Record<string, any>> {
  let combos: Array<Record<string, any>> = [{ ...base }];
  for (const [key, values] of axes) combos = combos.flatMap((c) => values.map((v) => ({ ...c, [key]: v })));
  return combos;
}

const pick = (mechanism: string, reason: string, interval_minutes = 0): Plan => ({ mechanism, reason, interval_minutes });

// The bank of the page: id, job, expected mechanism, reason code and interval in minutes.
const BANK: Array<[string, Record<string, any>, string, string, number]> = [
  ["r01", {}, "loop-self-paced", "pace-by-what-is-seen", 0],
  ["r02", { interval_seconds: 300 }, "loop-fixed", "fixed-cadence", 5],
  ["r03", { interval_seconds: 30 }, "loop-fixed", "fixed-cadence", 1],
  ["r04", { trigger: "once" }, "one-shot-task", "single-fire", 0],
  ["r05", { trigger: "once", machine_off: true }, "routine", "survives-closed-machine", 0],
  ["r06", { interval_seconds: 86400, machine_off: true }, "routine", "survives-closed-machine", 1440],
  ["r07", { interval_seconds: 1800, lasts_days: 30, local_files: true }, "desktop-task", "durable-and-local", 30],
  ["r08", { interval_seconds: 7200, lasts_days: 30 }, "routine", "outlives-seven-days", 120],
  ["r09", { trigger: "event" }, "monitor", "push-not-poll", 0],
  ["r10", { trigger: "event", repo_event: true }, "routine", "react-to-event-unattended", 0],
  ["r11", { trigger: "condition" }, "goal", "until-condition-holds", 0],
  ["r12", { trigger: "background" }, "background-task", "work-while-it-runs", 0],
  ["r13", { ci: true, interval_seconds: 600 }, "headless-run", "no-person-present", 0],
  ["r14", { interval_seconds: 900, session_open: false, local_files: true }, "desktop-task", "durable-and-local", 15],
];

test("m1 every job of the bank gets its mechanism its reason and its interval", () => {
  const wrong = BANK.filter(([, j, m, r, n]) => { const c = choose(j); return c.mechanism !== m || c.reason !== r || c.interval_minutes !== n; }).map(([id]) => id);
  assert.deepEqual(wrong, [], `these jobs got the wrong mechanism, reason or interval: ${wrong}`);
});

test("e1 a pipeline job is a headless run whatever else is true", () => {
  for (const j of sweep([["trigger", ["interval", "event", "condition", "once", "background"]], ["machine_off", [false, true]], ["lasts_days", [1, 30]], ["repo_event", [false, true]], ["session_open", [true, false]]], { ci: true })) assert.deepEqual(choose(j), pick("headless-run", "no-person-present", 0), JSON.stringify(j));
  assert.deepEqual(choose({ ci: true, interval_seconds: 600 }), pick("headless-run", "no-person-present", 0), "{\"ci\": true, \"interval_seconds\": 600}");
  assert.deepEqual(choose({ ci: true, interval_seconds: 90, local_files: true }), pick("headless-run", "no-person-present", 0), "{\"ci\": true, \"interval_seconds\": 90, \"local_files\": true}");
});

test("e2 a condition or a background command decides before an event and an event is watched unless it must run unattended", () => {
  for (const j of sweep([["machine_off", [false, true]], ["repo_event", [false, true]], ["interval_seconds", [0, 600]], ["lasts_days", [1, 30]]], { trigger: "condition" })) assert.deepEqual(choose(j), pick("goal", "until-condition-holds", 0), JSON.stringify(j));
  for (const j of sweep([["machine_off", [false, true]], ["repo_event", [false, true]], ["interval_seconds", [0, 600]], ["lasts_days", [1, 30]]], { trigger: "background" })) assert.deepEqual(choose(j), pick("background-task", "work-while-it-runs", 0), JSON.stringify(j));
  assert.deepEqual(choose({ trigger: "event" }), pick("monitor", "push-not-poll", 0), "{\"trigger\": \"event\"}");
  assert.deepEqual(choose({ trigger: "event", interval_seconds: 600 }), pick("monitor", "push-not-poll", 0), "{\"trigger\": \"event\", \"interval_seconds\": 600}");
  assert.deepEqual(choose({ trigger: "event", repo_event: true }), pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"repo_event\": true}");
  assert.deepEqual(choose({ trigger: "event", machine_off: true }), pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"machine_off\": true}");
  assert.deepEqual(choose({ trigger: "event", repo_event: true, machine_off: true }), pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"repo_event\": true, \"machine_off\": true}");
});

test("e3 a one off job fires in the session unless the machine is off or the session is closed", () => {
  assert.deepEqual(choose({ trigger: "once" }), pick("one-shot-task", "single-fire", 0), "{\"trigger\": \"once\"}");
  assert.deepEqual(choose({ trigger: "once", interval_seconds: 120, lasts_days: 30 }), pick("one-shot-task", "single-fire", 0), "{\"trigger\": \"once\", \"interval_seconds\": 120, \"lasts_days\": 30}");
  assert.deepEqual(choose({ trigger: "once", machine_off: true }), pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"machine_off\": true}");
  assert.deepEqual(choose({ trigger: "once", session_open: false }), pick("desktop-task", "durable-and-local", 0), "{\"trigger\": \"once\", \"session_open\": false}");
  assert.deepEqual(choose({ trigger: "once", session_open: false, machine_off: true }), pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"session_open\": false, \"machine_off\": true}");
  assert.deepEqual(choose({ trigger: "once", machine_off: true, lasts_days: 30 }), pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"machine_off\": true, \"lasts_days\": 30}");
});

test("e4 a cloud schedule is refused below one hour and a desktop schedule below one minute", () => {
  assert.ok(refused({ interval_seconds: 3599, machine_off: true }), "{\"interval_seconds\": 3599, \"machine_off\": true} must be an error");
  assert.ok(refused({ interval_seconds: 60, machine_off: true }), "{\"interval_seconds\": 60, \"machine_off\": true} must be an error");
  assert.ok(refused({ machine_off: true }), "{\"machine_off\": true} must be an error");
  assert.ok(refused({ interval_seconds: 1800, lasts_days: 30 }), "{\"interval_seconds\": 1800, \"lasts_days\": 30} must be an error");
  assert.ok(refused({ lasts_days: 30 }), "{\"lasts_days\": 30} must be an error");
  assert.ok(refused({ interval_seconds: 59, lasts_days: 30, local_files: true }), "{\"interval_seconds\": 59, \"lasts_days\": 30, \"local_files\": true} must be an error");
  assert.ok(refused({ lasts_days: 30, local_files: true }), "{\"lasts_days\": 30, \"local_files\": true} must be an error");
  assert.ok(refused({ interval_seconds: 30, session_open: false }), "{\"interval_seconds\": 30, \"session_open\": false} must be an error");
  assert.ok(refused({ session_open: false }), "{\"session_open\": false} must be an error");
  assert.deepEqual(choose({ interval_seconds: 3600, machine_off: true }), pick("routine", "survives-closed-machine", 60), "{\"interval_seconds\": 3600, \"machine_off\": true}");
  assert.deepEqual(choose({ interval_seconds: 5400, machine_off: true }), pick("routine", "survives-closed-machine", 90), "{\"interval_seconds\": 5400, \"machine_off\": true}");
  assert.deepEqual(choose({ interval_seconds: 60, lasts_days: 30, local_files: true }), pick("desktop-task", "durable-and-local", 1), "{\"interval_seconds\": 60, \"lasts_days\": 30, \"local_files\": true}");
  assert.deepEqual(choose({ interval_seconds: 61, session_open: false }), pick("desktop-task", "durable-and-local", 2), "{\"interval_seconds\": 61, \"session_open\": false}");
  assert.deepEqual(choose({ interval_seconds: 30 }), pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 30}");
});

test("e5 a fixed loop rounds seconds up to whole minutes and no interval means self paced", () => {
  assert.deepEqual(choose({ interval_seconds: 1 }), pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 1}");
  assert.deepEqual(choose({ interval_seconds: 59 }), pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 59}");
  assert.deepEqual(choose({ interval_seconds: 60 }), pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 60}");
  assert.deepEqual(choose({ interval_seconds: 61 }), pick("loop-fixed", "fixed-cadence", 2), "{\"interval_seconds\": 61}");
  assert.deepEqual(choose({ interval_seconds: 300 }), pick("loop-fixed", "fixed-cadence", 5), "{\"interval_seconds\": 300}");
  assert.deepEqual(choose({ interval_seconds: 3600 }), pick("loop-fixed", "fixed-cadence", 60), "{\"interval_seconds\": 3600}");
  assert.deepEqual(choose({ interval_seconds: 86400 }), pick("loop-fixed", "fixed-cadence", 1440), "{\"interval_seconds\": 86400}");
  assert.deepEqual(choose({ interval_seconds: 0 }), pick("loop-self-paced", "pace-by-what-is-seen", 0), "{\"interval_seconds\": 0}");
  assert.deepEqual(choose({ trigger: "interval", lasts_days: 7 }), pick("loop-self-paced", "pace-by-what-is-seen", 0), "{\"trigger\": \"interval\", \"lasts_days\": 7}");
});

test("e6 a recurring loop lasts seven days so a longer job needs a durable home", () => {
  assert.deepEqual(choose({ interval_seconds: 600, lasts_days: 7 }), pick("loop-fixed", "fixed-cadence", 10), "{\"interval_seconds\": 600, \"lasts_days\": 7}");
  assert.deepEqual(choose({ interval_seconds: 600, lasts_days: 8, local_files: true }), pick("desktop-task", "durable-and-local", 10), "{\"interval_seconds\": 600, \"lasts_days\": 8, \"local_files\": true}");
  assert.deepEqual(choose({ interval_seconds: 3600, lasts_days: 8 }), pick("routine", "outlives-seven-days", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 8}");
  assert.deepEqual(choose({ interval_seconds: 3600, lasts_days: 8, machine_off: true }), pick("routine", "survives-closed-machine", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 8, \"machine_off\": true}");
  assert.deepEqual(choose({ interval_seconds: 3600, lasts_days: 365, session_open: false }), pick("routine", "outlives-seven-days", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"session_open\": false}");
  assert.deepEqual(choose({ interval_seconds: 3600, lasts_days: 365, local_files: true, session_open: false }), pick("desktop-task", "durable-and-local", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"local_files\": true, \"session_open\": false}");
  assert.ok(refused({ lasts_days: 8 }), "{\"lasts_days\": 8} must be an error");
  assert.ok(refused({ lasts_days: 8, local_files: true }), "{\"lasts_days\": 8, \"local_files\": true} must be an error");
});

test("e7 an unknown value is an error a cloud run cannot see local files and a missing key takes its default", () => {
  assert.deepEqual(choose({}), pick("loop-self-paced", "pace-by-what-is-seen", 0), "{}");
  assert.ok(refused({ trigger: "cron" }), "{\"trigger\": \"cron\"} must be an error");
  assert.ok(refused({ trigger: "" }), "{\"trigger\": \"\"} must be an error");
  assert.ok(refused({ interval_seconds: -1 }), "{\"interval_seconds\": -1} must be an error");
  assert.ok(refused({ lasts_days: 0 }), "{\"lasts_days\": 0} must be an error");
  assert.ok(refused({ machine_off: true, local_files: true, interval_seconds: 7200 }), "{\"machine_off\": true, \"local_files\": true, \"interval_seconds\": 7200} must be an error");
  assert.ok(refused({ trigger: "once", machine_off: true, local_files: true }), "{\"trigger\": \"once\", \"machine_off\": true, \"local_files\": true} must be an error");
  assert.ok(refused({ ci: true, trigger: "cron" }), "{\"ci\": true, \"trigger\": \"cron\"} must be an error");
  assert.ok(refused({ ci: true, interval_seconds: -5 }), "{\"ci\": true, \"interval_seconds\": -5} must be an error");
  assert.ok(refused({ ci: true, machine_off: true, local_files: true }), "{\"ci\": true, \"machine_off\": true, \"local_files\": true} must be an error");
});
