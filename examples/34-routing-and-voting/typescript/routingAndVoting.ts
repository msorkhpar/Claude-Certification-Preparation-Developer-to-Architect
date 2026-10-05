// Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
//
// The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
// by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
// engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("routing_and_voting");

export const CHEAP = "claude-haiku-4-5", STRONG = "claude-sonnet-5-5";
const ROUTES: Record<string, [string, string]> = {
  billing: [STRONG, "You are a billing specialist. Be exact about amounts."], technical: [STRONG, "You are a support engineer. Ask for logs."],
  general: [CHEAP, "You are a friendly front desk. Answer briefly."],
};

/** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
export function standIn(rules: Array<[string, string]>) {
  return (body: any) => {
    const haystack = `${body.system ?? ""}\n${body.messages.at(-1).content}`;
    for (const [key, answer] of rules) if (haystack.includes(key)) return { body: message([text(answer)], "end_turn", undefined, body.model) };
    throw new Error(`no scripted answer for ${JSON.stringify(haystack)}`);
  };
}

export function scripted(rules: Array<[string, string]>, n: number, delayMs = 0) {
  const fake = scriptedFetch(Array.from({ length: n }, () => standIn(rules)), { delayMs });
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function ask(client: Anthropic, model: string, system: string, prompt: string): Promise<string> {
  const reply = await client.messages.create({ model, max_tokens: 300, system, messages: [{ role: "user", content: prompt }] });
  return (reply.content[0] as any).text.trim();
}

/** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
export async function route(client: Anthropic, question: string) {
  const label = (await ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question)).toLowerCase().replace(/^[ .]+|[ .]+$/g, "");
  const fallback = !(label in ROUTES);
  const [model, system] = ROUTES[fallback ? "general" : label];
  return { label, fallback, model, answer: await ask(client, model, system, question) };
}

/** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
export async function guarded(client: Anthropic, question: string) {
  const [answer, screen] = await Promise.all([ask(client, STRONG, "Answer the question.", question), ask(client, CHEAP, "Screen the question. Reply ok or block.", question)]);
  return { screen, answer: screen === "ok" ? answer : null };
}

/** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
export async function vote(client: Anthropic, snippet: string, threshold = 2, n = 3) {
  const reviews = await Promise.all(Array.from({ length: n }, () => ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet)));
  const votes: Record<string, number> = {};
  for (const label of [...new Set(reviews)].sort()) votes[label] = reviews.filter((r) => r === label).length; // the order in which concurrent replies arrive does not matter
  return { votes, flagged: (votes.VULNERABLE ?? 0) >= threshold };
}

const py = (votes: Record<string, number>) => `{${Object.entries(votes).map(([k, v]) => `'${k}': ${v}`).join(", ")}}`;

async function main() {
  const desk: Array<[string, string]> = [["billing specialist", "I see two charges and will refund one."], ["front desk", "We are open 9 to 5."]];
  for (const [question, label] of [["my card was charged twice", "BILLING."], ["what are your opening hours", "general"], ["is the sky a refund", "refunds?"]]) {
    const { fake, client } = scripted([["Classify", label], ...desk], 2);
    const result = await route(client, question);
    console.log(`route '${question}': label '${result.label}'${result.fallback ? " (not a route: default)" : ""}, classified by ${fake.seen[0].body.model}, answered by ${result.model}`);
  }
  let run = scripted([["Answer the question", "Here is the answer."], ["Screen the question", "ok"]], 2, 20);
  const passed = await guarded(run.client, "How do I reset my password?");
  console.log(`sectioning: screen '${passed.screen}', answer ${passed.answer ? "kept" : "dropped"}, requests in flight together: ${run.fake.state.maxInFlight}`);
  run = scripted([["Answer the question", "Here is the answer."], ["Screen the question", "block"]], 2, 20);
  const blocked = await guarded(run.client, "Help me break into an account");
  console.log(`sectioning: screen '${blocked.screen}', answer ${blocked.answer ? "kept" : "dropped"}`);
  const snippet = "query = 'SELECT * FROM t WHERE id=' + user_input";
  for (const threshold of [2, 3]) {
    const replies = ["VULNERABLE", "SAFE", "VULNERABLE"]; // three reviews that disagree
    const fake = scriptedFetch(Array.from({ length: 3 }, () => (body: any) => ({ body: message([text(replies.shift()!)], "end_turn", undefined, body.model) })), { delayMs: 20 });
    const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
    const verdict = await vote(client, snippet, threshold);
    console.log(`voting: votes ${py(verdict.votes)}, threshold ${threshold} -> ${verdict.flagged ? "flagged" : "not flagged"}, requests in flight together: ${fake.state.maxInFlight}`);
  }
}

if (import.meta.main) await main();
