import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { Conversation } = await import(pathToFileURL(resolve(dir, "conversation.ts")).href);

function answer(text: string, stop = "end_turn", usage = [10, 5], blocks: any[] | null = null) {
  const content = blocks ?? [{ type: "text", text }];
  return { id: "msg_x", type: "message", role: "assistant", model: "claude-sonnet-5-5", content, stop_reason: stop, stop_sequence: null, usage: { input_tokens: usage[0], output_tokens: usage[1] } };
}

/** A scripted `send`: replies in order (an Error is thrown); keeps the bodies it was given, without copying. */
class Script {
  replies: any[];
  bodies: any[] = [];
  constructor(...replies: any[]) {
    this.replies = replies;
  }
  call = (body: any) => {
    this.bodies.push(body); // no copy: a client that shares its array with us shows it here
    const item = this.replies.shift();
    if (item instanceof Error) throw item;
    return item;
  };
}

function errorOf(fn: () => unknown): unknown {
  try {
    fn();
  } catch (err) {
    return err;
  }
  return null;
}

test("m1 every request carries the whole history in order", () => {
  const send = new Script(answer("Paris."), answer("Since 987."), answer("The Seine."));
  const chat = new Conversation(send.call, "claude-sonnet-5-5", 64);
  assert.equal(chat.say("Capital of France?").text, "Paris.");
  chat.say("Since when?");
  chat.say("Its river?");
  assert.deepEqual(send.bodies.map((b) => b.messages.length), [1, 3, 5]);
  assert.deepEqual(send.bodies[2].messages.map((m: any) => m.role), ["user", "assistant", "user", "assistant", "user"]);
  assert.deepEqual(send.bodies[1].messages[1], { role: "assistant", content: [{ type: "text", text: "Paris." }] });
  assert.ok(send.bodies[0].model === "claude-sonnet-5-5" && send.bodies[0].max_tokens === 64);
});

test("e1 usage adds up over the turns", () => {
  const chat = new Conversation(new Script(answer("a", "end_turn", [12, 4]), answer("b", "end_turn", [30, 9])).call, "m", 64);
  chat.say("one");
  chat.say("two");
  assert.deepEqual(chat.totals(), { input_tokens: 42, output_tokens: 13 });
});

test("e2 a failed call leaves no dangling user turn", () => {
  const send = new Script(answer("ok"), new Error("overloaded"), answer("fine"));
  const chat = new Conversation(send.call, "m", 64);
  chat.say("first");
  const err = errorOf(() => chat.say("second"));
  assert.ok(err instanceof Error && err.message === "overloaded");
  assert.deepEqual(chat.history().map((m: any) => m.role), ["user", "assistant"]);
  chat.say("second again");
  assert.deepEqual(send.bodies[2].messages.map((m: any) => m.role), ["user", "assistant", "user"]);
});

test("e3 stop reason is reported and max_tokens marks the reply truncated", () => {
  const chat = new Conversation(new Script(answer("Complete."), answer("Cut o", "max_tokens"), answer("done", "stop_sequence")).call, "m", 64);
  const [first, second, third] = [chat.say("a"), chat.say("b"), chat.say("c")];
  assert.deepEqual([first.stopReason, first.truncated], ["end_turn", false]);
  assert.deepEqual([second.stopReason, second.truncated, second.text], ["max_tokens", true, "Cut o"]);
  assert.deepEqual([third.stopReason, third.truncated], ["stop_sequence", false]);
});

test("e4 system is a top-level field and stop sequences are passed on", () => {
  const send = new Script(answer("x"), answer("y"));
  new Conversation(send.call, "m", 8, "Be brief.", ["END"]).say("hi");
  assert.equal(send.bodies.length, 1);
  const body = send.bodies[0];
  assert.ok(body.system === "Be brief." && JSON.stringify(body.stop_sequences) === '["END"]');
  assert.ok(body.messages.every((m: any) => m.role !== "system"));
  new Conversation(send.call, "m", 8).say("hi");
  assert.ok(send.bodies.length === 2 && !("system" in send.bodies[1]) && !("stop_sequences" in send.bodies[1]));
});

test("e5 each request is a snapshot and history is a copy", () => {
  const send = new Script(answer("a"), answer("b"));
  const chat = new Conversation(send.call, "m", 8);
  chat.say("one");
  chat.say("two");
  assert.ok(send.bodies.length === 2 && send.bodies[0].messages.length === 1); // a later turn must not change an earlier request
  chat.history().push({ role: "user", content: "injected" });
  chat.history()[0].content = "changed";
  assert.ok(chat.history().length === 4 && chat.history()[0].content === "one");
});

test("e6 a blank turn is refused before anything is sent", () => {
  const send = new Script(answer("never used"));
  const chat = new Conversation(send.call, "m", 8);
  for (const blank of ["", "   ", "\n"]) {
    assert.ok(errorOf(() => chat.say(blank)) instanceof Error, JSON.stringify(blank));
  }
  assert.ok(send.bodies.length === 0 && chat.history().length === 0);
});
