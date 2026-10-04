/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. */

const SAFE_INPUT = ["tokenise", "redact"]; // pii_handling values that keep identifiers out of the prompt
const WANT: Record<string, string> = { us: "us", eu: "eu", other: "global" }; // the residency of the deployment that may serve a user region

export function checkDeployment(config: any, req: any): string[] | null {
  // TODO: the sorted list of finding ids for a deployment configuration against the requirements.
  return null;
}

export function retentionActions(entries: any[], maxDays: number, today: string): { purge: string[]; keep: string[] } | null {
  // TODO: { purge, keep }, both sorted by id.
  return null;
}

export function pickDeployment(userRegion: string, deployments: any[]): string | null {
  // TODO: the name of the deployment that may serve the user's region, or null.
  return null;
}
