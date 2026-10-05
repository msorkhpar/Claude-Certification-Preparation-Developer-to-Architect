// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { changeNotice, planSession } from "./sessions.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const NOW = 1_800_000_000;
const FILES: Record<string, string> = { "a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4" };
// What was saved about the session last time: its id, name, when it was used and the digest of each file it analysed.
const record: any = { id: "s-base", name: "auth-review", last_used: NOW - 3600, files: { ...FILES } };

// Three situations: nothing changed, one file changed, most files changed.
const cases: Record<string, Record<string, string>> = {
  "unchanged": { ...FILES },
  "one file changed": { ...FILES, "b.py": "x" },
  "most files changed": { "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4" },
};
for (const [name, current] of Object.entries(cases)) {
  const plan = planSession(record, current, NOW) ?? {};
  console.log(`${name}: action=${plan.action} session=${plan.session_id} changed=${JSON.stringify(plan.changed)}`);
}

console.log("notice for the changed one:", JSON.stringify(changeNotice(planSession(record, cases["one file changed"], NOW) ?? {})));
