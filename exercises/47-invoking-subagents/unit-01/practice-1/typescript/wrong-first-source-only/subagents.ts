// Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";

const READ_ONLY = ["Read", "Grep", "Glob"];
const SPAWN_TOOLS = ["Agent", "Task"]; // current and old name of the tool that starts a subagent

type Spec = { description?: string; prompt?: string; tools?: string[] | null; model?: string };

export function buildOptions(agents: Record<string, Spec>, cwd: string, cliPath?: string, maxBudgetUsd = 2.0, maxTurns = 20, maxConcurrent = 5): any {
  const definitions: Record<string, any> = {};
  for (const [name, spec] of Object.entries(agents)) {
    if (!/^[a-z][a-z0-9-]*$/.test(name)) throw new Error(`bad agent name: ${name}`);
    if (!String(spec.description ?? "").trim() || !String(spec.prompt ?? "").trim()) throw new Error(`agent ${name} needs a description and a prompt`);
    const tools = spec.tools == null ? [...READ_ONLY] : spec.tools.filter((t) => t !== "Agent"); // read-only unless told otherwise, and no spawning from below
    definitions[name] = { description: spec.description, prompt: spec.prompt, tools, model: spec.model || "inherit" };
  }
  return {
    agents: definitions, allowedTools: ["Agent", ...READ_ONLY], maxTurns, maxBudgetUsd, cwd,
    env: { ...process.env, CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS: String(maxConcurrent) },
    permissionMode: "default", settingSources: [],
    ...(cliPath ? { pathToClaudeCodeExecutable: cliPath } : {}),
  };
}

function blocks(message: any): any[] {
  const content = message?.message?.content;
  return Array.isArray(content) ? content : [];
}

export function spawned(messages: any[]): Array<{ id: string; subagent_type: string; description: string; prompt: string }> {
  const found: Array<{ id: string; subagent_type: string; description: string; prompt: string }> = [];
  for (const message of messages) {
    for (const block of blocks(message)) {
      if (block.type === "tool_use" && SPAWN_TOOLS.includes(block.name)) {
        const input = block.input ?? {};
        found.push({ id: block.id, subagent_type: input.subagent_type || "general-purpose", description: input.description ?? "", prompt: input.prompt ?? "" });
      }
    }
  }
  return found;
}

export function bySubagent(messages: any[]): Record<string, { subagent_type: string; messages: number; tools: string[] }> {
  const groups: Record<string, { subagent_type: string; messages: number; tools: string[] }> = {};
  for (const call of spawned(messages)) groups[call.id] = { subagent_type: call.subagent_type, messages: 0, tools: [] };
  for (const message of messages) {
    const parent = message?.parent_tool_use_id;
    if (parent && groups[parent]) {
      groups[parent].messages += 1;
      for (const block of blocks(message)) if (block.type === "tool_use" && !groups[parent].tools.includes(block.name)) groups[parent].tools.push(block.name);
    }
  }
  return groups;
}

export function makeBrief(task: string, files?: string[], facts?: string[], output?: string): string {
  if (!String(task ?? "").trim()) throw new Error("a brief needs a task");
  const lines = [`Task: ${task.trim()}`];
  for (const [title, items] of [["Files", files], ["Known", facts]] as const) {
    const kept = (items ?? []).map((item) => String(item).trim()).filter((item) => item !== "");
    if (kept.length > 0) lines.push(`${title}:`, ...kept.map((item) => `- ${item}`));
  }
  if (output && output.trim()) lines.push(`Return: ${output.trim()}`);
  return lines.join("\n");
}

export function packageFinding(claim: string, url?: string, title?: string, page?: number) {
  const source: Record<string, unknown> = {};
  if (url !== undefined) source.url = url;
  if (title !== undefined) source.title = title;
  if (page !== undefined) source.page = page;
  return { claim: claim.trim(), source: Object.keys(source).length > 0 ? source : null };
}

export function mergeFindings(findings: Array<{ claim: string; source: Record<string, unknown> | null }>) {
  const merged = new Map<string, { claim: string; sources: Array<Record<string, unknown>> }>();
  for (const finding of findings) {
    const key = finding.claim.toLowerCase().split(/\s+/).filter(Boolean).join(" ");
    const entry = merged.get(key) ?? { claim: finding.claim, sources: [] };
    merged.set(key, entry);
    if (finding.source !== null && entry.sources.length === 0 && !entry.sources.some((s) => JSON.stringify(s) === JSON.stringify(finding.source))) entry.sources.push(finding.source);
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
