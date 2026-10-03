// A conversation the client keeps: the API is stateless, so every request carries the whole history.
// Three turns through the real SDK against a scripted fetch. The replies are illustrative,
// hand-written Messages responses (claude-sonnet-5-5), not captures.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

const MODEL = "claude-sonnet-5-5";
const SYSTEM = "You answer in one short sentence.";

export const REPLIES = [
  { body: message([text("Paris.")], "end_turn", { input_tokens: 18, output_tokens: 4 }) },
  { body: message([text("It has been the capital since")], "max_tokens", { input_tokens: 30, output_tokens: 6 }) },
  { body: { ...message([text("Seine")], "stop_sequence", { input_tokens: 41, output_tokens: 2 }), stop_sequence: "END" } },
];
export const QUESTIONS = ["Capital of France?", "Since when?", "Name its river. End with END."];

// Keep the history in an array and send all of it every time.
export async function run(client: Anthropic, questions: string[]) {
  const history: Anthropic.MessageParam[] = [];
  const totals = { input: 0, output: 0 };
  const replies: Anthropic.Message[] = [];
  for (const question of questions) {
    history.push({ role: "user", content: question });
    const reply = await client.messages.create({
      model: MODEL,
      max_tokens: 16,
      system: SYSTEM,
      messages: history,
      stop_sequences: ["END"],
    });
    history.push({ role: "assistant", content: reply.content });
    totals.input += reply.usage.input_tokens;
    totals.output += reply.usage.output_tokens;
    replies.push(reply);
  }
  return { replies, totals };
}

async function main() {
  const fake = scriptedFetch(REPLIES);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const { replies, totals } = await run(client, QUESTIONS);
  replies.forEach((reply, i) => {
    const sent = fake.seen[i].body.messages;
    const roles = sent.map((m: any) => m.role).join(", ");
    const stop = reply.stop_sequence ? ` '${reply.stop_sequence}'` : "";
    console.log(`turn ${i + 1}: sent ${sent.length} message(s) [${roles}] -> ${reply.stop_reason}${stop}, '${(reply.content[0] as any).text}'`);
  });
  console.log("totals:", `{ input: ${totals.input}, output: ${totals.output} }`);
  const roles = [...new Set(fake.seen.flatMap((r) => r.body.messages.map((m: any) => m.role)))].sort();
  console.log("system is a top-level field:", fake.seen.every((r) => r.body.system === SYSTEM), "| roles ever used in messages:", roles);
}

if (import.meta.main) await main();
