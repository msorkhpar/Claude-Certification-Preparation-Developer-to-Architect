// Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md.
export async function preRefund(input: any, toolUseId?: string, context?: unknown): Promise<any> {
  // TODO: allow small refunds, ask for middle ones, deny large ones, and fail closed on a bad amount.
  return null;
}

export async function postNormalise(input: any, toolUseId?: string, context?: unknown): Promise<any> {
  // TODO: replace epoch seconds, numeric status codes and cents with values the model can read.
  return null;
}

export function buildOptions(cwd: string, cliPath?: string): any {
  // TODO: register both hooks with matchers that name tools.
  return null;
}

export function commandHook(stdinText: string): any {
  // TODO: decide what a command hook returns for the input it receives: exit code and stderr.
  return null;
}

export function settingsHooks(script = "python3 .claude/hooks/guard.py", timeout = 10): any {
  // TODO: the hooks block of settings.json for the command hook.
  return null;
}
