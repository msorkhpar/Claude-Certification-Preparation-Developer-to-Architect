// The loop around the computer use tool, against a toy screen. See ../../statement.md.
export const TOOLSET = "computer_toolset_20260801";
export const CLICKS = ["left_click", "right_click", "middle_click", "double_click", "triple_click"];
export const NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed.";

export type Screen = { width: number; height: number; cursor: number[]; typed: string; log: unknown[][]; elements: { id: string; x: number; y: number; w: number; h: number; risk: string }[] };

/** Given: the screenshot of the toy screen at `width` x `height`, as the base64 text of a picture. It changes after every logged action. */
export function render(screen: Screen, width: number, height: number): string {
  return `png:${width}x${height}:${screen.log.length}`;
}

export function scaleFor(width: number, height: number): any {
  // TODO: the factor that shrinks a screen to what the model may be sent: min(1, 1568 / longest side, sqrt(1,150,000 / pixels)).
  return null;
}

export function scaledSize(width: number, height: number): any {
  // TODO: [trunc(width * scale), trunc(height * scale)].
  return null;
}

export function toScreen(x: number, y: number, scale: number, screen: Screen): any {
  // TODO: a point on the scaled screenshot as a point on the real screen: divide by the scale, round (half to even), clamp into the screen.
  return null;
}

export function perform(screen: Screen, name: string, args: Record<string, any>, scale: number, confirm?: ((action: Record<string, unknown>) => boolean) | null): any {
  // TODO: run one action on the toy screen and return [content, isError]. See the statement for every action.
  return null;
}

export function pruneScreenshots(messages: any[], keep = 3): any {
  // TODO: a copy of the conversation in which every screenshot except the newest `keep` is replaced by a text note.
  return null;
}

export function runComputerLoop(ask: (request: any) => any, screen: Screen, model = "claude-sonnet-5-5", maxTurns = 10, confirm?: ((action: Record<string, unknown>) => boolean) | null): any {
  // TODO: call the model with the computer toolset, run its actions, send the results back, until it ends or a limit is hit.
  return null;
}
