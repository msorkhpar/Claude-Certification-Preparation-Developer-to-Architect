/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("data_policy");

const SAFE_INPUT = ["tokenise", "redact"]; // pii_handling values that keep identifiers out of the prompt
const WANT: Record<string, string> = { us: "us", eu: "eu", other: "global" }; // the residency of the deployment that may serve a user region

const byText = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
const given = (value: any) => value !== undefined && value !== null;

function report(findings: Set<string>): string[] | null {
  return [...findings].sort(byText);
}

function residencyFindings(req: any, config: any): Set<string> {
  const platform = config.platform;
  const region: string = config.region ?? "";
  if (req.residency === "us") {
    if (platform === "api" || platform === "aws-platform") {
      if (config.inference_geo !== "us") return new Set(["residency-not-pinned"]);
    } else if (!region.startsWith("us-")) {
      return new Set(["residency-region"]);
    }
  } else if (req.residency === "eu") {
    if (platform === "api" || platform === "aws-platform") {
      return new Set(["residency-unavailable"]);
    } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region === "eu")) {
      return new Set(["residency-region"]);
    }
  }
  return new Set();
}

function zdrFindings(req: any, config: any): Set<string> {
  const findings = new Set<string>();
  if (req.zdr_required) {
    if (config.platform === "bedrock" || config.platform === "vertex") {
      findings.add("zdr-not-anthropics");
    } else {
      if (!config.zdr) findings.add("zdr-missing");
      if (String(config.model ?? "").startsWith("claude-fable")) findings.add("model-needs-retention");
    }
  }
  return findings;
}

function phiFindings(req: any, config: any): Set<string> {
  const findings = new Set<string>();
  if (req.phi) {
    if (config.platform === "api" && !config.hipaa_baa) findings.add("phi-no-baa");
    if (config.platform === "aws-platform") findings.add("phi-platform-unsupported");
    if (!SAFE_INPUT.includes(config.pii_handling)) {
      findings.add("phi-not-deidentified");
    }
  }
  return findings;
}

function tenantFindings(req: any, config: any): Set<string> {
  return req.multi_tenant && config.tenancy !== "workspace-per-tenant" ? new Set(["tenant-isolation"]) : new Set();
}

function retentionFindings(req: any, config: any): Set<string> {
  const findings = new Set<string>();
  const days = (config.audit ?? {}).retain_days;
  if (given(days)) {
    if (given(req.audit_max_days) && days > req.audit_max_days) findings.add("retention-too-long");
    if (given(req.audit_min_days) && days < req.audit_min_days) findings.add("retention-too-short");
  }
  return findings;
}

function auditFindings(req: any, config: any): Set<string> {
  const audit = config.audit ?? {};
  return audit.store_prompts && (req.phi || !SAFE_INPUT.includes(config.pii_handling)) ? new Set(["audit-stores-sensitive"]) : new Set();
}

export function checkDeployment(config: any, req: any): string[] | null {
  log.debug("checkDeployment input", config);
  const findings = new Set<string>();
  for (const check of [residencyFindings, zdrFindings, phiFindings, tenantFindings, auditFindings, retentionFindings]) {
    for (const finding of check(req, config)) findings.add(finding);
  }
  return report(findings);
}

const dayNumber = (iso: string): number => Math.floor(Date.parse(`${iso}T00:00:00Z`) / 86_400_000);
const ageDays = (day: string, today: string): number => dayNumber(today) - dayNumber(day);

export function retentionActions(entries: any[], maxDays: number, today: string): { purge: string[]; keep: string[] } | null {
  const purge: string[] = [];
  const keep: string[] = [];
  for (const e of [...entries].sort((a, b) => byText(a.id, b.id))) {
    (ageDays(e.date, today) > maxDays && !e.hold ? purge : keep).push(e.id);
  }
  return { purge, keep };
}

export function pickDeployment(userRegion: string, deployments: any[]): string | null {
  const matching = deployments.filter((d) => d.residency === WANT[userRegion]).map((d) => d.name as string).sort(byText);
  return matching.length > 0 ? matching[0] : null;
}
