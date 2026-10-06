/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */
import { logger } from "../logger.ts";

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
  // TODO 1 of 8 (finish this to pass e6): refuse numbers and a combination that make no sense.
  // Receives repos, lastsDays, presence and localFiles (already defaulted). Throws an Error when repos or lastsDays is below 1, or when
  // presence is "away" and localFiles is true (a cloud run starts from a fresh clone). Returns nothing otherwise.
  // Example: checkCounts(0, 1, "session", false) throws; checkCounts(1, 1, "away", true) throws; checkCounts(2, 30, "away", false) returns
}

function apiChoice(s: Situation): Choice | null {
  // TODO 2 of 8 (finish this to pass e5): the pick of an application on the Messages API, or null for the default own tool.
  // Receives the situation. Returns builtin-tool / provided-schema when builtin_covers is true, else mcp / remote-server when external_system
  // and remote_server are both true, else null (the caller then answers api-tool / own-schema-and-code).
  // Example: apiChoice({ builtin_covers: true, remote_server: true }) -> builtin-tool; apiChoice({ external_system: true }) -> null
  return null;
}

function priorityChoice(s: Situation): Choice | null {
  // TODO 3 of 8 (finish this to pass e1 and e2): the pick for a rule that must hold, an outside system or noisy work.
  // Receives the situation. The first match wins: guarantee gives hook / must-hold-every-time; external_system gives mcp / external-system;
  // noisy gives subagent / isolate-context; otherwise null. Whatever else is in the situation (timing, knowledge) does not matter here.
  // Example: priorityChoice({ guarantee: true, noisy: true }) -> hook; priorityChoice({ timing: "event" }) -> null
  return null;
}

function eventChoice(presence: string): Choice | null {
  // TODO 4 of 8 (finish this to pass e7): the pick for work that reacts to an event.
  // Receives presence. Returns routine / runs-unattended when presence is "away", else monitor / push-not-poll.
  // Example: eventChoice("session") -> monitor / push-not-poll
  return null;
}

function intervalChoice(s: Situation): Choice | null {
  // TODO 5 of 8 (finish this to pass e8): the pick for work that repeats every so often.
  // Receives the situation. Returns routine / runs-unattended when presence is "away"; else, when lasts_days is above LOOP_EXPIRY_DAYS,
  // desktop-task / durable-and-local if local_files is true and routine / runs-unattended if not; else loop / session-rhythm.
  // Example: intervalChoice({ lasts_days: 8, local_files: true }) -> desktop-task; intervalChoice({ lasts_days: 7 }) -> loop
  return null;
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
  // TODO 6 of 8 (finish this to pass e9): the pick for a preference of one person, or null.
  // Receives the personal value. Returns output-style / response-voice for "voice", status-line / personal-display for "display",
  // keybinding / personal-keys for "keys", and null for "none".
  // Example: personalChoice("keys") -> keybinding / personal-keys; personalChoice("none") -> null
  return null;
}

function knowledgeChoice(knowledge: string, pathScoped: boolean): Choice | null {
  // TODO 7 of 8 (finish this to pass e3): the pick for what Claude must be told, or null when nothing needs telling.
  // Receives knowledge and pathScoped. "convention" gives path-rule / scoped-convention when pathScoped, else claude-md / always-known;
  // "reference" gives skill / on-demand-reference; "procedure" gives skill / repeatable-procedure; anything else gives null. pathScoped
  // matters only for a convention.
  // Example: knowledgeChoice("reference", true) -> skill / on-demand-reference; knowledgeChoice("none", true) -> null
  return null;
}

function packageChoice(choice: Choice, repos: number): Choice {
  // TODO 8 of 8 (finish this to pass e4): a plugin carries a skill, a hook, a subagent or a server to a second repository.
  // Receives the pick and repos. Returns plugin / shared-setup when repos is 2 or more and the pick's mechanism is in CARRIED; otherwise
  // returns the pick unchanged (an instruction file, a path rule and a built-in tool are never packaged).
  // Example: packageChoice(pick("hook", "must-hold-every-time"), 2) -> plugin; packageChoice(pick("claude-md", "always-known"), 2) -> unchanged
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
