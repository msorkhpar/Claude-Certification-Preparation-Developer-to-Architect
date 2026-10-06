import { test } from "node:test";
import assert from "node:assert/strict";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { CreateMessageRequestSchema, ElicitRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { buildServer } from "./mrtrHttp.ts";

async function connected(capabilities: Record<string, object>, onClient?: (c: Client) => void) {
  const [clientSide, serverSide] = InMemoryTransport.createLinkedPair();
  await buildServer().connect(serverSide);
  const client = new Client({ name: "test", version: "1.0.0" }, { capabilities });
  onClient?.(client);
  await client.connect(clientSide);
  return client;
}
const deploy = (c: Client, env: string): Promise<any> => c.callTool({ name: "deploy", arguments: { service: "api", env } });
const askWith = (action: "accept" | "decline", confirm?: boolean) => (c: Client) => c.setRequestHandler(ElicitRequestSchema, async () => ({ action, ...(action === "accept" ? { content: { confirm: !!confirm } } : {}) }));

test("a staging deploy asks nobody", async () => {
  assert.equal((await deploy(await connected({}), "staging")).content[0].text, "Deployed api to staging");
});

test("a production deploy follows the persons answer", async () => {
  for (const [action, confirm, expected] of [["accept", true, "Deployed api to production"], ["accept", false, "Deployment cancelled"], ["decline", false, "Deployment cancelled"]] as const) {
    assert.equal((await deploy(await connected({ elicitation: {} }, askWith(action, confirm)), "production")).content[0].text, expected, `${action} ${confirm}`);
  }
});

test("the release notes come from the clients model", async () => {
  const c = await connected({ sampling: {} }, (client) => client.setRequestHandler(CreateMessageRequestSchema, async () => ({ role: "assistant" as const, content: { type: "text" as const, text: "Faster." }, model: "scripted" })));
  assert.equal(((await c.callTool({ name: "release_notes", arguments: { service: "api" } })) as any).content[0].text, "api: Faster.");
});

test("a client that cannot be asked gets a tool error and not a question", async () => {
  const result = await deploy(await connected({}), "production");
  assert.ok(result.isError === true && result.content[0].text.includes("cannot be asked"));
});
