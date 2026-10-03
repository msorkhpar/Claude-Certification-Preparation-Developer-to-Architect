// Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md.

export function lintTool(tool: { [key: string]: any }): string[] | null {
  // TODO: the sorted list of rule ids this tool breaks.
  return null;
}

export function lintToolSet(tools: Array<{ [key: string]: any }>, maxTools = 20): string[][] | null {
  // TODO: sorted [tool name, rule] pairs for the whole set.
  return null;
}

export function pageResults(items: string[], cursor: string | null = null, limit = 10, maxChars = 2000): any {
  // TODO: { items, next_cursor, truncated, note }.
  return null;
}

export function effectiveHints(tool: { [key: string]: any }, trustedServer: boolean): { [key: string]: boolean } | null {
  // TODO: the four hints a client acts on.
  return null;
}

export function parallelSafe(tools: Array<{ [key: string]: any }>, trustedServers: Set<string>): string[] | null {
  // TODO: names of the tools that may run beside other read-only tools.
  return null;
}
