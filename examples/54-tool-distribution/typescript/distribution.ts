// Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
//
// No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
import { logger } from "./logger.ts";
const log = logger("distribution");

export const CATALOG: Record<string, string[]> = {
  web_search: ["web"], fetch_page: ["web"], verify_fact: ["web", "synthesis"], load_document: ["documents"], extract_data_points: ["documents"],
  summarize_content: ["synthesis"], write_report: ["reports"], send_report: ["reports"],
};
const IRREVERSIBLE = new Set(["send_report"]);
export const ROLES: Record<string, string> = { searcher: "web", analyst: "documents", synthesizer: "synthesis", reporter: "reports" };
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]);

export function toolsFor(role: string): string[] {
  return Object.entries(CATALOG).filter(([name, tags]) => tags.includes(ROLES[role]) && !IRREVERSIBLE.has(name)).map(([name]) => name);
}

/** The request settings for a turn whose first call must be `forced`. */
export function turnFor(model: string, forced: string, tools: string[]) {
  if (NO_FORCING.has(model)) return { tool_choice: "auto", tools: [forced], check_reply: true };
  return { tool_choice: `tool:${forced}`, tools, check_reply: false };
}

export function cacheCost(before: { tool_choice: string; tools: string[] }, after: { tool_choice: string; tools: string[] }): string {
  if (JSON.stringify(before.tools) !== JSON.stringify(after.tools)) return "everything (the tool definitions changed)";
  if (before.tool_choice !== after.tool_choice) return "the cached messages (tool_choice changed)";
  return "nothing";
}

export function madeTheCall(reply: Array<[string, string]>, forced: string): boolean {
  const calls = reply.filter((block) => block[0] === "tool_use");
  return calls.length > 0 && calls[0][1] === forced;
}

export function allowed(tool: string, amount: number, approved: boolean, cap = 200): string {
  if (tool !== "refund" && tool !== "lookup") return "refused: unknown tool";
  if (tool === "lookup") return "run";
  if (amount > cap) return `refused: above the limit of ${cap}, send to a person`;
  return approved ? "run" : "wait: a person must approve";
}

function main() {
  console.log(`catalog: ${Object.keys(CATALOG).length} tools`);
  for (const role of Object.keys(ROLES)) console.log(`  ${role}: ${toolsFor(role).join(", ")}`);
  console.log("a synthesizer that may also check one fact has verify_fact, and nothing else from the web");
  console.log("first call must be extract_metadata:");
  const both = ["extract_metadata", "enrich"];
  for (const model of ["claude-opus-5", "claude-sonnet-5-5"]) {
    const turn = turnFor(model, "extract_metadata", both);
    console.log(`  ${model}: tool_choice=${turn.tool_choice}, tools=[${turn.tools.map((t) => `'${t}'`).join(", ")}], check the reply=${turn.check_reply ? "True" : "False"}`);
    console.log(`    cost against a turn with auto and both tools: ${cacheCost({ tool_choice: "auto", tools: both }, turn)}`);
  }
  console.log("a reply to the fallback turn:");
  const replies: Array<[string, Array<[string, string]>]> = [["text only", [["text", "I will look at the metadata."]]], ["the right call", [["tool_use", "extract_metadata"]]], ["another tool", [["tool_use", "enrich"]]]];
  for (const [label, reply] of replies) console.log(`  ${label}: ${madeTheCall(reply, "extract_metadata") ? "accept" : "re-ask once, then escalate"}`);
  console.log("refund decisions:");
  const calls: Array<[string, number, boolean]> = [["lookup", 0, false], ["refund", 150, false], ["refund", 150, true], ["refund", 400, true], ["delete_account", 0, true]];
  for (const [tool, amount, approved] of calls) console.log(`  ${tool} ${amount}, approved=${approved ? "yes" : "no"}: ${allowed(tool, amount, approved)}`);
}

if (import.meta.main) main();
