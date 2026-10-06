// Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("subagents");

const READ_ONLY = ["Read", "Grep", "Glob"];
const SPAWN_TOOLS = ["Agent", "Task"]; // current and old name of the tool that starts a subagent

type Spec = { description?: string; prompt?: string; tools?: string[] | null; model?: string };

function validName(name: string): boolean {
  return /^[a-z][a-z0-9-]*$/.test(name);
}

function complete(spec: Spec): boolean {
  return Boolean(String(spec.description ?? "").trim()) && Boolean(String(spec.prompt ?? "").trim());
}

function agentTools(listed: string[] | null | undefined): string[] {
  if (listed == null) return [...READ_ONLY]; // read-only unless told otherwise
  return listed.filter((t) => t !== "Agent"); // and no spawning from below
}

function limitEnv(maxConcurrent: number): Record<string, string> {
  return { CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS: String(maxConcurrent) };
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
  return block.type === "tool_use" && SPAWN_TOOLS.includes(block.name);
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
  for (const block of inner) if (block.type === "tool_use" && !group.tools.includes(block.name)) group.tools.push(block.name);
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
  const kept = (items ?? []).map((item) => String(item).trim()).filter((item) => item !== "");
  return kept.length > 0 ? [`${title}:`, ...kept.map((item) => `- ${item}`)] : [];
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
  const source: Record<string, unknown> = {};
  if (url !== undefined) source.url = url;
  if (title !== undefined) source.title = title;
  if (page !== undefined) source.page = page;
  return source;
}

export function packageFinding(claim: string, url?: string, title?: string, page?: number) {
  const source = sourceOf(url, title, page);
  return { claim: claim.trim(), source: Object.keys(source).length > 0 ? source : null };
}

function addSource(entry: { sources: Array<Record<string, unknown>> }, source: Record<string, unknown> | null): void {
  if (source !== null && !entry.sources.some((s) => JSON.stringify(s) === JSON.stringify(source))) entry.sources.push(source);
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
