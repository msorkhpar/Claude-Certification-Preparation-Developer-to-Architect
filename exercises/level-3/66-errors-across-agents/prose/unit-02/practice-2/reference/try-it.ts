// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { coordinatorPlan, coverageNote, searchWithRecovery } from "./errorFlow.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A stand-in for a search subagent, like the tests use: it times out once, then answers.
const flaky = (_query: string, attempt: number) =>
  attempt === 1 ? { status: "error", type: "timeout", partial: [] } : { status: "ok", items: ["a", "b"] };

// A search the agent may not run: the failure is not transient.
const forbidden = (_query: string, _attempt: number) => ({ status: "error", type: "permission", partial: [] });

const news = searchWithRecovery("news", flaky);
console.log("transient failure:", JSON.stringify(news));
const filings = searchWithRecovery("filings", forbidden);
console.log("permission failure:", JSON.stringify(filings));

// What the coordinator does with each outcome, and what the final report admits.
const results = { news, filings };
console.log("plan:", JSON.stringify(coordinatorPlan(results)));
console.log(coverageNote(results, ["news", "filings", "patents"]));
