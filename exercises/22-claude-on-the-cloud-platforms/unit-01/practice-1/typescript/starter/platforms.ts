// One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md.
export type Config = { region?: string; project?: string; endpoint?: string };
export type Built = { method: string; url: string; headers: Record<string, string>; body: Record<string, any> };

/** The request cannot be built for this platform. `field` names the offending part. */
export class PlatformError extends Error {
  field: string;
  reason: string;
  constructor(field: string, reason: string) {
    super(`${field}: ${reason}`);
    this.field = field;
    this.reason = reason;
  }
}

export function buildRequest(platform: string, model: string, body: Record<string, any>, config: Config): Built {
  // TODO: return { method, url, headers, body } for the platform, or throw PlatformError.
  return undefined as unknown as Built;
}

export function unsupportedFeatures(platform: string, features: string[]): string[] {
  // TODO: return the features of the list that the platform lacks, in the order given.
  return undefined as unknown as string[];
}
