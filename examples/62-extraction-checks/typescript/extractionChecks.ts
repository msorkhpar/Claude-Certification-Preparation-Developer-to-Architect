import { logger } from "./logger.ts";
const log = logger("extraction_checks");
/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
export const DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR";
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]);
export type Answer = { items: number[]; total: number; evidence: string };

/** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
export function scriptedValue(document: string, nullable: boolean): string | null {
  const marker = "PO ";
  if (document.includes(marker)) return document.split(marker)[1].split(/\s+/)[0];
  return nullable ? null : "PO-0000";
}

/** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
export function check(record: Answer, document: string): string[] {
  log.debug("check input", record);
  const problems: string[] = [];
  const sum = record.items.reduce((a, b) => a + b, 0);
  if (Math.abs(sum - record.total) > 0.005) problems.push(`total: the items add up to ${sum}, not ${record.total}`);
  if (!document.includes(record.evidence)) problems.push("evidence: this quotation is not in the document");
  return problems;
}

/** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
export function extract(document: string, replies: Answer[], maxRetries = 1): { status: string; attempts: number; feedback: string[] } {
  const feedback: string[] = [];
  const tried = replies.slice(0, maxRetries + 1);
  for (const [i, reply] of tried.entries()) {
    const problems = check(reply, document);
    if (problems.length === 0) return { status: "valid", attempts: i + 1, feedback };
    feedback.push(`Document:\n${document}\nYour answer:\n${JSON.stringify(reply)}\nProblems:\n` + problems.map((p) => `- ${p}`).join("\n"));
  }
  return { status: "failed", attempts: tried.length, feedback };
}

/** outcomes: [status, correct] per document. The figure on validated records alone leaves out every document that failed. */
export function accuracy(outcomes: Array<[string, boolean]>): { validated_only: number; all_documents: number } {
  const valid = outcomes.filter(([status]) => status === "valid").map(([, correct]) => (correct ? 1 : 0) as number);
  const right = valid.reduce((a, b) => a + b, 0);
  return { validated_only: valid.length ? Math.round((right / valid.length) * 100) / 100 : 0, all_documents: outcomes.length ? Math.round((right / outcomes.length) * 100) / 100 : 0 };
}

/** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
export function requestChoice(model: string, tools: string[]): { tool_choice: string; check_reply: boolean } {
  if (NO_FORCING.has(model)) return { tool_choice: "auto", check_reply: true };
  return { tool_choice: tools.length > 1 ? "any" : `tool:${tools[0]}`, check_reply: false };
}

function main() {
  const noPo = "Invoice from Acme Tools. Total due: 130.00 EUR";
  console.log("purchase order, document without one: required ->", scriptedValue(noPo, false), "| nullable ->", scriptedValue(noPo, true));
  const wrong: Answer = { items: [100.0, 20.5], total: 130.0, evidence: "Total due: 130.00 EUR" };
  const right: Answer = { items: [100.0, 20.5, 9.5], total: 130.0, evidence: "Total due: 130.00 EUR" };
  const fabricated: Answer = { ...right, evidence: "Total due: 130.00 USD" };
  const result = extract(DOC, [wrong, right]);
  console.log("answer 1 wrong, answer 2 right:", result.status, "after", result.attempts, "attempts");
  console.log(result.feedback[0]);
  console.log("two answers that stay wrong:", extract(DOC, [wrong, fabricated]).status);
  const outcomes: Array<[string, boolean]> = [...Array(5).fill(["valid", true]), ["valid", false], ...Array(4).fill(["failed", false])];
  console.log("accuracy of 10 documents (6 valid, 5 of them right):", JSON.stringify(accuracy(outcomes)));
  for (const model of ["claude-haiku-4-5", "claude-sonnet-5-5"]) console.log(`${model}, two extraction tools:`, JSON.stringify(requestChoice(model, ["extract_invoice", "extract_receipt"])));
}

if (import.meta.main) main();
