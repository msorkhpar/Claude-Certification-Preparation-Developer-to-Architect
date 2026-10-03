// An injection-resistant tool gate. See ../../statement.md.
export type Args = Record<string, unknown>;
export type Decision = { decision: "allow" | "ask" | "deny"; reason: string };

/** The names of the injection signals found in the text, in the fixed order: override, role-tag, exfiltrate, reveal. */
export function screen(_text: string): string[] {
  return [];
}

/** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
export function wrapUntrusted(toolUseId: string, _source: string, content: string): Record<string, unknown> {
  return { type: "tool_result", tool_use_id: toolUseId, content };
}

/** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
export function redact(text: string): string {
  return text;
}

export class Gate {
  readonly root: string;
  readonly allowedHosts: string[];
  readonly allowedEmailDomains: string[];

  constructor(root: string, allowedHosts: string[], allowedEmailDomains: string[]) {
    this.root = root;
    this.allowedHosts = allowedHosts;
    this.allowedEmailDomains = allowedEmailDomains;
  }

  get tainted(): boolean {
    return false;
  }

  get audit(): Array<Record<string, unknown>> {
    return [];
  }

  markUntrusted(_source: string): void {}

  decide(_actor: string, _tool: string, _args: Args): Decision {
    return { decision: "allow", reason: "ok" };
  }

  alerts(): Array<{ actor: string; denials: number }> {
    return [];
  }
}

/** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
export function hookResponse(_decision: Decision): { exit_code: number; stdout: string; stderr: string } {
  return { exit_code: 0, stdout: "", stderr: "" };
}
