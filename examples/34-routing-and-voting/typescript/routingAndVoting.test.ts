import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { CHEAP, STRONG, guarded, route, scripted, vote } from "./routingAndVoting.ts";

const desk: Array<[string, string]> = [["billing specialist", "I see two charges."], ["front desk", "Open 9 to 5."]];

test("a label picks the model and an unknown label takes the default", async () => {
  let run = scripted([["Classify", "Billing."], ...desk], 2);
  const billing = await route(run.client, "charged twice");
  assert.deepEqual([billing.label, billing.model, billing.fallback, run.fake.seen[0].body.model], ["billing", STRONG, false, CHEAP]);
  run = scripted([["Classify", "refunds?"], ...desk], 2);
  const odd = await route(run.client, "hello");
  assert.deepEqual([odd.label, odd.model, odd.fallback, odd.answer], ["refunds?", CHEAP, true, "Open 9 to 5."]);
});

test("sectioning runs both calls together and drops the answer when the screen blocks", async () => {
  const run = scripted([["Answer the question", "Fine."], ["Screen the question", "block"]], 2, 20);
  assert.deepEqual(await guarded(run.client, "q"), { screen: "block", answer: null });
  assert.equal(run.fake.state.maxInFlight, 2);
});

test("voting counts the reviews against the threshold", async () => {
  const verdict = async (threshold: number) => {
    const replies = ["VULNERABLE", "SAFE", "VULNERABLE"];
    const fake = scriptedFetch(Array.from({ length: 3 }, () => (body: any) => ({ body: message([text(replies.shift()!)], "end_turn", undefined, body.model) })), { delayMs: 10 });
    return { result: await vote(new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }), "code", threshold), flight: fake.state.maxInFlight };
  };
  const [two, three] = [await verdict(2), await verdict(3)];
  assert.ok(two.result.flagged && !three.result.flagged && two.flight === 3);
  assert.deepEqual(two.result.votes, { SAFE: 1, VULNERABLE: 2 });
});
