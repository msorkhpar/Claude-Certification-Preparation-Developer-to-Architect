/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("launch_review");

const SEVERITY: Record<string, number> = { high: 0, medium: 1, low: 2 };
const DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"];

type Add = (condition: boolean, severity: string, domain: string, rule: string) => void;
type Numbers = Record<string, number>;

function p1Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(!f.has("feedback_loop"), "high", "P1", "missing-feedback");
  add((f.has("agent") || f.has("team")) && f.has("path_known"), "medium", "P1", "autonomy-without-need");
  add(f.has("team") && (n["team_value_chats"] ?? 0) < 15, "medium", "P1", "team-below-price");
}

function p3Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(f.has("filter_after_ranking"), "high", "P3", "filter-after-ranking");
  add(!f.has("replace_on_change"), "high", "P3", "stale-index");
  add((n["tool_tokens"] ?? 0) > 10000 && !f.has("deferral"), "medium", "P3", "tool-bloat");
  add(f.has("agent_rights_only"), "high", "P3", "agent-rights-only");
}

function p4Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(!f.has("protected_segment"), "medium", "P4", "no-protected-segment");
  add((n["eval_cases"] ?? 0) < 20, "low", "P4", "small-eval-set");
  add(!f.has("rollback"), "high", "P4", "no-way-back");
  add((n["rollout_stages"] ?? 0) < 3, "medium", "P4", "big-bang-rollout");
}

function p5Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(f.has("pii_reaches_model"), "high", "P5", "identifiers-reach-model");
  add(f.has("residency_unmet"), "high", "P5", "residency-unmet");
  add(f.has("audit_keeps_content"), "medium", "P5", "audit-keeps-content");
  add(f.has("irreversible_action") && !f.has("human_step"), "high", "P5", "irreversible-without-person");
  add((n["retain_days"] ?? 0) < (n["floor_days"] ?? 0) || (n["retain_days"] ?? 0) > (n["ceiling_days"] ?? 0), "medium", "P5", "retention-outside-window");
}

function p2Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(f.has("volatile_prefix"), "medium", "P2", "volatile-prefix");
  add(!f.has("model_measured"), "low", "P2", "model-not-measured");
}

function p6Rules(f: Set<string>, n: Numbers, add: Add): void {
  add(!f.has("owner"), "medium", "P6", "no-accountable-owner");
  add((n["latency_ms"] ?? 0) <= 0 || (n["availability_tenths"] ?? 0) <= 0, "medium", "P6", "sla-without-numbers");
  add(!f.has("accuracy_stated"), "low", "P6", "accuracy-unstated");
}

function p7Rules(f: Set<string>, n: Numbers, add: Add): void {
  add((n["team_size"] ?? 0) > 10 && !f.has("managed_settings"), "medium", "P7", "unmanaged-team-settings");
}

function order(found: string[]): string[] {
  const part = (s: string, i: number) => s.split(" ")[i];
  return found.sort((a, b) => SEVERITY[part(a, 0)] - SEVERITY[part(b, 0)] || (part(a, 1) < part(b, 1) ? -1 : part(a, 1) > part(b, 1) ? 1 : 0) || (part(a, 2) < part(b, 2) ? -1 : part(a, 2) > part(b, 2) ? 1 : 0));
}

/** The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule. */
export function launchReview(flags: Set<string>, numbers: Record<string, number>): string[] {
  log.debug("launchReview input", [...flags].sort());
  const f = new Set(flags);
  const n = { ...numbers };
  const found: string[] = [];
  const add: Add = (condition, severity, domain, rule) => {
    if (condition) found.push(`${severity} ${domain} ${rule}`);
  };
  for (const rules of [p1Rules, p2Rules, p3Rules, p4Rules, p5Rules, p6Rules, p7Rules]) rules(f, n, add);
  return order(found);
}

/** reject for any high finding, revise for any medium one, otherwise approve. */
export function verdict(findings: string[]): string {
  if (findings.some((x) => x.startsWith("high "))) return "reject";
  if (findings.some((x) => x.startsWith("medium "))) return "revise";
  return "approve";
}

/** The number of findings in each domain, P1 to P7. */
export function scorecard(findings: string[]): number[] {
  return DOMAINS.map((d) => findings.filter((x) => x.split(" ")[1] === d).length);
}

/** The break-even accuracy in whole percent, rounded up on the cost side; 0 when an error costs nothing or no more than a check. */
export function neededAccuracy(errorCost: number, reviewCost: number): number {
  if (errorCost <= 0) return 0;
  const needed = Math.ceil((100 * reviewCost) / errorCost);
  return Math.max(0, 100 - needed);
}
