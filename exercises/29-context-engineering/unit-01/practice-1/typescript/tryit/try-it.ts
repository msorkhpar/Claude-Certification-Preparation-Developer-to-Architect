// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { compact, countTokens } from "./context.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const user = (text: string) => ({ role: "user" as const, content: text });
const said = (text: string) => ({ role: "assistant" as const, content: [{ type: "text", text }] });
const call = (id: string, name: string) => ({ role: "assistant" as const, content: [{ type: "tool_use", id, name, input: {} }] });
const result = (id: string, text: string) => ({ role: "user" as const, content: [{ type: "tool_result", tool_use_id: id, content: text }] });

// A conversation of four turns, like the one the tests compact.
const messages = [user("first question"), said("first answer " + "x".repeat(80)),
  user("second question"), call("a1", "search"), result("a1", "R".repeat(300)), said("second answer"),
  user("third question"), said("third answer " + "y".repeat(60)),
  user("fourth question"), call("a2", "search"), result("a2", "S".repeat(100)), said("fourth answer")];

// A stand-in for the summariser model: it is told what to fold away and answers with a fixed text.
const summarise = (older: unknown[]) => {
  console.log("summariser asked to fold", older.length, "messages");
  return "The user asked three things.";
};

const budget = Math.floor(countTokens(messages) / 2);
const done = compact(messages, budget, summarise, 1) ?? [];

console.log("tokens before:", countTokens(messages), "| budget:", budget);
console.log("messages before and after:", messages.length, "->", done.length);
console.log("roles after:", done.map((m) => m.role).join(", "));
console.log("first message:", JSON.stringify(done[0]?.content));
