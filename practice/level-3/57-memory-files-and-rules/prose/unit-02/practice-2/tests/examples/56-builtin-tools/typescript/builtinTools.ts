// The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
// on which platform, and which permission rule covers which tool.
//
// A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
// Read, Write, Edit, Bash, Grep and Glob.
import { logger } from "./example-logger.ts";
const log = logger("builtin_tools");
const SEARCH_TOOLS = ["Grep", "Glob"];
const BASE_TOOLS = ["Read", "Write", "Edit", "Bash"];
const RULE_COVERS: Record<string, string[]> = { Read: ["Read", "Grep", "Glob"], Edit: ["Edit", "Write"], Bash: ["Bash"] }; // a Write(path) rule is never matched

type EditResult = { ok: false; error: string } | { ok: true; text: string; replaced: number };

/** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
export function edit(text: string, old: string, replacement: string, replaceAll = false): EditResult {
  const count = text.split(old).length - 1;
  if (count === 0) return { ok: false, error: "old_string not found" };
  if (count > 1 && !replaceAll) return { ok: false, error: `old_string appears ${count} times` };
  return { ok: true, text: replaceAll ? text.split(old).join(replacement) : text.replace(old, () => replacement), replaced: replaceAll ? count : 1 };
}

const occurrences = (text: string, part: string) => text.split(part).length - 1;

/** What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
 * and only when no unique anchor exists, read the file and write it back whole. */
export function planEdit(text: string, old: string, every = false, anchors: string[] = []): [string, string | null] {
  const count = occurrences(text, old);
  if (count === 0) return ["read_again", null];
  if (count === 1) return ["edit", old];
  if (every) return ["replace_all", old];
  for (const anchor of anchors) if (anchor.includes(old) && occurrences(text, anchor) === 1) return ["edit", anchor];
  return ["read_write", null];
}

/** The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
 * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed. */
export function toolSet(platform: string, tools: string[] | null = null, allowedTools: string[] = [], disallowedTools: string[] = []): string[] {
  let have: string[];
  if (tools !== null) have = tools.filter((t) => [...BASE_TOOLS, ...SEARCH_TOOLS].includes(t));
  else {
    have = [...BASE_TOOLS];
    if (platform === "windows" || allowedTools.some((t) => SEARCH_TOOLS.includes(t)) || disallowedTools.includes("Bash")) have.push(...SEARCH_TOOLS);
  }
  return have.filter((t) => !disallowedTools.includes(t));
}

/** The tools that a permission rule such as Read(secrets/**) applies to. */
export function coveredBy(rule: string): string[] {
  return [...(RULE_COVERS[rule.split("(")[0]] ?? [])];
}

/** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
export function ruleTool(tool: string): string {
  for (const [name, tools] of Object.entries(RULE_COVERS)) if (tools.includes(tool)) return name;
  return tool;
}

function main() {
  const text = "def a():\n    return 1\n\ndef b():\n    return 1\n";
  for (const [label, old, every] of [["unique", "def a():", false], ["twice", "    return 1", false], ["twice, every", "    return 1", true], ["absent", "def c():", false]] as const) {
    const r = edit(text, old, "X", every);
    console.log(`edit ${label}:`, r.ok ? `replaced ${r.replaced}` : r.error);
  }
  const anchors = ["def b():\n    return 1"];
  const show = (p: [string, string | null]) => `(${p[0] === null ? "None" : `'${p[0]}'`}, ${p[1] === null ? "None" : JSON.stringify(p[1]).replace(/^"|"$/g, "'")})`;
  console.log("plan, unique:", show(planEdit(text, "def a():")));
  console.log("plan, twice with an anchor:", show(planEdit(text, "    return 1", false, anchors)));
  console.log("plan, twice, every one:", show(planEdit(text, "    return 1", true)));
  console.log("plan, twice, no unique anchor:", show(planEdit(text, "    return 1", false, ["return 1"])));
  for (const platform of ["linux", "windows"]) console.log(`${platform}, default:`, toolSet(platform).join(", "));
  console.log("linux, allowedTools Grep:", toolSet("linux", null, ["Grep"]).join(", "));
  console.log("linux, tools Read Grep Glob:", toolSet("linux", ["Read", "Grep", "Glob"]).join(", "));
  console.log("linux, Bash removed:", toolSet("linux", null, [], ["Bash"]).join(", "));
  console.log("rules are written under:", ["Grep", "Glob", "Write", "Bash"].map((t) => `${t} as ${ruleTool(t)}`).join(", "));
  for (const rule of ["Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)"]) console.log(`${rule} covers:`, coveredBy(rule).join(", ") || "nothing");
}

if (import.meta.main) main();
