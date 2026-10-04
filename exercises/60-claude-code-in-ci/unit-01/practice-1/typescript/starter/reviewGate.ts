/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
import { schemaCheck } from "./schemaCheck.ts"; // provided: errors of a value against a schema

export const SEVERITIES = ["low", "medium", "high"];
void schemaCheck;

export function reviewPrompt(diff: string, prior: any[] = [], existingTests: string[] = []): string | null {
  // TODO: instructions, the findings already reported, the tests that exist, and the diff last.
  return null;
}

export function gate(stdout: string, exitCode: number, schema: any, policy: any): any {
  // TODO: decide the job from the exit status and the JSON the run printed: { exit, comments, problems }.
  return null;
}
