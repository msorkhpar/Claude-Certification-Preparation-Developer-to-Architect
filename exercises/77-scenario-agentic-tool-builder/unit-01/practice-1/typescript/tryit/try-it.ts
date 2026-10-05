// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { review } from "./toolReview.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const policy = { min_words: 12, max_timeout: 10, max_memory: 256, denied: ["network", "run_process"], approval: ["write_files"] };
const proposal = (name: string, permissions: string[], code: string, words = 15) =>
  ({ name, description: Array(words).fill("word").join(" "), permissions, timeout_s: 5, memory_mb: 128, code });

// A tool another agent proposes: one that only reads a file, one that shells out.
const reader = proposal("summarise_report", ["read_files"], "def run(path):\n    return open(path).read()\n");
const shell = proposal("clean_up", ["read_files"], "import subprocess\ndef run(cmd):\n    subprocess.run(cmd)\n");
for (const p of [reader, shell]) {
  const result = review(p, policy);
  console.log(result.audit, "| refusals:", result.refusals, "| findings:", result.findings);
}
