/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. */

const SAFE_INPUT = ["tokenise", "redact"]; // pii_handling values that keep identifiers out of the prompt
const WANT: Record<string, string> = { us: "us", eu: "eu", other: "global" }; // the residency of the deployment that may serve a user region

const byText = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

export function checkDeployment(config: any, req: any): string[] {
  const platform = config.platform;
  const region: string = config.region ?? "";
  const audit = config.audit ?? {};
  const findings = new Set<string>();
  if (req.residency === "us") {
    if (platform === "api" || platform === "aws-platform") {
      if (config.inference_geo !== "us") findings.add("residency-not-pinned");
    } else if (!region.startsWith("us-")) {
      findings.add("residency-region");
    }
  } else if (req.residency === "eu") {
    if (platform === "api" || platform === "aws-platform") {
      findings.add("residency-unavailable");
    } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region === "eu")) {
      findings.add("residency-region");
    }
  }
  if (req.zdr_required) {
    if (platform === "bedrock" || platform === "vertex") {
      findings.add("zdr-not-anthropics");
    } else {
      if (!config.zdr) findings.add("zdr-missing");
      if (String(config.model ?? "").startsWith("claude-fable")) findings.add("model-needs-retention");
    }
  }
  if (req.phi) {
    if (platform === "api" && !config.hipaa_baa) findings.add("phi-no-baa");
    if (platform === "aws-platform") findings.add("phi-platform-unsupported");
    if (!SAFE_INPUT.includes(config.pii_handling)) {
      findings.add("phi-not-deidentified");
    }
  }
  if (req.multi_tenant && config.tenancy !== "workspace-per-tenant") findings.add("tenant-isolation");
  if (audit.store_prompts && (req.phi || !SAFE_INPUT.includes(config.pii_handling))) findings.add("audit-stores-sensitive");
  const days = audit.retain_days;
  if (days !== undefined && days !== null) {
    if (req.audit_max_days !== undefined && req.audit_max_days !== null && days > req.audit_max_days) findings.add("retention-too-long");
    if (req.audit_min_days !== undefined && req.audit_min_days !== null && days < req.audit_min_days) findings.add("retention-too-short");
  }
  return [...findings].sort(byText);
}

const dayNumber = (iso: string): number => Math.floor(Date.parse(`${iso}T00:00:00Z`) / 86_400_000);

export function retentionActions(entries: any[], maxDays: number, today: string): { purge: string[]; keep: string[] } {
  const now = dayNumber(today);
  const purge: string[] = [];
  const keep: string[] = [];
  for (const e of [...entries].sort((a, b) => byText(a.id, b.id))) {
    const age = now - dayNumber(e.date);
    (age > maxDays && !e.hold ? purge : keep).push(e.id);
  }
  return { purge, keep };
}

export function pickDeployment(userRegion: string, deployments: any[]): string | null {
  const matching = deployments.filter((d) => d.residency === WANT[userRegion]).map((d) => d.name as string).sort(byText);
  return matching.length > 0 ? matching[0] : null;
}
