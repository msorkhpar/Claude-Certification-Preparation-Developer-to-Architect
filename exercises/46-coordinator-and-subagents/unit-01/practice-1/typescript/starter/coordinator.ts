// A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md.
type Finding = { scope: string; text: string };

export function coordinate(
  planner: (question: string) => any,
  subagent: (brief: string) => string,
  reviewer: (question: string, findings: Finding[]) => string[],
  synthesizer: (question: string, findings: Finding[]) => string,
  question: string,
  maxAgents = 4,
  maxRounds = 2,
): any {
  // TODO: plan, delegate one brief per subagent, review the findings for gaps, delegate the gaps, then synthesize once.
  return null;
}
