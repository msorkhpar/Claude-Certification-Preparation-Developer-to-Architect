// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { reviewChanges } from "./decompose.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const FILES = [{ path: "api.py", text: "def get(): ..." }, { path: "db.py", text: "def query(): ..." }, { path: "ui.py", text: "def show(): ..." }];
const SCRIPT: Record<string, [string[], string]> = {
  api: [["api: no auth"], "api calls db.query(id)"], db: [[], "db.query takes a name"], ui: [["ui: unused"], "ui shows rows"] };

// A scripted stand-in for the model reviewing one file (or one part of it).
const filePass = (path: string, _text: string, _part: number, _parts: number) => {
  const [findings, summary] = SCRIPT[path.split(".")[0]];
  return { findings, summary };
};
// The second look: it reads only the summaries, and finds what no single file shows.
const crossPass = (_summaries: unknown[]) => ["api passes id but db expects a name"];

// Each file is reviewed alone, then the cross pass reads their summaries.
const result = reviewChanges(FILES, filePass, crossPass) ?? {};

for (const [path, review] of Object.entries<any>(result.files ?? {})) console.log(path, "->", JSON.stringify(review.findings), "| parts:", review.parts);
console.log("cross findings:", JSON.stringify(result.cross));
console.log("failed:", JSON.stringify(result.failed), "| skipped:", JSON.stringify(result.skipped));
