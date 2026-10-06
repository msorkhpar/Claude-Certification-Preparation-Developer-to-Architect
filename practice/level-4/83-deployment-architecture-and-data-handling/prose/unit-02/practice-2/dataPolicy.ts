/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("data_policy");

const SAFE_INPUT = ["tokenise", "redact"]; // pii_handling values that keep identifiers out of the prompt
const WANT: Record<string, string> = { us: "us", eu: "eu", other: "global" }; // the residency of the deployment that may serve a user region

const byText = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
const given = (value: any) => value !== undefined && value !== null;

function report(findings: Set<string>): string[] | null {
  // TODO 1 of 6 (unlocks m1): the answer of checkDeployment.
  // Receives the set of finding ids. Returns them as an array sorted by text, or null when there is nothing to report yet.
  // Example: report(new Set(["zdr-missing", "phi-no-baa"])) -> ["phi-no-baa", "zdr-missing"]
  return null;
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
  // TODO 2 of 6 (unlocks e2): the findings about zero data retention.
  // Receives the requirements and the configuration. Returns a set of finding ids. When `zdr_required` is set: `zdr-not-anthropics` on the
  // platforms `bedrock` and `vertex` (and nothing else); on the others `zdr-missing` when `zdr` is not set, and `model-needs-retention` when the
  // model name starts with `claude-fable`. Nothing is required, nothing is found.
  // Example: platform "api", zdr false, zdr_required true -> Set { "zdr-missing" }
  return new Set();
}

function phiFindings(req: any, config: any): Set<string> {
  // TODO 3 of 6 (unlocks e3): the findings about protected health information.
  // Receives the requirements and the configuration. Returns a set of finding ids. When `phi` is set: `phi-no-baa` on the platform `api` without
  // `hipaa_baa`; `phi-platform-unsupported` on `aws-platform`; `phi-not-deidentified` when `pii_handling` is not in SAFE_INPUT.
  // Example: platform "api", hipaa_baa false, pii_handling "tokenise", phi true -> Set { "phi-no-baa" }
  return new Set();
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
  // TODO 4 of 6 (unlocks e5): the finding about stored prompts.
  // Receives the requirements and the configuration; the audit settings are `config.audit` (it may be missing). Returns
  // Set { "audit-stores-sensitive" } when `store_prompts` is set and either `phi` is required or `pii_handling` is not in SAFE_INPUT; an empty set otherwise.
  // Example: store_prompts true, pii_handling "none" -> Set { "audit-stores-sensitive" }
  return new Set();
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
  // TODO 5 of 6 (unlocks e7): what a retention rule purges.
  // Receives the entries (each has `id` and an ISO `date`, and may have `hold`), the limit in days and today's ISO date. Returns
  // { purge: [ids], keep: [ids] }, both sorted by id. An entry is purged when it is older than the limit (strictly) and is not on hold.
  // Example: an entry dated 365 days before today with maxDays 365 is kept; one dated 366 days before is purged.
  return null;
}

export function pickDeployment(userRegion: string, deployments: any[]): string | null {
  // TODO 6 of 6 (unlocks e8): the deployment that may serve a user.
  // Receives the user's region ("us", "eu" or "other") and the deployments (each has `name` and `residency`). Returns the first name, in
  // alphabetical order, of the deployments whose residency is WANT[userRegion], or null when there is none (a global deployment does not serve "us" or "eu").
  // Example: "eu" with eu-main and eu-backup -> "eu-backup"
  return null;
}
