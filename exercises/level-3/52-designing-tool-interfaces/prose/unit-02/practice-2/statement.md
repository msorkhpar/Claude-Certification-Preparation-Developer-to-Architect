# Practice: grade a tool set on schema rules

The model chooses a tool from its name, its description and its schema, so those are the interface, and they can be checked like code. This practice has you
write the grader: a linter for one tool and for a whole set, a pager for a result that is too long, and the rule for how far a tool's own annotations may be
trusted. The rules are the course's own, built on the documented guidance (a description of at least three or four sentences, when to use the tool and when not,
a description on every parameter, valid examples, namespacing, a bounded list); their thresholds are stated below so the tests can pin them. The model is never called. It is in
Python, TypeScript, Java and Kotlin;

Names are Python's (`lint_tool`, `lint_tool_set`, `page_results`, `effective_hints`, `parallel_safe`); TypeScript has `lintTool`, `lintToolSet`, `pageResults`, `effectiveHints`
and `parallelSafe` (arguments `maxTools`, `maxChars`); Java has `Toolset.lintTool` and so on, with overloads for the defaults; Kotlin has top-level functions with default
arguments. A tool is a map: `name`, `description`, `input_schema` (`properties`, each with `type`, `description` and optionally `enum`; and `required`), optionally
`input_examples` (a list of example inputs), `annotations` (a map of Boolean hints) and, for a tool that came from a server, `server`.

## What is already written, and what you write

The starter is a working grader with eight gaps cut out of it. Everything that is plumbing is written and correct: the constants, the type check
for examples, the word extraction, the cursor encoding and decoding, the paging loop and its note, the pairing and sorting of a tool set, the
hint defaults and `parallel_safe`, and the rules that are not listed below. Each gap is a small function with its signature, a comment that
says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the
cases on an assertion. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `_name_rules` unlocks `e1`: `bad-name` and `vague-name`.
2. `_description_rules` unlocks `e2`: `short-description` and `no-boundary`.
3. `_parameter_rules` unlocks `e3`: `param-undescribed` and `required-unknown`.
4. `_list_and_hint_rules` unlocks `e4`: `list-unbounded` and `hint-contradicts-name`.
5. `_similar` unlocks `e5`: the overlap threshold of two descriptions.
6. `_check_limit` unlocks `e6`: the limit that is refused below 1 and cut to 50.
7. `_over_cap` unlocks `e7`: when the size cap ends a page, and the first item that is always taken.
8. `effective_hints` unlocks `e8`: a tool's own hints count only from a trusted server.

`m1` needs the first four. About twenty lines in all. The steps below describe the whole grader, so you can see how your functions are used.

## What to write

### Step 1: `lint_tool(tool)`

Return the sorted list of rule ids the tool breaks, without repeats; `[]` for a clean tool.

| Rule | A tool breaks it when |
|---|---|
| `bad-name` | the name does not match `[A-Za-z0-9_-]{1,128}` in full |
| `vague-name` | the name, in lower case, is one of `tool`, `helper`, `do`, `run`, `process`, `handle`, `data`, `util`, `utils`, `query` |
| `short-description` | the description has fewer than 3 sentences, a sentence ending at `.`, `!` or `?` followed by white space or the end |
| `no-use-when` | the lower-cased description lacks `use when` |
| `no-boundary` | it lacks all of `do not use`, `not for` and `instead of` (the boundary against the neighbouring tool) |
| `param-undescribed` | a property has no description, or a blank one |
| `required-unknown` | `required` names something that is not a property |
| `open-set` | a string property without an `enum` has a description containing `one of` or `either` |
| `reasoning-param` | a property name or description contains `reasoning` or `thinking` |
| `bad-example` | an `input_examples` entry is not a map, lacks a required key, has a key that is not a property, has a value of the wrong type (`string`, `integer` without booleans, `number`, `boolean`, `array`, `object`) or outside the property's `enum` |
| `list-unbounded` | the name starts with `list_`, `search_` or `find_` and the properties lack `limit` or lack `cursor` |
| `hint-contradicts-name` | `readOnlyHint` is true and the name starts with `create_`, `update_`, `delete_`, `remove_`, `send_` or `write_`, or `destructiveHint` is false and the name starts with `delete_` or `remove_` |

