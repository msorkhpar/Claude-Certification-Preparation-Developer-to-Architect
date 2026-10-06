# Practice: choose how a job runs unattended or on a rhythm

A team wants Claude Code to watch a pull request, check a deployment every few minutes, triage an alert at night and review every merge. Each of these can be started in at least
three ways, and each way has a limit that decides it: a loop dies with its session, a routine never runs more often than hourly, a headless run has nobody to answer a prompt. In this
practice you write the decision function of the page "Running on a rhythm": a job described by a few features goes in, and the mechanism that fits, a reason code and the interval in
minutes come out. The model is not called, no schedule is created and nothing is installed. It is in Python, TypeScript, Java and Kotlin;

Python has `choose(job)` in `rhythm_plan.py`; TypeScript has `choose` in `rhythmPlan.ts`; Java has the static method `RhythmPlan.choose` and the record `Choice`; Kotlin has the top-level
function `choose` and the data class `Choice`. Python and TypeScript take a plain object, and Java and Kotlin take a map from the key to its value. The keys are the same strings in
every language, and a missing key has the default shown below. The result has the fields `mechanism`, `reason` and `interval_minutes` (`intervalMinutes` in Java and Kotlin).

## What is already written, and what you write

The starter is a working planner of unattended and scheduled runs with eight gaps cut out of it. Everything that is plumbing is written and correct: the constants, the reading of a job's fields with their defaults, the rows for a condition, a background command, a closed machine and a closed session, the local desktop task and the fixed and self-paced loops. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The routine's interval (unlocks `m1`): a cloud routine carries its interval in whole minutes, rounded up from the job's seconds.
2. The pipeline job (unlocks `e1`): a job that runs in a pipeline is a `headless-run` with the reason `no-person-present`, whatever else is true of it.
3. The event trigger (unlocks `e2`): an event that is a repository event, or that must run while the machine is off, is a `routine` (`react-to-event-unattended`); any other event is watched with a `monitor` (`push-not-poll`).
4. The one-off job and the closed session (unlocks `e3`): a one-off job on a machine that stays on, whose session is closed, is a durable `desktop-task` (`durable-and-local`).
5. The hourly floor (unlocks `e4`): a cloud schedule below one hour (3,600 seconds) is refused with an error; exactly one hour is allowed.
6. Whole minutes (unlocks `e5`): seconds are rounded up to whole minutes (90 seconds are 2 minutes).
7. The seven days (unlocks `e6`): a recurring job that lasts more than 7 days cannot stay a loop of a session: it goes to a desktop task when it needs local files and to a cloud routine otherwise (`outlives-seven-days`).
8. The refusals (unlocks `e7`): an unknown trigger, a negative interval, a duration below 1 day, and a job that needs a closed machine and local files are refused with an error.

`m1` needs gap 1. About twenty lines in all. The steps below describe the whole planner, so you can see how your gaps are used.

## What to write

| Key | Meaning | Default |
|---|---|---|
| `trigger` | what starts a run: `interval` (every so often), `event` (something happens), `condition` (keep going until a check holds), `once` (a single run later) or `background` (a long command Claude started and does not wait for) | `interval` |
| `interval_seconds` | the wanted gap between runs, in seconds; 0 means no fixed gap | 0 |
| `lasts_days` | how long the job must keep running, at least 1 | 1 |
| `machine_off` | the job must run while the computer is off or asleep | false |
| `local_files` | the job needs files on this machine, uncommitted changes included | false |
| `session_open` | a Claude Code session stays open for the job | true |
| `repo_event` | the event is a pull request or a release in a repository | false |
| `ci` | the job runs inside a pipeline, with nobody to answer a prompt | false |

The mechanisms, with their reason codes and what the interval means:

