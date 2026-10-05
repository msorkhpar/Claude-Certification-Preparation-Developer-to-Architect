import { logger } from "./logger.ts";
const log = logger("policy_resolver");
/**
 * Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.
 *
 * The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
 */
export type Value = boolean | number | string | string[];
export type Layers = Record<string, Record<string, Value>>;

export const LEVELS = ["managed", "command line", "local", "project", "user"];
const MANAGED_ONLY = new Set(["allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"]);
const EFFORT = ["low", "medium", "high", "xhigh", "max"];

/** The settings Claude Code applies, and a note for every entry that was ignored and why. */
export function effective(layers: Layers): [Record<string, Value>, string[]] {
  log.debug("effective input", layers);
  const managed = layers["managed"] ?? {};
  const lockRules = managed["allowManagedPermissionRulesOnly"] === true;
  const lockMcp = managed["allowManagedMcpServersOnly"] === true;
  const out: Record<string, Value> = {};
  const notes: string[] = [];
  const keys = [...new Set(Object.values(layers).flatMap((level) => Object.keys(level)))].sort();
  for (const key of keys) {
    let values: [string, Value][] = LEVELS.filter((level) => level in layers && key in layers[level]).map((level) => [level, layers[level][key]]);
    let reason: string | null = null;
    if (MANAGED_ONLY.has(key)) reason = "a managed-only key";
    else if ((key === "permissions.allow" || key === "permissions.deny") && lockRules) reason = "managed settings are the only source of permission rules";
    else if (key === "allowedMcpServers" && lockMcp) reason = "managed settings are the only source of the MCP allowlist";
    else if (key === "availableModels" && values.some(([level]) => level === "managed")) reason = "the managed list applies as it is";
    if (reason) {
      for (const [level] of values) if (level !== "managed") notes.push(`ignored ${key} from ${level}: ${reason}`);
      values = values.filter(([level]) => level === "managed");
    }
    if (values.length === 0) continue;
    out[key] = combine(key, values.map(([, v]) => v));
  }
  return [out, notes];
}

/** Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value. */
export function combine(key: string, values: Value[]): Value {
  if (Array.isArray(values[0])) {
    const merged: string[] = [];
    for (const value of values as string[][]) for (const item of value) if (!merged.includes(item)) merged.push(item);
    return merged;
  }
  if (key === "maxEffortLevel") return (values as string[]).reduce((a, b) => (EFFORT.indexOf(b) < EFFORT.indexOf(a) ? b : a));
  if (key === "disableClaudeAiConnectors") return values.some((v) => v === true);
  return values[0];
}

/** A managed list of available models refuses any other choice, whoever makes it. */
export function allowedModel(requested: string, settings: Record<string, Value>): string {
  const listed = settings["availableModels"] as string[] | undefined;
  return listed === undefined || listed.includes(requested) ? "allowed" : "refused (not in availableModels)";
}

function show(value: Value): string {
  if (Array.isArray(value)) return value.join(", ");
  return value === true ? "True" : value === false ? "False" : String(value);
}

function main(): void {
  const layers: Layers = {
    managed: { allowManagedPermissionRulesOnly: true, "permissions.deny": ["Read(./.env)"], "permissions.allow": ["Read(./docs/**)"], maxEffortLevel: "high", availableModels: ["sonnet", "haiku"], cleanupPeriodDays: 7, spinnerTipsEnabled: true },
    "command line": { cleanupPeriodDays: 14 },
    local: { "permissions.allow": ["Bash(npm test)"], allowManagedHooksOnly: false },
    project: { "permissions.allow": ["Bash(git status)"], "permissions.deny": ["Read(./secrets/**)"], maxEffortLevel: "xhigh", availableModels: ["opus"], disableClaudeAiConnectors: false, spinnerTipsEnabled: false },
    user: { "permissions.allow": ["Edit(./notes/**)"], disableClaudeAiConnectors: true, spinnerTipsEnabled: false },
  };
  const [settings, notes] = effective(layers);
  console.log("effective settings:");
  for (const [key, value] of Object.entries(settings)) console.log(`  ${key} = ${show(value)}`);
  console.log("notes:");
  for (const note of notes) console.log(`  ${note}`);
  for (const model of ["opus", "haiku"]) console.log(`model ${model}: ${allowedModel(model, settings)}`);
  const open: Layers = { project: { "permissions.allow": ["Bash(git status)"] }, user: { "permissions.allow": ["Edit(./notes/**)", "Bash(git status)"] } };
  const [merged] = effective(open);
  console.log(`without a lock the lists merge: ${show(merged["permissions.allow"])}`);
}

if (import.meta.main) main();
