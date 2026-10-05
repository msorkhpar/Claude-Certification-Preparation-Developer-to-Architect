// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { choose } from "./extensionChoice.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Three situations from the scenario bank, described by a few features.
const situations: Record<string, any> = {
  "a rule that must never be broken": { guarantee: true, knowledge: "convention" },
  "a database that needs a connection": { external_system: true },
  "work that must run while the laptop is closed": { timing: "interval", presence: "away" },
};
for (const [name, features] of Object.entries(situations)) {
  console.log(`${name}:`, JSON.stringify(choose(features)));
}
