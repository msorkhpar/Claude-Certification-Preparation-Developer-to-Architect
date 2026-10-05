// Provenance: studyforge release/claude-cert-support, docs/authoring/run-output/logger.ts; two changes: silent when no test is running (current is empty), and node:test is imported only under `node --test`
// A tiny logger, because Node has no standard one. Use it like the standard loggers of the other languages:
//   const log = logger("prompt-builder");  log.debug("input", spec);
// It writes `[log:<test>] LEVEL name: message` lines to stdout, naming the test that was running, so the report can show
// each line under its test. Imported by the practice's own file, it registers one test hook; the file still runs outside the tests.
let current = "";
// node:test is loaded only inside a test run (`node --test` sets NODE_TEST_CONTEXT in the file's process); importing it in a plain
// `node file.ts` run would print an empty test summary, which would change what a program prints.
if (process.env.NODE_TEST_CONTEXT) { const { beforeEach } = await import("node:test"); beforeEach((t) => { current = t.name; }); }

export function logger(name: string) {
  const write = (level: string) => (...parts: unknown[]) => {
    if (current === "") return;   // outside a test (node file.ts) nothing is written, so a program's printed output is unchanged
    const text = parts.map((part) => (typeof part === "string" ? part : JSON.stringify(part))).join(" ");
    process.stdout.write(`[log:${current}] ${level} ${name}: ${text}\n`);
  };
  return { debug: write("DEBUG"), info: write("INFO"), warn: write("WARN"), error: write("ERROR") };
}
