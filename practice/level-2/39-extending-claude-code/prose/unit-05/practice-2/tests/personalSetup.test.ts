import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { basename, join, relative, resolve } from "node:path";

// The solution is the file beside this test.
const ROOT = resolve((import.meta.dirname + "/../project"));
const HOME = "home/.claude";
const PROJECT = "project/.claude";
const BUILTIN_STYLES = new Set(["Proactive", "Concise", "Explanatory", "Learning"]);
const STYLE_FIELDS = new Set(["name", "description", "keep-coding-instructions", "force-for-plugin"]);
const CONTEXTS = new Set(["Global", "Chat", "Autocomplete", "Settings", "Confirmation", "Tabs", "Help", "Transcript", "HistorySearch", "Task", "ThemePicker", "Attachments", "Footer",
  "MessageSelector", "DiffDialog", "DiffPanel", "ModelPicker", "EffortSlider", "Select", "Plugin", "Pane", "PaneField", "Agents", "Scroll"]);
const MODIFIERS: Record<string, string> = { ctrl: "ctrl", control: "ctrl", shift: "shift", alt: "alt", opt: "alt", option: "alt", meta: "alt", cmd: "cmd", command: "cmd", super: "cmd", win: "cmd" };
const NAMED_KEYS = new Set(["escape", "esc", "enter", "return", "tab", "space", "up", "down", "left", "right", "pageup", "pagedown", "home", "end", "backspace", "delete", "wheelup", "wheeldown"]);
const RESERVED = new Set(["ctrl+c", "ctrl+d", "ctrl+m", "ctrl+[", "ctrl+i", "ctrl+h"]);
const ACTION = /^[a-z][A-Za-z]*:[A-Za-z]+$/;
const STATUS_FIELDS = new Set(["model.id", "model.display_name", "cwd", "workspace.current_dir", "workspace.project_dir", "workspace.added_dirs", "workspace.git_worktree", "cost.total_cost_usd",
  "cost.total_duration_ms", "cost.total_api_duration_ms", "cost.total_lines_added", "cost.total_lines_removed", "context_window.total_input_tokens",
  "context_window.total_output_tokens", "context_window.context_window_size", "context_window.used_percentage", "context_window.remaining_percentage",
  "exceeds_200k_tokens", "fast_mode", "effort.level", "thinking.enabled", "rate_limits.five_hour.used_percentage", "rate_limits.seven_day.used_percentage",
  "session_id", "session_name", "transcript_path", "version", "output_style.name", "vim.mode", "agent.name", "pr.number", "pr.url", "pr.review_state",
  "worktree.name", "worktree.path", "worktree.branch"]);
const PERSONAL = /\/home\/|\/Users\/|C:\\Users|[\w.+-]+@[\w-]+\.[\w.-]+|sk-ant|TODO|FIXME/;

