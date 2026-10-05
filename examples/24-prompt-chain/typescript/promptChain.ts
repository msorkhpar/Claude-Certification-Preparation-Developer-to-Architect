// A two-step prompt chain with versioned templates, against a scripted model.
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("prompt_chain");

export const MODEL = "claude-sonnet-5-5";

export const TEMPLATES: Record<string, { system: string; user: string }> = {
  "extract-quotes@2": {
    system: "You answer questions about company documents and use only what the documents say.",
    user: "{{documents}}\n\nFirst quote the passages that bear on the question, each in <quote> tags inside <quotes>. " +
      "If nothing bears on it, write <quotes></quotes>.\n\n<question>{{question}}</question>",
  },
  "answer-from-quotes@1": {
    system: "You answer from quoted evidence. If the quotes do not settle the question, say what is missing.",
    user: "{{quotes}}\n\nAnswer the question in one or two sentences, and say which quote supports each claim.\n\n<question>{{question}}</question>",
  },
};

export const DOCUMENTS: Array<[string, string]> = [
  ["travel-policy.txt", "Flights above 400 dollars need approval from a manager before booking. Economy class is the default for flights under six hours."],
  ["expenses-faq.txt", "Meals are reimbursed up to 60 dollars a day when travelling. Receipts are required for every claim over 25 dollars."],
];
export const QUESTION = "Does a 450 dollar economy flight need approval?";

/** Fill {{name}} placeholders in one pass; a value is data and is never read as a template. */
export function render(template: string, values: Record<string, string>): string {
  const wanted = new Set([...template.matchAll(/{{(\w+)}}/g)].map((m) => m[1]));
  const missing = [...wanted].filter((name) => !(name in values)).sort();
  if (missing.length > 0) throw new Error(`unfilled variables: ${JSON.stringify(missing)}`);
  return template.replace(/{{(\w+)}}/g, (_, name: string) => values[name]);
}

export function documentsBlock(documents: Array<[string, string]>): string {
  const body = documents.map(([name, content], i) => `<document index="${i + 1}">\n<source>${name}</source>\n<document_content>\n${content}\n</document_content>\n</document>\n`).join("");
  return `<documents>\n${body}</documents>`;
}

export async function step(client: Anthropic, version: string, values: Record<string, string>) {
  const template = TEMPLATES[version];
  return client.messages.create({ model: MODEL, max_tokens: 500, system: template.system, messages: [{ role: "user", content: render(template.user, values) }] });
}

export const REPLIES = () => [
  { body: message([text("<quotes>\n<quote>Flights above 400 dollars need approval from a manager before booking.</quote>\n</quotes>")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) },
  { body: message([text("Yes: at 450 dollars it is above the 400 dollar limit, so it needs a manager's approval first (quote 1).")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const { fake, client } = clientFor(REPLIES());
  const first = await step(client, "extract-quotes@2", { documents: documentsBlock(DOCUMENTS), question: QUESTION });
  const quotes = (first.content[0] as { text: string }).text;
  const second = await step(client, "answer-from-quotes@1", { quotes, question: QUESTION });
  const [one, two] = fake.seen.map((r) => r.body.messages[0].content as string);
  console.log("templates used:", JSON.stringify(["extract-quotes@2", "answer-from-quotes@1"]));
  console.log("step 1: documents come before the question:", one.indexOf("<documents>") < one.indexOf("<question>"));
  console.log("step 1: the prompt ends with the question:", one.trimEnd().endsWith("</question>"));
  console.log("step 2: the full documents are not resent:", !two.includes("<documents>"), `(${one.length} characters then ${two.length})`);
  console.log("last message of each request is a user turn (no prefill):", JSON.stringify(fake.seen.map((r) => r.body.messages.at(-1).role)));
  const sampling = ["temperature", "top_p", "top_k"].filter((k) => k in fake.seen[0].body);
  console.log("sampling parameters sent:", sampling.length ? JSON.stringify(sampling) : "none");
  console.log("system prompts differ per step:", fake.seen[0].body.system !== fake.seen[1].body.system);
  console.log("data is not read as a template:", render(TEMPLATES["answer-from-quotes@1"].user, { quotes: "{{question}} stays", question: "Q?" }).split("{{question}} stays").length - 1 === 1);
  try {
    render(TEMPLATES["answer-from-quotes@1"].user, { quotes });
  } catch (err) {
    console.log("a missing variable is an error:", (err as Error).message);
  }
  console.log("step 1 output:", quotes.replaceAll("\n", " "));
  console.log("step 2 output:", (second.content[0] as { text: string }).text);
}

if (import.meta.main) await main();