| Mechanism | Reason code | When | `interval_minutes` |
|---|---|---|---|
| `headless-run` | `no-person-present` | a pipeline job | 0 |
| `goal` | `until-condition-holds` | keep working until a check holds | 0 |
| `background-task` | `work-while-it-runs` | a long command Claude does not wait for | 0 |
| `monitor` | `push-not-poll` | react to each line of an event stream in the session | 0 |
| `routine` | `react-to-event-unattended` | an event that is a repository event, or that must run with the machine off | 0 |
| `routine` | `survives-closed-machine` | a run with the machine off | the interval in minutes (0 for a single run) |
| `routine` | `outlives-seven-days` | an interval job that must last more than seven days and needs no local files | the interval in minutes |
| `desktop-task` | `durable-and-local` | a job that outlives its session and needs local files, or a job with no open session | the interval in minutes (0 for a single run) |
| `one-shot-task` | `single-fire` | one run later, in the open session | 0 |
| `loop-fixed` | `fixed-cadence` | a fixed interval in the open session | the interval rounded up to whole minutes, at least 1 |
| `loop-self-paced` | `pace-by-what-is-seen` | no interval in the open session | 0 |

The rules apply in this order:

1. An unknown `trigger`, a negative `interval_seconds`, `lasts_days` below 1, or `machine_off` together with `local_files` (a cloud run starts from a fresh clone) is an error. Say so with the
   language's usual argument error (`ValueError` in Python, `Error` in TypeScript, `IllegalArgumentException` in Java and Kotlin).
2. `ci` gives `headless-run`, whatever else is true.
3. `trigger` `condition` gives `goal`; then `background` gives `background-task`.
4. `trigger` `event` gives `routine` with `react-to-event-unattended` when `repo_event` or `machine_off`, and `monitor` otherwise. The interval is ignored.
5. `trigger` `once`: `machine_off` gives `routine` with `survives-closed-machine`; no open session gives `desktop-task`; otherwise `one-shot-task`. The interval and `lasts_days` are ignored.
6. `trigger` `interval`, the first match wins:
   - `machine_off` gives `routine` with `survives-closed-machine`;
   - `lasts_days` above 7 gives `desktop-task` when `local_files`, and `routine` with `outlives-seven-days` otherwise;
   - no open session gives `desktop-task`;
   - `interval_seconds` 0 gives `loop-self-paced`;
   - anything else gives `loop-fixed`.
7. A `routine` on an interval needs `interval_seconds` of at least 3600, and a `desktop-task` on an interval needs at least 60; below that is an error. A loop has no such error: seconds round up to
   the next whole minute, with a minimum of 1.

## Why each part is there, and what you should see

1. **Nobody to ask.** A pipeline job cannot answer a prompt, so it is a headless run whatever the schedule says. *You should see* `headless-run` for every combination of the other keys.
2. **A condition is not a clock.** Keeping on until a check holds, and a command that runs while Claude works, are not intervals. *You should see* `goal` and `background-task` before any
   event or interval rule, and an event watched with `monitor` unless it must run unattended.
3. **A loop belongs to its session.** It stops when the session ends and a recurring one expires after seven days. *You should see* a loop only with an open session, up to seven days.
4. **A schedule has a floor.** A routine runs at most hourly; a desktop task at most every minute. *You should see* an error, not a silent change of the interval.
5. **Rounding goes up.** A loop fires at whole minutes. *You should see* 30 seconds become 1 minute and 61 seconds become 2.
6. **Unknown means error.** *You should see* an error for a value that is not in the tables, and the default for a key that is missing.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each of the fourteen jobs of the page's bank gets its mechanism, its reason code and its interval |
| `e1` | A pipeline job is a headless run whatever else is true |
| `e2` | A condition or a background command decides before an event, and an event is watched unless it must run unattended |
| `e3` | A one-off job fires in the session unless the machine is off or the session is closed |
| `e4` | A cloud schedule is refused below one hour and a desktop schedule below one minute |
| `e5` | A fixed loop rounds seconds up to whole minutes, and no interval means self-paced |
| `e6` | A recurring loop lasts seven days, so a longer job needs a durable home |
| `e7` | An unknown value is an error, a cloud run cannot see local files, and a missing key takes its default |