function isObject(value: unknown): boolean {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function readJson(rel: string): any {
  let data: any;
  try {
    data = JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
  assert.ok(isObject(data), `${rel} must hold an object`);
  return data;
}

function front(rel: string): [Record<string, string>, string] {
  const match = /^---\n([\s\S]*?)\n---\n?([\s\S]*)$/.exec(read(rel));
  assert.ok(match, `${rel} has no front matter`);
  const fields: Record<string, string> = {};
  for (const line of (match as RegExpExecArray)[1].split("\n")) {
    const at = line.indexOf(":");
    if (at > 0 && line.slice(0, at).trim()) fields[line.slice(0, at).trim()] = line.slice(at + 1).trim();
  }
  return [fields, (match as RegExpExecArray)[2]];
}

function styleFiles(): string[] {
  const folder = join(ROOT, HOME, "output-styles");
  return existsSync(folder) ? readdirSync(folder).filter((f) => f.endsWith(".md")).sort() : [];
}

function customStyles(): Set<string> {
  const names = new Set<string>();
  for (const file of styleFiles()) names.add(front(`${HOME}/output-styles/${file}`)[0].name || basename(file, ".md"));
  return names;
}

function knownStyle(name: unknown): boolean {
  return typeof name === "string" && name !== "" && (BUILTIN_STYLES.has(name) || customStyles().has(name));
}

function keyProblem(keystroke: string): string | null {
  for (const part of keystroke.split(/\s+/).filter(Boolean)) {
    const pieces = part.split("+");
    const key = pieces[pieces.length - 1].toLowerCase();
    if (!(key.length === 1 || NAMED_KEYS.has(key))) return `${part}: unknown key ${key}`;
    for (const mod of pieces.slice(0, -1)) if (!(mod.toLowerCase() in MODIFIERS)) return `${part}: unknown modifier ${mod}`;
  }
  return null;
}

function normal(keystroke: string): string {
  const parts = keystroke.toLowerCase().split("+");
  return [...parts.slice(0, -1).map((m) => MODIFIERS[m] ?? m), parts[parts.length - 1]].join("+");
}

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

test("m1 the user level holds a style a status line and a keybindings file that fit together", () => {
  const settings = readJson(`${HOME}/settings.json`);
  assert.ok(typeof settings.outputStyle === "string" && settings.outputStyle, "the user settings select an output style");
  assert.ok(knownStyle(settings.outputStyle), `outputStyle ${settings.outputStyle} names no built-in style and no style file`);
  const line = settings.statusLine;
  assert.ok(isObject(line) && line.type === "command", "statusLine is an object of type command");
  const command = line.command;
  assert.ok(typeof command === "string" && command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder");
  const script = join(ROOT, "home", ".claude", command.slice("~/.claude/".length));
  assert.ok(existsSync(script) && statSync(script).isFile(), `${command} is not a file`);
  const bindings = readJson(`${HOME}/keybindings.json`).bindings;
  assert.ok(Array.isArray(bindings) && bindings.length > 0, "keybindings.json holds a non-empty bindings array");
  assert.ok(bindings.every((b: any) => isObject(b) && typeof b.context === "string" && isObject(b.bindings)), "each block has a context and a bindings object");
});

test("e1 the style file keeps the coding instructions and carries only documented fields", () => {
  const files = styleFiles();
  assert.ok(files.length > 0, "an output style file is needed");
  for (const file of files) {
    const [fields, body] = front(`${HOME}/output-styles/${file}`);
    const unknown = Object.keys(fields).filter((k) => !STYLE_FIELDS.has(k));
    assert.deepEqual(unknown, [], `${file}: unknown front matter fields ${unknown} (a misspelled field is ignored without an error)`);
    assert.ok(fields.name, `${file}: name the style`);
    assert.ok(fields.description, `${file}: describe the style`);
    assert.equal(fields["keep-coding-instructions"], "true", `${file}: a style that only changes communication keeps the coding instructions`);
    assert.ok(body.split("\n").filter((l) => l.trim()).length >= 3, `${file}: the instructions are too thin`);
  }
});

test("e2 the status line reads only fields the session sends and refreshes no faster than every second", () => {
  const line = readJson(`${HOME}/settings.json`).statusLine;
  assert.ok(isObject(line), "statusLine is an object");
  if ("padding" in line) assert.ok(Number.isInteger(line.padding) && line.padding >= 0, "padding is a number of characters, 0 or more");
  if ("refreshInterval" in line) assert.ok(Number.isInteger(line.refreshInterval) && line.refreshInterval >= 1, "refreshInterval is in seconds, 1 or more");
  const command: string = line.command ?? "";
  assert.ok(command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder");
  const script = read(`${HOME}/${command.slice("~/.claude/".length)}`);
  assert.ok(script.startsWith("#!/bin/sh") || script.startsWith("#!/usr/bin/env bash") || script.startsWith("#!/bin/bash"), "the script starts with a shebang line");
  const used = new Set<string>();
  for (const m of script.matchAll(/jq -r '([^']*)'/g)) for (const f of m[1].matchAll(/(?<![\w$"])\.([A-Za-z_][\w.]*)/g)) used.add(f[1]);
  assert.ok(used.size > 0, "the script reads at least one field of the session JSON with jq");
  const unknown = [...used].filter((f) => !STATUS_FIELDS.has(f)).sort();
  assert.deepEqual(unknown, [], `fields the session does not send: ${unknown}`);
});

test("e3 the keybindings use real contexts and actions free keys and a null to unbind", () => {
  const blocks = readJson(`${HOME}/keybindings.json`).bindings;
  assert.ok(Array.isArray(blocks) && blocks.length > 0, "the file holds a bindings array");
  let rebound = 0;
  let unbound = 0;
  for (const block of blocks) {
    assert.ok(CONTEXTS.has(block.context), `${block.context} is not a context (names are case sensitive)`);
    for (const [keystroke, action] of Object.entries(block.bindings ?? {})) {
      const problem = keyProblem(keystroke);
      assert.equal(problem, null, String(problem));
      assert.ok(!RESERVED.has(normal(keystroke)), `${keystroke} is reserved and cannot be rebound`);
      if (action === null) unbound += 1;
      else {
        assert.ok(typeof action === "string" && ACTION.test(action), `${action} is not a namespace:action name`);
        rebound += 1;
      }
    }
  }
  assert.ok(rebound >= 1 && unbound >= 1, "rebind one key to an action and unbind another with null");
});

test("e4 personal settings stay in the user files and the project file stays shared", () => {
  const shared = readJson(`${PROJECT}/settings.json`);
  for (const key of ["outputStyle", "statusLine"]) assert.ok(!(key in shared), `${key} is personal: a shared project file would override every teammate's own choice`);
  const allow = isObject(shared.permissions) ? shared.permissions.allow : undefined;
  assert.ok(Array.isArray(allow) && allow.length > 0, "the shared file carries the team's permission rules");
  const local = readJson(`${PROJECT}/settings.local.json`);
  assert.ok(knownStyle(local.outputStyle), "the local file overrides the style for this project with an exact style name");
});

test("e5 the local settings file is kept out of git", () => {
  const lines = read("project/.gitignore").split("\n").map((l) => l.trim());
  assert.ok(lines.includes(".claude/settings.local.json") || lines.includes("**/.claude/settings.local.json"), "list the local settings file in the ignore file");
});

test("e6 no file holds a personal path an address a key or an unfinished marker", () => {
  const files = walk(ROOT);
  assert.ok(files.length > 0, "there are no files");
  for (const path of files) {
    const match = PERSONAL.exec(readFileSync(path, "utf8"));
    assert.equal(match, null, `${relative(ROOT, path)} holds ${match?.[0]}`);
  }
});
