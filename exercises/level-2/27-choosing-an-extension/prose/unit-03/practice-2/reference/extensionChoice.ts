/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
import { logger } from "./logger.ts";

const log = logger("extension_choice");

const SURFACES = ["code", "api"];
const KNOWLEDGE = ["none", "convention", "reference", "procedure"];
const CARRIED = ["skill", "hook", "subagent", "mcp"]; // what a plugin can bundle
const TIMINGS = ["none", "interval", "event", "condition", "background"];
const PRESENCE = ["session", "pipeline", "away"];
const PERSONAL = ["none", "voice", "display", "keys"];
const LOOP_EXPIRY_DAYS = 7; // a recurring task of a session expires after seven days

type Choice = { mechanism: string; reason: string };
type Situation = Record<string, any>;

function pick(mechanism: string, reason: string): Choice {
  return { mechanism, reason };
}

function checkCounts(repos: number, lastsDays: number, presence: string, localFiles: boolean): void {
  if (repos < 1 || lastsDays < 1 || (presence === "away" && localFiles)) throw new Error("repos and lasts_days start at 1, and a cloud run starts from a fresh clone and sees no local files");
}

function apiChoice(s: Situation): Choice | null {
  if (s.builtin_covers ?? false) return pick("builtin-tool", "provided-schema");
  if ((s.external_system ?? false) && (s.remote_server ?? false)) return pick("mcp", "remote-server");
  return null;
}

function priorityChoice(s: Situation): Choice | null {
  if (s.guarantee ?? false) return pick("hook", "must-hold-every-time");
  if (s.external_system ?? false) return pick("mcp", "external-system");
  if (s.noisy ?? false) return pick("subagent", "isolate-context");
  return null;
}

function eventChoice(presence: string): Choice | null {
  return presence === "away" ? pick("routine", "runs-unattended") : pick("monitor", "push-not-poll");
}

function intervalChoice(s: Situation): Choice | null {
  if ((s.presence ?? "session") === "away") return pick("routine", "runs-unattended");
  if ((s.lasts_days ?? 1) > LOOP_EXPIRY_DAYS) return (s.local_files ?? false) ? pick("desktop-task", "durable-and-local") : pick("routine", "runs-unattended");
  return pick("loop", "session-rhythm");
}

/** The pick for work that runs without a person, on a rhythm or on an event, or null when the situation has none of these. */
function rhythm(s: Situation): Choice | null {
  const timing = s.timing ?? "none";
  const presence = s.presence ?? "session";
  if (presence === "pipeline") return pick("headless-ci", "no-person-present");
  if (timing === "condition") return pick("goal", "until-condition-holds");
  if (timing === "background") return pick("background-task", "work-while-it-runs");
  if (timing === "event") return eventChoice(presence);
  if (timing === "interval") return intervalChoice(s);
  return null;
}

function personalChoice(personal: string): Choice | null {
  if (personal === "voice") return pick("output-style", "response-voice");
  if (personal === "display") return pick("status-line", "personal-display");
  if (personal === "keys") return pick("keybinding", "personal-keys");
  return null;
}

function knowledgeChoice(knowledge: string, pathScoped: boolean): Choice | null {
  if (knowledge === "convention") return pathScoped ? pick("path-rule", "scoped-convention") : pick("claude-md", "always-known");
  if (knowledge === "reference") return pick("skill", "on-demand-reference");
  if (knowledge === "procedure") return pick("skill", "repeatable-procedure");
  return null;
}

function packageChoice(choice: Choice, repos: number): Choice {
  if (repos >= 2 && CARRIED.includes(choice.mechanism)) return pick("plugin", "shared-setup");
  return choice;
}

export function choose(s: Situation): Choice {
  log.debug("choose input", s);
  const surface = s.surface ?? "code";
  const knowledge = s.knowledge ?? "none";
  const repos = s.repos ?? 1;
  if (!SURFACES.includes(surface)) throw new Error(`unknown surface: ${surface}`);
  if (!KNOWLEDGE.includes(knowledge)) throw new Error(`unknown knowledge kind: ${knowledge}`);
  if (!TIMINGS.includes(s.timing ?? "none")) throw new Error(`unknown timing: ${s.timing}`);
  if (!PRESENCE.includes(s.presence ?? "session")) throw new Error(`unknown presence: ${s.presence}`);
  if (!PERSONAL.includes(s.personal ?? "none")) throw new Error(`unknown personal preference: ${s.personal}`);
  checkCounts(repos, s.lasts_days ?? 1, s.presence ?? "session", s.local_files ?? false);
  if (surface === "api") return apiChoice(s) ?? pick("api-tool", "own-schema-and-code");
  const choice =
    priorityChoice(s) ??
    rhythm(s) ??
    personalChoice(s.personal ?? "none") ??
    knowledgeChoice(knowledge, s.path_scoped ?? false) ??
    pick("builtin-tool", "built-in-covers");
  return packageChoice(choice, repos);
}
