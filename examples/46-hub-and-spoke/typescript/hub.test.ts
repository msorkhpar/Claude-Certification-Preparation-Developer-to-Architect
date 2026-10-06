import { test } from "node:test";
import assert from "node:assert/strict";
import { MODEL, REPORTS, SUBAGENT_SYSTEM, SUBTASKS, clientFor, plan, runSubagent, synthesize } from "./hub.ts";
import { message, text } from "../../../harness/ts/scriptedFetch.ts";

const usage = { input_tokens: 1, output_tokens: 1 };

test("the plan is read from the tool call and no choice is forced", async () => {
  const { fake, client } = clientFor([{ body: message([{ type: "tool_use", id: "t", name: "plan", input: { subtasks: SUBTASKS } }], "tool_use", usage, MODEL) }]);
  assert.deepEqual(await plan(client, "q"), SUBTASKS);
  assert.equal(fake.seen[0].body.tool_choice, undefined);
  assert.ok(fake.seen[0].body.messages[0].content.includes("plan tool"));
});

test("a subagent request holds its brief and the role prompt only", async () => {
  const { fake, client } = clientFor([{ body: message([text(REPORTS[0])], "end_turn", usage, MODEL) }]);
  await runSubagent(client, SUBTASKS[0].brief);
  const request = fake.seen[0].body;
  assert.equal(request.system, SUBAGENT_SYSTEM);
  assert.deepEqual(request.messages, [{ role: "user", content: SUBTASKS[0].brief }]);
  assert.equal(request.tools, undefined);
});

test("the synthesis request holds every finding", async () => {
  const { fake, client } = clientFor([{ body: message([text("done")], "end_turn", usage, MODEL) }]);
  await synthesize(client, "q", [["chips", REPORTS[0]], ["cars", REPORTS[1]]]);
  const body = JSON.stringify(fake.seen[0].body);
  assert.ok(body.includes("CHIPS-REPORT") && body.includes("CARS-REPORT") && !body.includes("RATES-REPORT"));
});
