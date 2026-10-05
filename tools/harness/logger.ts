// Provenance: studyforge release/claude-cert-support, docs/authoring/run-output/logger.ts (copied unchanged below this line)
// A tiny logger, because Node has no standard one. Use it like the standard loggers of the other languages:
//   const log = logger("prompt-builder");  log.debug("input", spec);
// It writes `[log:<test>] LEVEL name: message` lines to stdout, naming the test that was running, so the report can show
// each line under its test. Imported by the practice's own file, it registers one test hook; the file still runs outside the tests.
import { beforeEach } from "node:test";
let current = "";
beforeEach((t) => { current = t.name; });

export function logger(name: string) {
  const write = (level: string) => (...parts: unknown[]) => {
    const text = parts.map((part) => (typeof part === "string" ? part : JSON.stringify(part))).join(" ");
    process.stdout.write(`[log:${current}] ${level} ${name}: ${text}\n`);
  };
  return { debug: write("DEBUG"), info: write("INFO"), warn: write("WARN"), error: write("ERROR") };
}
