/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

const SURFACES = ["code", "api"];
const KNOWLEDGE = ["none", "convention", "reference", "procedure"];
const CARRIED = ["skill", "hook", "subagent", "mcp"]; // what a plugin can bundle
const TIMINGS = ["none", "interval", "event", "condition", "background"];
const PRESENCE = ["session", "pipeline", "away"];
const PERSONAL = ["none", "voice", "display", "keys"];
const LOOP_EXPIRY_DAYS = 7; // a recurring task of a session expires after seven days

function pick(mechanism: string, reason: string) {
  return { mechanism, reason };
}

/** The mechanism for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
function rhythm(s: Record<string, any>): { mechanism: string; reason: string } | null {
  const timing = s.timing ?? "none";
  const presence = s.presence ?? "session";
  if (presence === "pipeline") return pick("headless-ci", "no-person-present");
  if (timing === "condition") return pick("goal", "until-condition-holds");
  if (timing === "background") return pick("background-task", "work-while-it-runs");
  if (timing === "event") return presence === "away" ? pick("routine", "runs-unattended") : pick("monitor", "push-not-poll");
  if (timing === "interval") {
    if (presence === "away") return pick("routine", "runs-unattended");
    if ((s.lasts_days ?? 1) > LOOP_EXPIRY_DAYS) return (s.local_files ?? false) ? pick("desktop-task", "durable-and-local") : pick("routine", "runs-unattended");
    return pick("loop", "session-rhythm");
  }
  return null;
}

export function choose(s: Record<string, any>): { mechanism: string; reason: string } {
  const surface = s.surface ?? "code";
  const knowledge = s.knowledge ?? "none";
  const repos = s.repos ?? 1;
  if (!SURFACES.includes(surface)) throw new Error(`unknown surface: ${surface}`);
  if (!KNOWLEDGE.includes(knowledge)) throw new Error(`unknown knowledge kind: ${knowledge}`);
  if (repos < 1) throw new Error("repos starts at 1");
  if (!TIMINGS.includes(s.timing ?? "none")) throw new Error(`unknown timing: ${s.timing}`);
  if (!PRESENCE.includes(s.presence ?? "session")) throw new Error(`unknown presence: ${s.presence}`);
  if (!PERSONAL.includes(s.personal ?? "none")) throw new Error(`unknown personal preference: ${s.personal}`);
  if ((s.lasts_days ?? 1) < 1) throw new Error("lasts_days starts at 1");
  if ((s.presence ?? "session") === "away" && (s.local_files ?? false)) throw new Error("a cloud run starts from a fresh clone and sees no local files");
  if (surface === "api") {
    if (s.builtin_covers ?? false) return pick("builtin-tool", "provided-schema");
    if ((s.external_system ?? false) && (s.remote_server ?? false)) return pick("mcp", "remote-server");
    return pick("api-tool", "own-schema-and-code");
  }
  let choice;
  if (s.guarantee ?? false) choice = pick("hook", "must-hold-every-time");
  else if (s.external_system ?? false) choice = pick("mcp", "external-system");
  else if (s.noisy ?? false) choice = pick("subagent", "isolate-context");
  else if (rhythm(s) !== null) choice = rhythm(s)!;
  else if ((s.personal ?? "none") === "voice") choice = pick("output-style", "response-voice");
  else if ((s.personal ?? "none") === "display") choice = pick("status-line", "personal-display");
  else if ((s.personal ?? "none") === "keys") choice = pick("keybinding", "personal-keys");
  else if (knowledge === "convention") choice = (s.path_scoped ?? false) ? pick("path-rule", "scoped-convention") : pick("claude-md", "always-known");
  else if (knowledge === "reference") choice = pick("skill", "on-demand-reference");
  else if (knowledge === "procedure") choice = pick("skill", "repeatable-procedure");
  else choice = pick("builtin-tool", "built-in-covers");
  if (repos >= 2 && CARRIED.includes(choice.mechanism)) return pick("plugin", "shared-setup");
  return choice;
}
