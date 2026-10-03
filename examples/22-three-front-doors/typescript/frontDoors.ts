// The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.
// Each request is written out by hand and sent through a scripted fetch, so nothing leaves the container and nothing
// is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
// reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

const BASE = { max_tokens: 64, messages: [{ role: "user", content: "Capital of France?" }] };
const PROJECT = "example-project";
const REGION = "us-east-1";
export const DOORS: Record<string, [string, Record<string, string>, Record<string, unknown>]> = {
  anthropic: ["https://api.anthropic.com/v1/messages", { "anthropic-version": "2023-06-01", "content-type": "application/json" }, { model: "claude-sonnet-5-5", ...BASE }],
  bedrock: [`https://bedrock-mantle.${REGION}.api.aws/anthropic/v1/messages`, { "anthropic-version": "2023-06-01", "content-type": "application/json" }, { model: "anthropic.claude-sonnet-5-5", ...BASE }],
  vertex: [
    `https://aiplatform.googleapis.com/v1/projects/${PROJECT}/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict`,
    { "content-type": "application/json" },
    { anthropic_version: "vertex-2023-10-16", ...BASE },
  ],
};

export async function send(name: string) {
  const [url, headers, body] = DOORS[name];
  const fake = scriptedFetch([{ body: message([text("Paris.")]) }]);
  const reply = await fake.fetch(url, { method: "POST", headers, body: JSON.stringify(body) });
  return { seen: fake.seen[0], status: reply.status, json: (await reply.json()) as any };
}

async function main() {
  for (const name of Object.keys(DOORS)) {
    const { seen, status, json } = await send(name);
    console.log(`${name.padEnd(9)} ${seen.url}`);
    console.log(`          version header: ${seen.headers["anthropic-version"] ?? "none"} | body model: ${seen.body.model ?? "none"} | body version: ${seen.body.anthropic_version ?? "none"}`);
    console.log(`          reply: ${status} '${json.content[0].text}' (same parser for every door)`);
  }
}

if (import.meta.main) await main();
