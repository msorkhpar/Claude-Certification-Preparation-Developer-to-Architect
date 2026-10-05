// An injection-resistant tool gate. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("gate");
export type Args = Record<string, unknown>;
export type Decision = { decision: "allow" | "ask" | "deny"; reason: string };

const TOOLS = new Set(["read_file", "write_file", "bash", "fetch", "send_email"]);
const SIGNALS: Array<[string, RegExp]> = [
  ["override", /\b(ignore|disregard|forget)\b[\s\S]{0,40}\b(previous|prior|above|earlier|system)\b[\s\S]{0,20}\b(instructions?|prompts?|rules)\b/i],
  ["role-tag", /<\s*\/?\s*(system|assistant|tool_result|instructions?)\s*>/i],
  ["exfiltrate", /\b(send|email|forward|post|upload)\b[\s\S]{0,60}\b(to|at)\b[\s\S]{0,40}[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+/i],
  ["reveal", /\b(reveal|print|show|repeat)\b[\s\S]{0,40}\b(system prompt|password|secret|api key)\b/i],
];
const SHELL_TRICKS = [";", "&", "|", ">", "<", "`", "$(", "\n"];

/** The names of the injection signals found in the text, in the fixed order of SIGNALS. */
export function screen(text: string): string[] {
  return SIGNALS.filter(([, pattern]) => pattern.test(text)).map(([name]) => name);
}

/** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
export function wrapUntrusted(toolUseId: string, source: string, content: string): Record<string, unknown> {
  log.debug("wrapUntrusted input", content);
  // TODO 1 of 6 (unlocks m1 and e1): wrap untrusted text as a tool_result block.
  // Receives the tool use id, where the text came from and the text. Returns a tool_result object. Clean text becomes one line of
  // compact JSON (JSON.stringify) with the keys source, trust ("untrusted") and content, in that order. When `screen` finds a signal,
  // return the error block from the statement instead and leave the text out of it.
  // Example: wrapUntrusted("t1", "web page", "hi")
  //   -> { type: "tool_result", tool_use_id: "t1", content: '{"source":"web page","trust":"untrusted","content":"hi"}' }
  return { type: "tool_result", tool_use_id: toolUseId, content };
}

function luhn(digits: string): boolean {
  let total = 0;
  [...digits].reverse().forEach((ch, i) => {
    let d = Number(ch);
    if (i % 2 === 1) d = d * 2 > 9 ? d * 2 - 9 : d * 2;
    total += d;
  });
  return total % 10 === 0;
}

/** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
export function redact(input: string): string {
  let text = input.replace(/sk-ant-[A-Za-z0-9_-]{8,}/g, "[SECRET]");
  text = text.replace(/\bAKIA[0-9A-Z]{16}\b/g, "[SECRET]");
  text = text.replace(/Bearer [A-Za-z0-9._-]{16,}/g, "Bearer [SECRET]");
  text = text.replace(/[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+/g, "[EMAIL]");
  return text.replace(/\b(?:[0-9][ -]?){12,18}[0-9]\b/g, (m) => {
    const digits = m.replace(/[ -]/g, "");
    return isCard(digits) ? "[CARD]" : m;
  });
}

function resolve(root: string, path: string): string {
  const p = path.replaceAll("\\", "/");
  const stack: string[] = [];
  for (const part of (p.startsWith("/") ? p : `${root}/${p}`).split("/")) {
    if (part === "" || part === ".") continue;
    if (part === "..") stack.pop();
    else stack.push(part);
  }
  return "/" + stack.join("/");
}

function isSecret(path: string): boolean {
  // TODO 2 of 6 (unlocks e2): does this path name a secret file?
  // Receives a path and returns true for a file named `.env`, a file starting `.env.` unless it is `.env.example`, any file inside a
  // folder called `secrets`, and a name ending in `.pem` or `.key`; false otherwise. Backslashes count as `/`.
  // Example: isSecret("app/.env.local") -> true, isSecret(".env.example") -> false
  return false;
}

function commandAllowed(words: string[]): boolean {
  // TODO 3 of 6 (finish this to pass e3): is the first word of a bash command on the allow-list?
  // Receives the command split into words. Returns true for `ls`, `cat`, `pytest`, and `git` followed by `status`, `diff` or `log`;
  // false for anything else, including an empty list. (The checks for sudo, rm, chaining and secret arguments are written.)
  // Example: commandAllowed(["git", "log", "--oneline"]) -> true, commandAllowed(["git", "push"]) -> false
  return false;
}

function isCard(digits: string): boolean {
  // TODO 4 of 6 (finish this to pass e6): is this run of digits a card number?
  // Receives the digits with spaces and hyphens already removed. Returns true when there are 13 to 19 of them and `luhn` accepts them
  // (a long number that fails the check is not a card). Example: isCard("4111111111111111") -> true, isCard("4111111111111112") -> false
  return false;
}

const result = (decision: Decision["decision"], reason = "ok"): Decision => ({ decision, reason });

export class Gate {
  readonly root: string;
  private allowedHosts: string[];
  private allowedEmailDomains: string[];
  private isTainted = false;
  private records: Array<Record<string, unknown>> = [];

  constructor(root: string, allowedHosts: string[], allowedEmailDomains: string[]) {
    this.root = resolve("/", root);
    this.allowedHosts = allowedHosts.map((h) => h.toLowerCase());
    this.allowedEmailDomains = allowedEmailDomains.map((d) => d.toLowerCase());
  }

  get tainted(): boolean {
    return this.isTainted;
  }

  get audit(): Array<Record<string, unknown>> {
    return [...this.records];
  }

  markUntrusted(_source: string): void {
    this.isTainted = true;
  }

  private inside(path: string): boolean {
    return path === this.root || path.startsWith(this.root + "/");
  }

  decide(actor: string, tool: string, args: Args): Decision {
    const r = this.choose(tool, args);
    const clean: Args = {};
    for (const [k, v] of Object.entries(args)) clean[k] = typeof v === "string" ? redact(v) : v;
    this.records.push({ actor, tool, decision: r.decision, reason: r.reason, args: clean });
    return r;
  }

  private choose(tool: string, args: Args): Decision {
    if (!TOOLS.has(tool)) return result("deny", "unknown tool");
    if (tool === "read_file" || tool === "write_file") {
      const path = resolve(this.root, String(args.path ?? ""));
      if (!this.inside(path)) return result("deny", "outside the project");
      if (isSecret(path)) return result("deny", "secret file");
      if (tool === "write_file") {
        if (path.split("/").some((s) => s === ".git" || s === ".claude")) return result("deny", "protected path");
        if (this.isTainted) return result("ask", "untrusted content in this session");
      }
      return result("allow");
    }
    if (tool === "bash") return this.bash(String(args.command ?? ""));
    if (tool === "fetch") return this.fetch(String(args.url ?? ""));
    return this.email(args);
  }

  private bash(command: string): Decision {
    const words = command.split(/\s+/).filter(Boolean);
    if (words.some((w) => ["sudo", "rm"].includes(w.split("/").at(-1) as string))) return result("deny", "dangerous command");
    if (SHELL_TRICKS.some((t) => command.includes(t))) return result("deny", "chaining or redirection");
    if (!commandAllowed(words)) return result("deny", "command not allowed");
    if (words.slice(1).some((w) => !w.startsWith("-") && isSecret(w))) return result("deny", "secret file");
    if (words[0] === "pytest" && this.isTainted) return result("ask", "untrusted content in this session");
    return result("allow");
  }

  private fetch(url: string): Decision {
    const m = /^([A-Za-z][A-Za-z0-9+.-]*):\/\/([^/?#]*)([^?#]*)(\?[^#]*)?(#.*)?$/.exec(url);
    if (!m || m[1].toLowerCase() !== "https") return result("deny", "https only");
    if (m[2].includes("@")) return result("deny", "credentials in the URL");
    const host = m[2].toLowerCase().replace(/:[0-9]+$/, "");
    if (!this.allowedHosts.some((h) => host === h || host.endsWith("." + h))) return result("deny", "host not allowed");
    if (this.isTainted && (m[4] || m[5])) return result("ask", "data could leave in the URL");
    return result("allow");
  }

  private email(args: Args): Decision {
    const to = String(args.to ?? "");
    const domain = to.includes("@") ? to.slice(to.lastIndexOf("@") + 1).toLowerCase() : "";
    const text = `${args.subject ?? ""}\n${args.body ?? ""}`;
    return this.emailDecision(domain, text);
  }

  private emailDecision(domain: string, text: string): Decision {
    // TODO 5 of 6 (finish this to pass e4 and e5): decide a send_email call.
    // Receives the recipient's lower-case domain and the subject and body joined by a newline. Returns result(decision, reason): deny
    // "recipient not allowed" when the domain is not in this.allowedEmailDomains; deny "sensitive data in the body" when redact(text)
    // differs from the text; deny "a person must send it" when the session is tainted (this.isTainted); else allow.
    // Example: a clean mail to "example.com" in a tainted session -> { decision: "deny", reason: "a person must send it" }
    return result("allow");
  }

  alerts(): Array<{ actor: string; denials: number }> {
    // TODO 6 of 6 (finish this to pass e7): actors with three or more denials.
    // Reads this.records (objects with actor and decision). Returns one { actor, denials } per actor with three or more "deny" records
    // (an "ask" is not a denial), with that actor's current count, ordered by when each reached three.
    // Example: alice denied 3 times, then bob 3 times, then alice once more -> [{ actor: "alice", denials: 4 }, { actor: "bob", denials: 3 }]
    return [];
  }
}

/** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
export function hookResponse(d: Decision): { exit_code: number; stdout: string; stderr: string } {
  if (d.decision === "allow") return { exit_code: 0, stdout: "", stderr: "" };
  const out = { hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: d.decision, permissionDecisionReason: d.reason } };
  return { exit_code: 0, stdout: JSON.stringify(out), stderr: "" };
}
