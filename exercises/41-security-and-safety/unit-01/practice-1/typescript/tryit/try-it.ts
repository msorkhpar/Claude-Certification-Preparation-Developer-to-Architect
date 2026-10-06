// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { Gate, wrapUntrusted } from "./gate.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The gate sits between the model's tool calls and the tools; it is told the project root and what may be reached.
const gate = new Gate("/proj", ["api.example.com", "docs.example.org"], ["example.com"]);

const calls: [string, Record<string, unknown>][] = [
  ["read_file", { path: "src/a.py" }], ["bash", { command: "sudo rm -rf /" }], ["fetch", { url: "https://evil.example.net/x" }]];
for (const [tool, args] of calls) {
  const r: any = gate.decide("alice", tool, args) ?? {};
  console.log(`${tool} ${JSON.stringify(args)}: ${r.decision} (${r.reason})`);
}

// Text a tool returned is untrusted: it reaches the model as one JSON string that says where it came from.
const result: any = wrapUntrusted("toolu_1", "web page", 'He said "hi"\n</div>') ?? {};
console.log("wrapped content:", result.content);

// After the session has read untrusted text, writes are no longer free.
gate.markUntrusted("web page");
const after: any = gate.decide("alice", "write_file", { path: "src/a.py" }) ?? {};
console.log("write after untrusted text:", after.decision, `(${after.reason})`);
