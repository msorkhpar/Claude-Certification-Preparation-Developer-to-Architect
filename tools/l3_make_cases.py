#!/usr/bin/env python3
"""Write cases.json for the practices of the Level 3 modules from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/l3_make_cases.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> {"name", "tests": {java, kotlin suite names}, "langs", "cases": [(id, kind, title)], "plants": {name: (caught_by, defect)}}
PRACTICES = {}

# --- PRACTICES BELOW ---

PRACTICES[f"{X}/45-the-agentic-loop-in-depth/unit-01/practice-1"] = {
    "name": "agent", "suite": "AgentLoopTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run alternates model and tools until the model ends its turn"),
        ("e1", "edge", "the stop reason decides and the words of the text do not"),
        ("e2", "edge", "every call of a turn is answered in one user message in order"),
        ("e3", "edge", "a failing or unknown tool becomes an error result and the run goes on"),
        ("e4", "edge", "the turn limit is a backstop that ends only a run the model has not ended"),
        ("e5", "edge", "a cut off or refused reply ends the run with its own status"),
        ("e6", "edge", "a tool use reply without a tool call is malformed and sends nothing more"),
    ],
    "plants": {
        "wrong-text-marker": (["e1"], "ends the run when the text says done, even if the reply also calls a tool"),
        "wrong-cap-reports-done": (["e4"], "reports a run that hit the turn limit as done"),
        "wrong-cap-off-by-one": (["e4"], "allows one model call more than the turn limit"),
        "wrong-results-split": (["e2"], "answers each tool call of a turn in its own user message"),
        "wrong-error-without-flag": (["e3"], "sends a failing tool's message back as an ordinary result"),
        "wrong-truncated-is-done": (["e5"], "treats a reply cut off by max_tokens as a finished one"),
        "wrong-malformed-continues": (["e6"], "sends an empty user message and calls the model again after a tool use reply with no call"),
    },
}

PRACTICES[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "name": "coordinator", "suite": "CoordinatorTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a hub sends one brief to each spoke and synthesizes what comes back"),
        ("e1", "edge", "a question the coordinator can answer itself is answered without any subagent"),
        ("e2", "edge", "a subagent sees its own brief and nothing the others found"),
        ("e3", "edge", "the plan is cleaned of empty briefs and duplicate scopes and capped"),
        ("e4", "edge", "a failing subagent does not stop the others and a run with no findings does not synthesize"),
        ("e5", "edge", "a review sends only the gaps back out and stops when none are left"),
        ("e6", "edge", "the rounds are capped and the gaps that remain are reported"),
    ],
    "plants": {
        "wrong-leaks-context": (["e2"], "appends what earlier subagents found to the brief of the next one"),
        "wrong-no-dedupe": (["e3"], "runs two subagents on the same scope"),
        "wrong-empty-brief-sent": (["e3"], "sends a subagent an empty brief"),
        "wrong-always-delegates": (["e1"], "builds a team even when the planner answered the question itself"),
        "wrong-failure-as-finding": (["e4"], "reports a subagent's error message as a finding"),
        "wrong-synthesizes-nothing": (["e4"], "synthesizes an answer when no subagent returned anything"),
        "wrong-rerun-all": (["e5"], "sends every first-round brief out again in each refinement round"),
        "wrong-rounds-off-by-one": (["e6"], "runs one refinement round more than the limit"),
    },
}

PRACTICES[f"{X}/47-invoking-subagents/unit-01/practice-1"] = {
    "name": "subagents", "suite": "SubagentsTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the options register each named agent with its description prompt tools and model"),
        ("e1", "edge", "a subagent gets read only tools by default and never the right to spawn another"),
        ("e2", "edge", "a definition with a bad name or no description or no prompt is refused"),
        ("e3", "edge", "depth concurrency budget and turn limits are set on the options"),
        ("e4", "edge", "a spawn is recognised under the new and the old tool name and nothing else is"),
        ("e5", "edge", "messages from inside a subagent are grouped under the call that started it"),
        ("e6", "edge", "a brief carries the task and every fact the subagent needs in a fixed layout"),
        ("e7", "edge", "findings keep the claim apart from its source and merging keeps every source"),
        ("e8", "edge", "a run through the sdk shows the spawn the inner messages and the agents sent to the binary"),
    ],
    "plants": {
        "wrong-inherits-all-tools": (["e1"], "leaves the tools out of a definition that lists none, so the subagent inherits every tool"),
        "wrong-allows-nesting": (["e1"], "keeps the Agent tool in a subagent's own tool list"),
        "wrong-name-unchecked": (["e2"], "accepts any agent name"),
        "wrong-no-depth-limit": (["e3"], "does not limit how deep subagents may nest"),
        "wrong-budget-dropped": (["e3"], "forgets to pass the budget limit to the options"),
        "wrong-only-new-name": (["e4"], "recognises a spawn only under the current tool name"),
        "wrong-counts-everything": (["e5"], "credits the coordinator's own messages to the first subagent"),
        "wrong-brief-drops-facts": (["e6"], "leaves the known facts out of the brief"),
        "wrong-first-source-only": (["e7"], "keeps only the first source of a merged claim"),
        "wrong-source-in-claim": (["e7"], "writes the source into the claim text"),
    },
}

PRACTICES[f"{X}/48-multi-step-workflows-with-guarantees/unit-01/practice-1"] = {
    "name": "desk", "suite": "RefundDeskTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a verified customer can look up an order and be refunded within the limit"),
        ("e1", "edge", "a refund before identity is verified is blocked in code and never reaches the backend"),
        ("e2", "edge", "an order that belongs to someone else is neither shown nor refundable"),
        ("e3", "edge", "a refund is checked against the order its amount and what is left"),
        ("e4", "edge", "a refund over the limit is not executed and becomes a structured hand off"),
        ("e5", "edge", "a failed check does not unlock anything and three in a row lock the desk"),
        ("e6", "edge", "unknown tools and backend errors are reported and the hand off lists every block"),
    ],
    "plants": {
        "wrong-identity-unchecked": (["e1"], "lets any call but verification through without a verified customer"),
        "wrong-ownership-unchecked": (["e2"], "remembers and shows an order that belongs to another customer"),
        "wrong-over-limit-executes": (["e4"], "runs a refund above the limit instead of handing it to a person"),
        "wrong-failure-unlocks": (["e5"], "keeps the earlier verified customer after a failed check"),
        "wrong-no-lockout": (["e5"], "never locks the desk after repeated failed checks"),
        "wrong-exceeds-ignored": (["e3"], "refunds more than what is left on the order"),
        "wrong-zero-amount-ok": (["e3"], "accepts a refund of zero cents"),
        "wrong-handoff-no-blocks": (["e6"], "leaves the refusals out of the hand-off"),
    },
}

PRACTICES[f"{X}/49-hooks/unit-01/practice-1"] = {
    "name": "hooks", "suite": "HooksTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the gate allows a small refund asks about a middle one and denies a large one"),
        ("e1", "edge", "each limit belongs to the lower tier and the next cent goes up"),
        ("e2", "edge", "a missing or invalid amount is denied and other tools are left alone"),
        ("e3", "edge", "the output gets a date a status word and a decimal amount"),
        ("e4", "edge", "output that is not json or is already readable is left alone"),
        ("e5", "edge", "the hooks are registered on tool name matchers with a timeout"),
        ("e6", "edge", "a command hook blocks with exit two and a reason and fails closed on bad input"),
        ("e7", "edge", "the settings block runs the command hook on bash with a timeout"),
        ("e8", "edge", "a run through the sdk denies the large refund and shows the model a readable order"),
    ],
    "plants": {
        "wrong-200-asks": (["e1"], "asks a person about a refund of exactly the automatic limit"),
        "wrong-500-denied": (["e1"], "denies a refund of exactly the upper limit"),
        "wrong-missing-amount-allowed": (["e2"], "lets a refund with no amount through"),
        "wrong-coerces-amount": (["e2"], "turns a text or a boolean amount into a number"),
        "wrong-gates-every-tool": (["e2"], "applies the refund limits to a tool that is not a refund"),
        "wrong-always-milliseconds": (["e3"], "reads every epoch as milliseconds"),
        "wrong-cents-kept": (["e3"], "leaves amount_cents beside the new amount"),
        "wrong-rewrites-plain-text": (["e4"], "answers a replacement for output that is not json"),
        "wrong-not-idempotent": (["e4"], "reports a change for output that is already readable"),
        "wrong-no-matcher": (["e5"], "registers the refund gate for every tool"),
        "wrong-no-timeout": (["e5"], "registers the hooks without a timeout"),
        "wrong-exit-one": (["e6"], "blocks with exit code 1, which does not block"),
        "wrong-bad-input-passes": (["e6"], "lets a call through when the hook input is not json"),
        "wrong-settings-no-timeout": (["e7"], "writes the settings hook without a timeout"),
    },
}

PRACTICES[f"{X}/50-task-decomposition/unit-01/practice-1"] = {
    "name": "decompose", "suite": "DecomposeTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each file is reviewed alone and the cross pass reads their summaries"),
        ("e1", "edge", "a file pass sees only its own file and the cross pass sees summaries never the text"),
        ("e2", "edge", "a long file is reviewed in labelled parts and a blank file is skipped"),
        ("e3", "edge", "a failing file is reported and left out of the cross pass which needs two files"),
        ("e4", "edge", "the planner is asked again after each step with the steps so far and the loop ends when it says done"),
        ("e5", "edge", "the loop stops on a repeated subtask or no next step or an unreadable reply and counts the step limit exactly"),
        ("e6", "edge", "the strategy follows what is known about the steps and whether the items interact"),
    ],
    "plants": {
        "wrong-shared-context": (["e1"], "gives each file pass the text of every file"),
        "wrong-cross-gets-text": (["e1"], "hands the cross pass the file text beside the summaries"),
        "wrong-no-chunking": (["e2"], "sends a long file in one piece whatever the limit"),
        "wrong-blank-reviewed": (["e2"], "reviews a file that holds only blank lines"),
        "wrong-failed-in-cross": (["e3"], "keeps a failed file among the reviewed ones"),
        "wrong-cross-with-one": (["e3"], "runs the cross pass when only one file was reviewed"),
        "wrong-no-history": (["e4"], "asks the planner without the steps done so far"),
        "wrong-repeat-allowed": (["e5"], "lets the planner repeat a subtask"),
        "wrong-limit-off-by-one": (["e5"], "runs one step more than the step limit"),
        "wrong-strategy-items-first": (["e6"], "chooses per item and cross even when the steps are not known"),
    },
}

PRACTICES[f"{X}/51-session-state/unit-01/practice-1"] = {
    "name": "sessions", "suite": "SessionsTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the plan resumes an unchanged session and tells a changed one what differs and starts fresh when most of it changed"),
        ("e1", "edge", "changed deleted and added files are listed in order and any difference asks for a notice"),
        ("e2", "edge", "more than half of the files changed or gone starts fresh and exactly half does not"),
        ("e3", "edge", "a session idle for more than a week starts fresh and exactly a week is resumed"),
        ("e4", "edge", "no saved session starts fresh and a fork is only planned from a session that is resumed"),
        ("e5", "edge", "the change notice names only what differs and the prompt puts the notice or the summary before the task"),
        ("e6", "edge", "the summary has a fixed layout with blank and repeated items dropped and files listed by path"),
        ("e7", "edge", "options resume by id and fork together and continue is refused unless one session exists and a name must be unique"),
        ("e8", "edge", "a run through the sdk passes resume and fork to the binary and returns the session id even after an error"),
    ],
    "plants": {
        "wrong-ignores-changes": (["m1"], "resumes a session without a notice even when files changed"),
        "wrong-added-ignored": (["e1"], "does not count a new file as a difference"),
        "wrong-half-is-fresh": (["e2"], "starts fresh when exactly half of the files changed"),
        "wrong-added-counted": (["e2"], "counts new files toward the share of changed files"),
        "wrong-age-ignored": (["e3"], "resumes a session however long it has been idle"),
        "wrong-age-inclusive": (["e3"], "starts fresh when the session was idle for exactly a week"),
        "wrong-fork-without-session": (["e4"], "plans a fork even when the session is not resumed"),
        "wrong-notice-when-nothing": (["e5"], "writes a notice even when no file differs"),
        "wrong-summary-after-task": (["e5"], "puts the summary after the task"),
        "wrong-summary-keeps-repeats": (["e6"], "keeps repeated items in the summary"),
        "wrong-files-unsorted": (["e6"], "lists the files in the order they came"),
        "wrong-fork-alone": (["e7"], "sets the fork option even when there is no session to resume"),
        "wrong-continue-many": (["e7"], "continues the latest session when several exist"),
        "wrong-name-first-match": (["e7"], "resumes the first of several sessions that share a name"),
        "wrong-id-only-on-success": (["e8"], "reads the session id only from a successful result"),
        "wrong-no-fork-flag": (["e7", "e8"], "writes the fork option as false"),
    },
}

PRACTICES[f"{X}/53-tool-errors-agents-can-act-on/unit-01/practice-1"] = {
    "name": "errors", "suite": "ErrorsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a failed call becomes a structured error with a category a retry flag and an error flag"),
        ("e1", "edge", "a generic message or an unknown category is refused"),
        ("e2", "edge", "only transient failures are retried with growing waits and the other kinds return at once"),
        ("e3", "edge", "the retries are bounded and a wait the service asks for is honoured"),
        ("e4", "edge", "a valid empty result is a success and not an error"),
        ("e5", "edge", "a timeout on a write is an unknown outcome and is retried only when repeating it is safe"),
        ("e6", "edge", "the next action follows the category"),
        ("e7", "edge", "an unexpected exception becomes an internal error and the run goes on"),
    ],
    "plants": {
        "wrong-flag-missing": (["m1"], "reports a failed call as a tool result without the error flag"),
        "wrong-business-retryable": (["m1"], "marks a business rule violation as worth retrying"),
        "wrong-generic-message": (["e1"], "accepts a message such as Operation failed"),
        "wrong-unknown-kind-accepted": (["e1"], "accepts a category that is not one of the six"),
        "wrong-retries-validation": (["e2"], "retries a call that failed validation"),
        "wrong-flat-wait": (["e2"], "waits the same time before every retry"),
        "wrong-extra-retry": (["e3"], "makes one attempt more than the retry limit allows"),
        "wrong-ignores-retry-after": (["e3"], "sleeps its own backoff when the service named a wait"),
        "wrong-empty-is-error": (["e4"], "reports a valid empty result as a failure"),
        "wrong-timeout-retried": (["e5"], "retries a timed out write that has no key"),
        "wrong-key-dropped": (["e5"], "leaves the idempotency key out of the arguments it retries with"),
        "wrong-mutates-args": (["e5"], "writes the idempotency key into the caller's own arguments"),
        "wrong-permission-retry-later": (["e6"], "tells the loop to retry later after a permission error"),
        "wrong-internal-retryable": (["e7"], "marks an unexpected failure as retryable"),
    },
}

PRACTICES[f"{X}/55-mcp-in-claude-code/unit-01/practice-1"] = {
    "name": "mcp_setup", "suite": "McpSetupTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the shared file declares the four team servers with the right shape"),
        ("e1", "edge", "credentials are read from the environment and never written in the file"),
        ("e2", "edge", "endpoints and paths that are not secret have a default so the file works unset"),
        ("e3", "edge", "only the small core server is loaded at the start"),
        ("e4", "edge", "shared servers stay in the project file and personal ones in the user scope file"),
        ("e5", "edge", "permissions allow the read only servers by name and deny the destructive tool"),
        ("e6", "edge", "the tool description says when to use it instead of grep and fits the limit"),
        ("e7", "edge", "the notes list every server with its scope and read the catalog as a resource"),
        ("e8", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-github-type": (["m1"], "declares the GitHub server with a transport that is not http"),
        "wrong-docs-no-command": (["m1"], "declares the local docs server without a command"),
        "wrong-literal-token": (["e1"], "writes the GitHub token into the file"),
        "wrong-covered-credential": (["e1"], "sends the token under a name Claude Code blanks out toward a remote server"),
        "wrong-secret-default": (["e1"], "gives the documentation key a default value in the file"),
        "wrong-url-no-default": (["e2"], "leaves the GitHub URL without a default"),
        "wrong-project-dir-no-default": (["e2"], "uses the project directory variable without a default in a path"),
        "wrong-always-load-all": (["e3"], "loads the documentation server at the start as well"),
        "wrong-core-deferred": (["e3"], "leaves the core server to be found on demand"),
        "wrong-personal-in-shared": (["e4"], "puts the personal scratch server into the committed file"),
        "wrong-user-duplicates": (["e4"], "gives the user scope file a server with a shared name"),
        "wrong-allow-unanchored": (["e5"], "allows every MCP tool with a rule that names no server"),
        "wrong-allow-github": (["e5"], "allows every GitHub tool without asking"),
        "wrong-no-delete-deny": (["e5"], "drops the denial of the destructive GitHub tool"),
        "wrong-description-long": (["e6"], "writes a description longer than Claude Code keeps"),
        "wrong-boundary-buried": (["e6"], "puts the boundary against Grep after the first 300 characters"),
        "wrong-no-resource-ref": (["e7"], "shows an @ mention that names a server that is not configured"),
        "wrong-schema-as-tool": (["e7"], "describes the schema server as a tool instead of a catalog of resources"),
        "wrong-scope-user": (["e7"], "gives a shared server the scope user in the notes"),
        "wrong-home-path": (["e8"], "writes a personal home path into the notes"),
    },
}

PRACTICES[f"{X}/56-the-built-in-tools/unit-01/practice-1"] = {
    "name": "explorer_setup", "suite": "ExplorerSetupTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the permission rules let an explorer read search and take notes but not change the source"),
        ("e1", "edge", "a read rule protects secrets from reading searching and writing"),
        ("e2", "edge", "only rule forms that are consulted are used and no whole tool that changes things is allowed"),
        ("e3", "edge", "the explorer agent reads and searches and says when to use it"),
        ("e4", "edge", "the sdk options make the search tools available and remove the tools that change things"),
        ("e5", "edge", "the exploration plan starts with a search then reads and never reads everything first"),
        ("e6", "edge", "the edit fallback widens the anchor then replaces all then rewrites the file"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-no-src-deny": (["m1"], "drops the denial of edits under src"),
        "wrong-bash-bare": (["m1", "e2"], "allows the whole Bash tool instead of read-only git patterns"),
        "wrong-notes-everywhere": (["m1"], "allows edits to every path instead of notes only"),
        "wrong-write-rule": (["m1", "e2"], "writes the notes rule for the Write tool, whose path rules are never matched"),
        "wrong-no-env-deny": (["e1"], "drops the denial of reading the environment file"),
        "wrong-grep-rule": (["e1", "e2"], "writes the secrets rule for Grep instead of Read"),
        "wrong-agent-edit": (["e3"], "gives the explorer the Edit tool"),
        "wrong-agent-bash": (["e3"], "gives the explorer the Bash tool"),
        "wrong-agent-no-use-when": (["e3"], "leaves the when-to-use sentence out of the description"),
        "wrong-agent-unbounded": (["e3"], "leaves the turn limit out of the agent"),
        "wrong-options-no-search": (["e4"], "lists only Read in tools, so the search tools are missing"),
        "wrong-options-allow-bash": (["e4"], "pre-approves Bash in allowedTools"),
        "wrong-options-no-disallow": (["e4"], "removes only Bash and leaves Edit and Write in the context"),
        "wrong-plan-read-first": (["e5"], "reads the entry points before any search"),
        "wrong-plan-read-all": (["e5"], "starts by reading every file"),
        "wrong-no-export-step": (["e5"], "traces usage by function name without listing exported names"),
        "wrong-fallback-rewrite-first": (["e6"], "rewrites the whole file before widening the anchor"),
        "wrong-fallback-no-replace-all": (["e6"], "leaves replace_all out of the remedies"),
        "wrong-home-path": (["e7"], "writes a personal home path into the plan"),
    },
}

PRACTICES[f"{X}/54-distributing-tools-across-agents/unit-01/practice-1"] = {
    "name": "distribute", "suite": "DistributeTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each role gets only the tools of its specialisation in catalog order"),
        ("e1", "edge", "a role over its budget or given an unknown or unscoped outside tool or a duplicate catalog name is refused"),
        ("e2", "edge", "an irreversible tool is given only by an explicit grant"),
        ("e3", "edge", "a model that accepts forcing gets the native choice and the others get auto with strict tools and one named tool"),
        ("e4", "edge", "a change of tool choice costs the cached messages a change of tools costs everything and a repeat costs nothing"),
        ("e5", "edge", "a reply is checked against the call that was required"),
        ("e6", "edge", "an unknown tool a wrong owner or a bad amount is refused"),
        ("e7", "edge", "the cap is inclusive and an irreversible call needs approval that never lifts the cap"),
    ],
    "plants": {
        "wrong-sorted-names": (["m1"], "lists a role's tools alphabetically instead of in catalog order"),
        "wrong-no-budget": (["e1"], "lets a role have more tools than the budget"),
        "wrong-unscoped-extra": (["e1"], "grants an outside tool that is not marked as a scoped cross-role tool"),
        "wrong-duplicates-ok": (["e1"], "accepts two catalog entries with the same name"),
        "wrong-irreversible-by-tag": (["e2"], "gives an irreversible tool to every role whose specialisation matches"),
        "wrong-forces-everywhere": (["e3"], "forces the tool choice on models that reject it"),
        "wrong-fallback-all-tools": (["e3"], "offers every tool in the fallback for a named first tool"),
        "wrong-fallback-no-verify": (["e3"], "does not ask for the reply to be checked in the fallback"),
        "wrong-tools-ignored": (["e4"], "does not count a narrower tool list as a change that costs the whole cache"),
        "wrong-choice-ignored": (["e4"], "does not count a changed tool choice as a cost to the cached messages"),
        "wrong-repeat-costs": (["e4"], "reports a cost for a request that repeats the previous one"),
        "wrong-missed-call-ok": (["e5"], "accepts a reply with no tool call when one was required"),
        "wrong-any-tool-counts": (["e5"], "accepts any tool when a particular one was required first"),
        "wrong-default-allow": (["e6"], "allows a tool that the policy does not name"),
        "wrong-owner-unchecked": (["e6"], "does not compare the customer with the verified one"),
        "wrong-cap-exclusive": (["e7"], "refuses an amount equal to the cap"),
        "wrong-approval-lifts-cap": (["e7"], "lets an approval allow an amount above the cap"),
        "wrong-approval-skipped": (["e7"], "allows an irreversible call without an approval"),
    },
}

PRACTICES[f"{X}/52-designing-tool-interfaces/unit-01/practice-1"] = {
    "name": "toolset", "suite": "ToolsetTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a well made tool lints clean and a poor one is named for every rule it breaks"),
        ("e1", "edge", "names must match the pattern and a vague name is flagged"),
        ("e2", "edge", "a description needs three sentences a when to use phrase and a boundary against the neighbour"),
        ("e3", "edge", "parameters are described and required names exist and closed sets are enums and examples fit the schema"),
        ("e4", "edge", "a list tool needs a limit and a cursor and a hint may not contradict the name"),
        ("e5", "edge", "a set is graded for duplicate names overlapping descriptions and size"),
        ("e6", "edge", "pages carry an opaque cursor and a clamped limit and a note"),
        ("e7", "edge", "a page stops at the size cap and says so but always carries one item"),
        ("e8", "edge", "a tools own hints are trusted only from a trusted server and the defaults apply otherwise"),
    ],
    "plants": {
        "wrong-name-spaces-allowed": (["e1"], "accepts a name with spaces, dots or a hash sign"),
        "wrong-vague-case-sensitive": (["e1"], "flags a vague name only when it is written in lower case"),
        "wrong-two-sentences-ok": (["e2"], "accepts a description of two sentences"),
        "wrong-boundary-do-not-only": (["e2"], "accepts only the words do not use as a boundary against a neighbouring tool"),
        "wrong-required-unchecked": (["e3"], "does not notice a required name that is not a property"),
        "wrong-example-enum-ignored": (["e3"], "accepts an example whose value is outside the enum"),
        "wrong-bool-is-integer": (["e3"], "accepts true as an integer example"),
        "wrong-undescribed-ok": (["m1", "e3"], "does not notice a parameter without a description"),
        "wrong-limit-only": (["e4"], "asks a list tool for a limit but not for a cursor"),
        "wrong-hint-readonly-only": (["e4"], "checks the read-only hint against the name but not the destructive hint"),
        "wrong-duplicate-allowed": (["e5"], "lets two tools share a name"),
        "wrong-overlap-strict": (["e5"], "does not flag descriptions that overlap by exactly the threshold"),
        "wrong-count-off-by-one": (["e5"], "flags a set that is exactly as large as the budget"),
        "wrong-limit-unclamped": (["e6"], "lets the limit grow beyond the maximum page size"),
        "wrong-no-note": (["e6"], "leaves out the note that tells the model how to continue"),
        "wrong-cursor-unchecked": (["e6"], "starts from the beginning when the cursor is not valid"),
        "wrong-cap-exclusive": (["e7"], "stops one item early when a page fills the size cap exactly"),
        "wrong-truncated-never": (["e7"], "never reports that a page was cut by the size cap"),
        "wrong-untrusted-honoured": (["e8"], "reads the hints of a server that is not trusted"),
        "wrong-parallel-untrusted": (["e8"], "lets a tool of an untrusted server run in parallel on its own say so"),
    },
}

# --- PRACTICES ABOVE ---


def camel(title):
    words = title.split()
    return words[0] + "".join(w[:1].upper() + w[1:] for w in words[1:])


def main():
    problems = 0
    for practice, spec in PRACTICES.items():
        base = ROOT / practice
        cases, tests = {}, {}
        for cid, kind, title in spec["cases"]:
            cases[cid] = {"kind": kind, "python": f"test_{cid}_" + title.replace(" ", "_"), "typescript": f"{cid} {title}",
                          "java": f"{cid}_{camel(title)}", "kotlin": f"{cid}_{camel(title)}"}
        data = {"practice": practice.split(f"{X}/", 1)[1], "name": spec["name"], "tests": {"java": spec["suite"], "kotlin": spec["suite"]},
                "cases": {cid: {k: v for k, v in c.items() if k in ("kind",) + tuple(spec["langs"])} for cid, c in cases.items()},
                "plants": {n: {"caught_by": caught, "defect": defect} for n, (caught, defect) in spec["plants"].items()}}
        if spec["langs"] == ["python", "typescript"]:
            data.pop("tests")
        elif spec["langs"] != ["python", "typescript", "java", "kotlin"]:
            sys.exit(f"{practice}: unsupported languages {spec['langs']}")
        (base / "cases.json").write_text(json.dumps(data, indent=2) + "\n")
        for lang in spec["langs"]:
            found = {"python": next(base.glob("python/tests/*.py"), None), "typescript": next(base.glob("typescript/tests/*.test.ts"), None),
                     "java": next(base.glob("java/tests/*Test.java"), None), "kotlin": next(base.glob("kotlin/tests/*Test.kt"), None)}[lang]
            text = found.read_text() if found else ""
            for cid, c in cases.items():
                name = c[lang]
                ok = (f"def {name}(" in text) if lang == "python" else (f'test("{name}"' in text) if lang == "typescript" else (f"void {name}(" in text or f"fun {name}(" in text)
                if not ok:
                    print(f"{practice}/{lang}: test {name!r} not found")
                    problems += 1
        print(f"{practice}: cases.json written ({len(cases)} cases, {len(spec['plants'])} plants)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
