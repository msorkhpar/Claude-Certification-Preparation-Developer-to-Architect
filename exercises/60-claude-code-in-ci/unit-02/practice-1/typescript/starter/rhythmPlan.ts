/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */

export function choose(job: Record<string, any>): { mechanism: string; reason: string; interval_minutes: number } {
  // TODO: the mechanism, the reason code and the interval in minutes for a job (an object whose missing keys take the defaults in the statement).
  return { mechanism: "", reason: "", interval_minutes: 0 };
}
