// Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
//
// The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
// security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
// This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
// that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
// the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.

import { logger } from "./logger.ts";
const log = logger("deidentify");

export const PATTERNS: Array<[string, RegExp]> = [["EMAIL", /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g], ["MEMBER", /\bM-\d{6}\b/g]];

/** Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local. */
export function tokenise(text: string, vault: Map<string, string>): string {
  log.debug("tokenise input", text);
  for (const [label, pattern] of PATTERNS) {
    text = text.replace(pattern, (value) => {
      if (!vault.has(value)) vault.set(value, `<${label}_${[...vault.values()].filter((t) => t.startsWith(`<${label}_`)).length + 1}>`);
      return vault.get(value)!;
    });
  }
  return text;
}

export function restore(text: string, vault: Map<string, string>): string {
  for (const [value, token] of vault) text = text.replaceAll(token, value);
  return text;
}

/** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
export function auditEntry(requestId: string, original: string, vault: Map<string, string>) {
  return { request_id: requestId, chars: original.length, tokens_issued: vault.size, prompt_stored: false };
}

function main() {
  const original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice.";
  const vault = new Map<string, string>();
  const sent = tokenise(original, vault);
  console.log("sent to the model:", sent);
  const reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim.";
  console.log("model reply (illustrative):", reply);
  console.log("restored for the user:", restore(reply, vault));
  const entry = auditEntry("req-001", original, vault);
  console.log("audit entry:", `request_id=${entry.request_id} chars=${entry.chars} tokens_issued=${entry.tokens_issued} prompt_stored=${entry.prompt_stored ? "True" : "False"}`);
  console.log("raw values in the entry:", [...vault.keys()].some((value) => JSON.stringify(entry).includes(value)) ? "True" : "False");
  console.log("gap: the name survives tokenising:", sent.includes("Jane Doe") ? "True" : "False");
}

if (import.meta.main) main();