**Why the exam cares.** The guide says tool descriptions are the primary mechanism for tool selection, and asks for input formats, example queries, edge cases and
boundaries against similar tools. A rule that a script can check is a rule a team can keep. **What you should see when it works:** a tool of four sentences with a
when-to-use phrase, a boundary, described parameters and valid examples is clean; a tool called `helper` with the description "Gets stuff." is named for five rules.

### Step 2: `lint_tool_set(tools, max_tools=20)`

Return sorted `[tool name, rule]` pairs: every pair `lint_tool` finds, plus `duplicate-name` (once per name used by more than one tool), `overlap:<other name>` on both
tools of a pair with different names whose descriptions share at least 0.6 of their words (the words are the runs of three or more letters a to z in the lower-cased
description; the share is the size of the common words over the size of all the words), and `["*", "too-many-tools"]` when the set has more than `max_tools` tools. Sort by name,
then by rule.

**Why the exam cares.** Overlapping descriptions cause misrouting (the guide's `analyze_content` and `analyze_document`), and the documentation says that tool selection
accuracy degrades with more than 30 to 50 tools loaded at once. **What you should see when it works:** two tools with the same description point at each other, two with
different ones do not, and a set at exactly the budget is not too large.

### Step 3: `page_results(items, cursor=None, limit=10, max_chars=2000)`

`items` is a list of text. Return `{"items", "next_cursor", "truncated", "note"}`.

- `limit` must be a whole number of at least 1, else raise `ValueError` (TypeScript: an `Error`; Java and Kotlin: `IllegalArgumentException`); a limit above 50 is cut to 50.
- The cursor is opaque: build it from the offset of the first item not returned (the reference encodes `offset:<n>` in base64, and any encoding that hides the number
  passes). A missing cursor starts at the beginning; a cursor that does not decode to an offset between 0 and the number of items is refused as above.
- Take up to `limit` items from the offset, but stop before the item that would push the total length of the page over `max_chars`; the first item is always taken, however long.
  `truncated` is true when the size cap made the page shorter than the limit and the items left allowed.
- `next_cursor` is null on the last page. `note` is null then too; otherwise it is `Showing N of M results; pass next_cursor to continue, or narrow the query with a filter.`

**Why the exam cares.** A tool that returns everything fills the context; the documentation advises pagination, filtering and truncation with sensible defaults, and a
truncation message that steers the agent. **What you should see when it works:** 25 rows come back in pages of 10, 10 and 5; the last page has no cursor and no note;
a limit of 500 returns at most 50; and a page that hit the size cap says so.

### Step 4: `effective_hints(tool, trusted_server)` and `parallel_safe(tools, trusted_servers)`

- `effective_hints` returns the four hints `readOnlyHint` (default false), `destructiveHint` (true), `idempotentHint` (false) and `openWorldHint` (true). The tool's own
  Boolean values replace the defaults only when its server is trusted; values that are not Booleans are ignored.
- `parallel_safe` returns the names of the tools whose effective `readOnlyHint` is true, taking each tool's `server` and the set of trusted servers, in the order given.

**Why the exam cares.** The MCP specification says that clients "MUST consider tool annotations to be untrusted unless they come from trusted servers", and the Agent SDK uses
the read-only hint only to decide which tools may run side by side. A hint is a claim, not a control. **What you should see when it works:** a read-only hint from an
unknown server changes nothing, and the same hint from a trusted server lets the tool run in parallel.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A well made tool is clean and a poor one is named for every rule it breaks |
| `e1` | Names must match the pattern, and a vague name is flagged in any case |
| `e2` | A description needs three sentences, a when-to-use phrase and a boundary |
| `e3` | Parameters are described, required names exist, closed sets are enums, examples fit the schema |
| `e4` | A list tool needs a limit and a cursor, and a hint may not contradict the name |
| `e5` | A set is graded for duplicate names, overlapping descriptions at the threshold, and size at the budget |
| `e6` | Pages carry an opaque cursor, a clamped limit and a note, and refuse a bad limit or cursor |
| `e7` | A page stops at the size cap, says so, and always carries one item |
| `e8` | A tool's own hints count only from a trusted server |
