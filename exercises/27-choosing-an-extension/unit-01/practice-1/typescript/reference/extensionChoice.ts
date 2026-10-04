/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

const SURFACES = ["code", "api"];
const KNOWLEDGE = ["none", "convention", "reference", "procedure"];
const CARRIED = ["skill", "hook", "subagent", "mcp"]; // what a plugin can bundle

function pick(mechanism: string, reason: string) {
  return { mechanism, reason };
}

export function choose(s: Record<string, any>): { mechanism: string; reason: string } {
  const surface = s.surface ?? "code";
  const knowledge = s.knowledge ?? "none";
  const repos = s.repos ?? 1;
  if (!SURFACES.includes(surface)) throw new Error(`unknown surface: ${surface}`);
  if (!KNOWLEDGE.includes(knowledge)) throw new Error(`unknown knowledge kind: ${knowledge}`);
  if (repos < 1) throw new Error("repos starts at 1");
  if (surface === "api") {
    if (s.builtin_covers ?? false) return pick("builtin-tool", "provided-schema");
    if ((s.external_system ?? false) && (s.remote_server ?? false)) return pick("mcp", "remote-server");
    return pick("api-tool", "own-schema-and-code");
  }
  let choice;
  if (s.guarantee ?? false) choice = pick("hook", "must-hold-every-time");
  else if (s.external_system ?? false) choice = pick("mcp", "external-system");
  else if (s.noisy ?? false) choice = pick("subagent", "isolate-context");
  else if (knowledge === "convention") choice = (s.path_scoped ?? false) ? pick("path-rule", "scoped-convention") : pick("claude-md", "always-known");
  else if (knowledge === "reference") choice = pick("skill", "on-demand-reference");
  else if (knowledge === "procedure") choice = pick("skill", "repeatable-procedure");
  else choice = pick("builtin-tool", "built-in-covers");
  if (repos >= 2 && CARRIED.includes(choice.mechanism)) return pick("plugin", "shared-setup");
  return choice;
}
