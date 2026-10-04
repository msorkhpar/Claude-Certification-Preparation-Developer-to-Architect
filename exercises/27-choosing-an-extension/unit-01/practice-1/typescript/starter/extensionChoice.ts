/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

export function choose(s: Record<string, any>): { mechanism: string; reason: string } {
  // TODO: the mechanism and the reason code for a situation (an object whose missing keys take the defaults in the statement).
  return null as any;
}
