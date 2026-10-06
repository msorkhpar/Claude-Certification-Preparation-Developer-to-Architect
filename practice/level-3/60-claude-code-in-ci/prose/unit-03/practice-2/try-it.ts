// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { choose } from "./rhythmPlan.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Jobs a team wants to repeat: each is a small record of what is known about it.
const jobs: Record<string, Record<string, any>> = {
  "check the build while I watch": {},
  "poll a status page every 5 minutes": { interval_seconds: 300 },
  "nightly report, laptop may be closed": { interval_seconds: 86400, machine_off: true },
  "react to a pull request unattended": { trigger: "event", repo_event: true },
  "run in a pipeline": { ci: true, interval_seconds: 600 },
};
for (const [name, job] of Object.entries(jobs)) console.log(`${name}:`, JSON.stringify(choose(job)));
