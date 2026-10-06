// Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "../logger.ts";
const log = logger("subagents");

const READ_ONLY = ["Read", "Grep", "Glob"];
const SPAWN_TOOLS = ["Agent", "Task"]; // current and old name of the tool that starts a subagent

type Spec = { description?: string; prompt?: string; tools?: string[] | null; model?: string };

function validName(name: string): boolean {
  // TODO 1 of 9 (unlocks e2): is this a legal agent name?
  // Receives a name. Returns true for lower-case letters, digits and hyphens that start with a letter, false otherwise.
  // Example: validName("ok-name-2") -> true, validName("under_score") -> false
  return true;
}

function complete(spec: Spec): boolean {
  // TODO 2 of 9 (unlocks e2): does the spec have a description and a prompt?
  // Receives a spec. Returns true only when both description and prompt are present and not blank.
  // Example: complete({ prompt: "p" }) -> false, complete({ description: "d", prompt: "p" }) -> true
  return true;
}

function agentTools(listed: string[] | null | undefined): string[] {
  // TODO 3 of 9 (unlocks e1): the tool list of one subagent.
  // Receives the spec's tools (an array, or null/undefined when it gave none). Returns a copy of READ_ONLY for none, else the array without "Agent"
  // (an empty array stays empty).
  // Example: agentTools(["Read", "Agent", "Bash"]) -> ["Read", "Bash"], agentTools(undefined) -> ["Read", "Grep", "Glob"]
  return [];
}

function limitEnv(maxConcurrent: number): Record<string, string> {
  // TODO 4 of 9 (unlocks e3): the environment that bounds the team.
  // Receives the number of subagents allowed at once. Returns an object with CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH as "1" and
  // CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS as the number written as a string.
  // Example: limitEnv(3) -> { CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS: "3" }
  return {};
}

export function buildOptions(agents: Record<string, Spec>, cwd: string, cliPath?: string, maxBudgetUsd = 2.0, maxTurns = 20, maxConcurrent = 5): any {
  log.debug("buildOptions input", agents);
  const definitions: Record<string, any> = {};
  for (const [name, spec] of Object.entries(agents)) {
    if (!validName(name)) throw new Error(`bad agent name: ${name}`);
    if (!complete(spec)) throw new Error(`agent ${name} needs a description and a prompt`);
    const tools = agentTools(spec.tools);
    definitions[name] = { description: spec.description, prompt: spec.prompt, tools, model: spec.model || "inherit" };
  }
  return {
    agents: definitions, allowedTools: ["Agent", ...READ_ONLY], maxTurns, maxBudgetUsd, cwd,
    env: { ...process.env, ...limitEnv(maxConcurrent) },
    permissionMode: "default", settingSources: [],
    ...(cliPath ? { pathToClaudeCodeExecutable: cliPath } : {}),
  };
}

function blocks(message: any): any[] {
  const content = message?.message?.content;
  return Array.isArray(content) ? content : [];
}

function isSpawn(block: any): boolean {
  // TODO 5 of 9 (unlocks e4): does this content block start a subagent?
  // Receives a block. Returns true for a tool_use block whose name is in SPAWN_TOOLS, false for anything else.
  // Example: { type: "tool_use", name: "Task" } -> true, { type: "tool_use", name: "Read" } -> false, { type: "text" } -> false
  return false;
}

export function spawned(messages: any[]): Array<{ id: string; subagent_type: string; description: string; prompt: string }> {
  const found: Array<{ id: string; subagent_type: string; description: string; prompt: string }> = [];
  for (const message of messages) {
    for (const block of blocks(message)) {
      if (isSpawn(block)) {
        const input = block.input ?? {};
        found.push({ id: block.id, subagent_type: input.subagent_type || "general-purpose", description: input.description ?? "", prompt: input.prompt ?? "" });
      }
    }
  }
  return found;
}

function noteTools(group: { tools: string[] }, inner: any[]): void {
  // TODO 6 of 9 (unlocks e5): record the tools used in one inner message.
  // Receives a group (with a tools array) and the blocks of one message. Pushes the name of each tool_use block, once, in order.
  // Example: blocks named Read, Read, Grep added to a group whose tools are [] -> ["Read", "Grep"]
}

export function bySubagent(messages: any[]): Record<string, { subagent_type: string; messages: number; tools: string[] }> {
  const groups: Record<string, { subagent_type: string; messages: number; tools: string[] }> = {};
  for (const call of spawned(messages)) groups[call.id] = { subagent_type: call.subagent_type, messages: 0, tools: [] };
  for (const message of messages) {
    const parent = message?.parent_tool_use_id;
    if (parent && groups[parent]) {
      groups[parent].messages += 1;
      noteTools(groups[parent], blocks(message));
    }
  }
  return groups;
}

function section(title: string, items?: string[]): string[] {
  // TODO 7 of 9 (unlocks e6): the lines of one section of a brief.
  // Receives a title and an array of items (or undefined). Returns [] when no item is left after trimming and dropping blanks, else
  // `Title:` followed by one `- item` line per item.
  // Example: section("Known", ["a", " "]) -> ["Known:", "- a"], section("Files", undefined) -> []
  return [];
}

export function makeBrief(task: string, files?: string[], facts?: string[], output?: string): string {
  if (!String(task ?? "").trim()) throw new Error("a brief needs a task");
  const lines = [`Task: ${task.trim()}`];
  for (const [title, items] of [["Files", files], ["Known", facts]] as const) {
    lines.push(...section(title, items));
  }
  if (output && output.trim()) lines.push(`Return: ${output.trim()}`);
  return lines.join("\n");
}

function sourceOf(url?: string, title?: string, page?: number): Record<string, unknown> {
  // TODO 8 of 9 (unlocks e7): the source of a finding.
  // Receives url, title and page (each may be undefined). Returns an object with only the parts that were given, keys in that order.
  // Example: sourceOf("https://example.com/b") -> { url: "https://example.com/b" }, sourceOf() -> {}
  return {};
}

export function packageFinding(claim: string, url?: string, title?: string, page?: number) {
  const source = sourceOf(url, title, page);
  return { claim: claim.trim(), source: Object.keys(source).length > 0 ? source : null };
}

function addSource(entry: { sources: Array<Record<string, unknown>> }, source: Record<string, unknown> | null): void {
  // TODO 9 of 9 (unlocks e7): keep every different source of a merged claim.
  // Receives the merged entry (with a sources array) and one source (an object or null). Pushes it unless it is null or already there.
  // Example: sources [A], add B -> [A, B]; add A again -> [A, B]
}

export function mergeFindings(findings: Array<{ claim: string; source: Record<string, unknown> | null }>) {
  const merged = new Map<string, { claim: string; sources: Array<Record<string, unknown>> }>();
  for (const finding of findings) {
    const key = finding.claim.toLowerCase().split(/\s+/).filter(Boolean).join(" ");
    const entry = merged.get(key) ?? { claim: finding.claim, sources: [] };
    merged.set(key, entry);
    addSource(entry, finding.source);
  }
  return [...merged.values()].map((e) => ({ claim: e.claim, sources: e.sources, attributed: e.sources.length > 0 }));
}

/** Collect every message of a single-shot run. The SDK throws after an error result: what was received is kept. */
export async function runTeam(prompt: string, options: any): Promise<{ messages: any[]; error: string | null }> {
  const messages: any[] = [];
  let error: string | null = null;
  try {
    for await (const message of query({ prompt, options })) messages.push(message);
  } catch (exc) { // the error result, if there was one, is already in `messages`
    error = exc instanceof Error ? exc.message : String(exc);
  }
  return { messages, error };
}
