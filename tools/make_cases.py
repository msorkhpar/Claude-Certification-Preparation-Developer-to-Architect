#!/usr/bin/env python3
"""Write cases.json for the practices of modules 30 to 35, 38 to 43 and 45 to 56 from one specification, and check that
every test the file names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
The cases.json of modules 6 and 13 to 29 is written by hand and has no entry here.
usage: tools/make_cases.py [--modules REGEX]
  --modules REGEX   only the practices whose module folder name matches (default: all), for example '^(3[6-9]|4[01])-'
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> {"name", "tests": {java, kotlin suite names}, "langs", "cases": [(id, kind, title)], "plants": {name: (caught_by, defect)}}
PRACTICES = {}

# ===== Level 2: modules 30 to 35 =====
PRACTICES[f"{X}/30-vision-and-documents/unit-01/practice-1"] = {
    "name": "vision", "suite": "VisionTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_vision.py", "tsfile": "vision.test.ts",
    "cases": [
        ("m1", "main", "images come first with labels and the question last"),
        ("e1", "edge", "the token cost follows the models resolution tier"),
        ("e2", "edge", "formats dimensions sizes and counts are checked before any call"),
        ("e3", "edge", "more than twenty images lower the side limit to 2000 pixels"),
        ("e4", "edge", "an image whose size matters is rejected instead of resized"),
        ("e5", "edge", "pdfs become document blocks in order and are limited by pages"),
        ("e6", "edge", "bedrock and vertex take only base64 sources and smaller images"),
        ("e7", "edge", "coordinates map back to the original and cost follows the price"),
    ],
    "plants": {
        "wrong-edge-only-resize": (["e1"], "sizes an image by the edge limit alone and ignores the visual token budget"),
        "wrong-text-first": (["m1"], "puts the question before the images"),
        "wrong-no-labels": (["m1"], "does not label the images when there are several"),
        "wrong-padded-coordinates": (["e7"], "divides a returned coordinate by the padded height instead of the resized height"),
        "wrong-same-limit-all-models": (["e2"], "allows 600 images for every model, including the 200k-context one"),
        "wrong-many-image-rule": (["e3"], "ignores the 2000 pixel limit that applies above 20 images"),
        "wrong-exact-silent": (["e4"], "lets an oversized image through even when its exact size matters"),
    },
}

PRACTICES[f"{X}/31-computer-use/unit-01/practice-1"] = {
    "name": "computer", "suite": "ComputerTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the loop runs scaled actions and sends every result in one message"),
        ("e1", "edge", "the screen is scaled to what the model may see and clicks are scaled back"),
        ("e2", "edge", "a click on a risky element needs a persons confirmation"),
        ("e3", "edge", "a failed action stops the rest of its batch"),
        ("e4", "edge", "invalid actions come back as error results with a reason"),
        ("e5", "edge", "the loop ends on a stop reason or at the turn limit"),
        ("e6", "edge", "old screenshots are replaced so the context does not fill up"),
        ("e7", "edge", "screenshots and zooms are sent at the size the model may see"),
    ],
    "plants": {
        "wrong-unscaled-click": (["m1"], "uses the model's coordinates as screen coordinates without scaling them back"),
        "wrong-no-confirm": (["e2"], "clicks a payment element without asking a person"),
        "wrong-continue-after-failure": (["e3"], "keeps running the actions of a batch after one has failed"),
        "wrong-result-per-message": (["m1"], "sends each tool result in a user message of its own"),
        "wrong-screenshot-full-size": (["e7"], "sends the screenshot at the full screen size instead of the scaled size"),
        "wrong-prune-oldest-kept": (["e6"], "keeps the oldest screenshots and removes the newest"),
        "wrong-extra-turn": (["e5"], "makes one model call more than the turn limit"),
    },
}

PRACTICES[f"{X}/34-workflows-and-agents/unit-01/practice-1"] = {
    "name": "workflows", "suite": "WorkflowsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an orchestrator plans runs a worker per subtask and combines"),
        ("e1", "edge", "the plan is read from prose cleaned capped and replaced when unusable"),
        ("e2", "edge", "one failing worker does not stop the others or the answer"),
        ("e3", "edge", "a draft is revised with the feedback until the judge accepts it"),
        ("e4", "edge", "when the rounds run out the best draft wins and an unreadable judge scores zero"),
        ("e5", "edge", "a label is read from the reply and anything else takes the default route"),
        ("e6", "edge", "the majority answer wins and a tie goes to the one seen first"),
        ("e7", "edge", "a writer that fails ends the loop with the best draft so far"),
    ],
    "plants": {
        "wrong-no-cap": (["e1"], "runs every subtask the planner lists instead of the first max_subtasks"),
        "wrong-stop-at-first-failure": (["e2"], "stops running workers after the first one fails"),
        "wrong-no-feedback": (["e3"], "revises without sending the judge's feedback to the writer"),
        "wrong-last-draft": (["e4"], "returns the last draft instead of the best one when the rounds run out"),
        "wrong-trust-unreadable-judge": (["e4"], "scores a judge reply it cannot read as a pass"),
        "wrong-punctuation-kept": (["e5"], "does not strip punctuation around the label, so a reply of Billing. misses its route"),
        "wrong-tie-goes-last": (["e6"], "breaks a voting tie in favour of the answer seen last"),
    },
}

PRACTICES[f"{X}/33-mcp-advanced/unit-01/practice-1"] = {
    "name": "mrtr", "suite": "MrtrTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a production deploy takes three round trips and keeps no state"),
        ("e1", "edge", "a staging deploy and a status call finish at once and the tool list is cacheable"),
        ("e2", "edge", "the server only asks what the client declared it can answer"),
        ("e3", "edge", "a no or a missing answer is handled without an error"),
        ("e4", "edge", "a state the server did not sign is refused"),
        ("e5", "edge", "a state works only for the same user the same call and before it expires"),
        ("e6", "edge", "a request the server cannot serve is a protocol error with a code"),
        ("e7", "edge", "the state carries the whole context and an answer alone never skips a step"),
    ],
    "plants": {
        "wrong-no-expiry": (["e5"], "accepts a requestState after its expiry time"),
        "wrong-any-principal": (["e5"], "accepts a requestState that was issued to another user"),
        "wrong-no-digest": (["e5"], "accepts a requestState on a call whose arguments differ from the ones it was issued for"),
        "wrong-bad-state-restarts": (["e4"], "silently starts the flow over when the requestState is not one it signed"),
        "wrong-elicit-without-capability": (["e2"], "sends an elicitation request to a client that did not declare the capability"),
        "wrong-decline-continues": (["e3"], "carries on with the deployment after the user declined"),
        "wrong-error-on-missing-input": (["e3"], "answers a retry that lacks the requested input with a protocol error instead of asking again"),
    },
}

PRACTICES[f"{X}/35-the-claude-agent-sdk/unit-01/practice-1"] = {
    "name": "agent", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "every tool call goes through the permission callback and the run is summarised"),
        ("e1", "edge", "the options reach the cli as flags and the run starts in the project"),
        ("e2", "edge", "file tools stay inside the project and away from secrets"),
        ("e3", "edge", "bash is limited to a few commands and dangerous ones stop the run"),
        ("e4", "edge", "a hook blocks a push before the permission callback is asked"),
        ("e5", "edge", "the messages of a run fold into a summary"),
        ("e6", "edge", "an error result still gives its summary and a crash is not hidden"),
        ("e7", "edge", "denied calls are counted and the run still ends with a result"),
    ],
    "plants": {
        "wrong-auto-approve": (["e1"], "lists the read tools in allowed_tools, so they run without the permission callback ever being asked"),
        "wrong-edit-in-readonly": (["e2"], "lets Write and Edit through in read-only mode"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or other .env files"),
        "wrong-chaining-allowed": (["e3"], "allows a command that chains another with a semicolon or a pipe when it starts with a safe word"),
        "wrong-danger-no-interrupt": (["e3"], "denies sudo and rm -rf without interrupting the run"),
        "wrong-push-substring": (["e4"], "blocks any command that merely starts a word with git push, such as git pushd"),
        "wrong-no-turn-limit": (["e6"], "leaves max turns unset, so the run is not capped"),
        "wrong-raises-after-error-result": (["e6"], "lets the error that follows an error result escape, so a run that hit its limit has no summary"),
        "wrong-hides-crash": (["e6"], "swallows every error, so a process that died before any result looks like a run that ended"),
        "wrong-status-unmapped": (["e5"], "reports the raw result subtype instead of the course status for the turn limit"),
    },
}

PRACTICES[f"{X}/32-mcp-fundamentals/unit-01/practice-1"] = {
    "name": "notes", "suite": "NotesTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a client can save a note find it and read it back"),
        ("e1", "edge", "the server declares tools resources and prompts and names its tools"),
        ("e2", "edge", "bad input comes back as a tool error the model can read"),
        ("e3", "edge", "search ignores case keeps id order honours the limit and says when nothing matches"),
        ("e4", "edge", "tool annotations tell a client which tool only reads"),
        ("e5", "edge", "resources give the count a note by id and an error for a missing one"),
        ("e6", "edge", "the prompt lists the notes and defaults the tone"),
        ("e7", "edge", "ids are sequential and a failed call does not use one"),
    ],
    "plants": {
        "wrong-error-not-flagged": (["e2"], "reports bad input as an ordinary result instead of a tool error"),
        "wrong-case-sensitive": (["e3"], "matches the query only with the same letter case"),
        "wrong-no-limit": (["e3"], "returns every hit and ignores the limit argument"),
        "wrong-read-only-unmarked": (["e4"], "does not mark the search tool as read-only"),
        "wrong-plural": (["e5"], "says 1 notes instead of 1 note"),
        "wrong-default-tone": (["e6"], "uses another default tone when the client gives none"),
        "wrong-no-trim": (["e7"], "keeps the spaces around a title and a text, so a blank one passes as given"),
    },
}

# --- PRACTICES ABOVE ---


# ===== Level 2: modules 38 to 41 =====
PRACTICES[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = {
    "name": "project_setup", "suite": "ProjectSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the memory file is short concrete and pulls in the architecture notes"),
        ("e1", "edge", "the permission rules allow the daily commands ask before commits and deny secrets and pushes"),
        ("e2", "edge", "the shared file sets a mode it may set and uses only rules that are consulted"),
        ("e3", "edge", "personal settings stay local and the local file wins"),
        ("e4", "edge", "the custom command is a skill that only a person can start"),
        ("e5", "edge", "the headless script is bounded and does not skip permissions"),
        ("e6", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-bypass-mode": (["e2"], "sets defaultMode to bypassPermissions in the shared file, where it is ignored"),
        "wrong-write-deny": (["e1"], "writes the secret rules for the Write tool, which no check consults, instead of Read"),
        "wrong-push-ask-only": (["e1"], "asks before a push instead of denying it"),
        "wrong-bare-bash-allow": (["e2"], "allows every Bash command with a bare Bash rule"),
        "wrong-local-not-ignored": (["e3"], "leaves settings.local.json out of .gitignore"),
        "wrong-local-widens": (["e3"], "puts a permission rule in the personal file"),
        "wrong-bloated-memory": (["m1"], "lets CLAUDE.md grow past 200 lines"),
        "wrong-no-import": (["m1"], "names the architecture file without the @ that imports it"),
        "wrong-skill-auto": (["e4"], "lets Claude start the fix-issue command on its own"),
        "wrong-headless-bypass": (["e5"], "skips permissions in the headless run"),
        "wrong-headless-unbounded": (["e5"], "leaves the headless run without a turn limit"),
        "wrong-key-in-script": (["e6"], "writes an API key into the script"),
    },
}

PRACTICES[f"{X}/39-extending-claude-code/unit-01/practice-1"] = {
    "name": "plugin_setup", "suite": "PluginSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the hook script blocks pushes deletes and piped downloads in any spelling"),
        ("e1", "edge", "edits to protected paths stop with exit two and a reason"),
        ("e2", "edge", "an event it cannot read blocks the call and other tools are left alone"),
        ("e3", "edge", "the hook is registered for every tool it guards and found through the plugin root"),
        ("e4", "edge", "the skills set the right invocation rules and approve only patterns"),
        ("e5", "edge", "the subagent only reads is bounded and uses only fields a plugin agent honours"),
        ("e6", "edge", "the manifest names the plugin and pins its dependency to patch updates"),
        ("e7", "edge", "the team settings register the marketplace the enabled plugin comes from"),
    ],
    "plants": {
        "wrong-push-prefix": (["m1"], "refuses git push only when push is the first word after git, so git -C . push passes"),
        "wrong-rm-literal": (["m1"], "refuses only the exact flag spelling -rf"),
        "wrong-pipe-shell-allowed": (["m1"], "lets a download be piped into a shell"),
        "wrong-edit-exit-one": (["e1"], "exits 1 on a protected path, which does not block the call"),
        "wrong-no-reason": (["e1"], "blocks a protected path without saying why"),
        "wrong-bad-json-allowed": (["e2"], "lets a call through when the event cannot be read"),
        "wrong-matcher-bash-only": (["e3"], "registers the hook for Bash only, so edits are not guarded"),
        "wrong-relative-path": (["e3"], "finds the script by a path relative to the working directory instead of the plugin root"),
        "wrong-publish-auto": (["e4"], "lets Claude start the publish skill on its own"),
        "wrong-bare-bash": (["e4"], "pre-approves every shell command for the publish skill"),
        "wrong-agent-writes": (["e5"], "gives the reviewer the Edit tool"),
        "wrong-agent-bypass": (["e5"], "sets a permission mode that a plugin agent ignores"),
        "wrong-dependency-unpinned": (["e6"], "depends on secrets-vault with no version range"),
        "wrong-marketplace-mismatch": (["e7"], "enables the plugin from a marketplace that the file does not register"),
    },
}

PRACTICES[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = {
    "name": "pipeline_setup", "suite": "PipelineSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the mention workflow answers only claude comments and holds no key"),
        ("e1", "edge", "the review workflow reads the code and posts the review"),
        ("e2", "edge", "each prompt file carries a version that its changelog explains"),
        ("e3", "edge", "the review guidance and the git workflow are files the reviewer and claude read"),
        ("e4", "edge", "every run is bounded by turns time and concurrency"),
        ("e5", "edge", "no file holds a key a personal path or an address"),
    ],
    "plants": {
        "wrong-mention-unguarded": (["m1"], "starts a runner on every comment instead of only on @claude mentions"),
        "wrong-mention-beta": (["m1"], "uses the beta action instead of v1"),
        "wrong-review-writes": (["e1"], "gives the review job write access to the contents"),
        "wrong-review-no-comment": (["e1"], "runs the review without --comment, so nothing is posted"),
        "wrong-review-no-checkout": (["e1"], "runs the review skill without checking the repository out"),
        "wrong-prompt-version-mismatch": (["e2"], "leaves the changelog on an older version than the prompt"),
        "wrong-prompt-no-version": (["e2"], "drops the version from the prompt file"),
        "wrong-review-no-skip": (["e3"], "has no Skip section in the review guidance"),
        "wrong-no-timeout": (["e4"], "leaves the mention job without a timeout"),
        "wrong-no-turn-cap": (["e4"], "leaves the mention run without a turn limit"),
        "wrong-literal-key": (["e5"], "writes the API key into the workflow"),
    },
}

PRACTICES[f"{X}/41-security-and-safety/unit-01/practice-1"] = {
    "name": "gate", "suite": "GateTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "untrusted text reaches the model only as one json string that says where it came from"),
        ("e1", "edge", "a screen names injection signals and a flagged result is withheld with an error"),
        ("e2", "edge", "reads and writes stay inside the project and away from secrets and protected folders"),
        ("e3", "edge", "bash is limited to a few read only commands and dangerous or chained ones are refused"),
        ("e4", "edge", "fetch and email obey the host and domain lists and refuse credentials in a url"),
        ("e5", "edge", "once untrusted content is in the session anything that changes things asks or is refused"),
        ("e6", "edge", "secrets card numbers and addresses are redacted in text and in the audit"),
        ("e7", "edge", "the hook answer follows the documented shapes and repeated denials raise an alert"),
    ],
    "plants": {
        "wrong-concatenate": (["m1"], "joins the source and the text into one string instead of encoding them as JSON"),
        "wrong-screen-case-sensitive": (["e1"], "matches the override signal only in the exact letter case"),
        "wrong-screen-leaks": (["e1"], "puts the start of the flagged text into the error that withholds it"),
        "wrong-prefix-sibling": (["e2"], "treats a folder whose name merely starts like the project folder as inside it"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or the other .env files"),
        "wrong-chain-allowed": (["e3"], "checks the first word only, so a chained or redirected command passes"),
        "wrong-host-suffix": (["e4"], "matches a host by the end of its name without the dot, so a lookalike host passes"),
        "wrong-email-tainted-allowed": (["e5"], "lets an email go out after untrusted content entered the session"),
        "wrong-luhn-skip": (["e6"], "redacts any long number as a card without the Luhn check"),
        "wrong-audit-raw": (["e6"], "writes the arguments to the audit unredacted"),
        "wrong-hook-exit2": (["e7"], "answers a denial with exit code 2 and not with the documented JSON and exit code 0"),
        "wrong-alert-asks": (["e7"], "counts questions as denials when it raises an alert"),
    },
}

# --- PRACTICES ABOVE ---


# ===== Level 2: modules 42 and 43 =====
PRACTICES[f"{X}/42-evaluation/unit-01/practice-1"] = {
    "name": "harness", "suite": "HarnessTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run grades every case with its own check and reports the pass rate"),
        ("e1", "edge", "an exact check ignores case and spacing but nothing else"),
        ("e2", "edge", "a json field check needs a json object with the field and the same typed value"),
        ("e3", "edge", "a judge check sends the rubric prompt and accepts only a bare score at the threshold"),
        ("e4", "edge", "a model that fails on one case does not stop the run"),
        ("e5", "edge", "tags report their own rates and success criteria judge each dimension"),
        ("e6", "edge", "a regression run names what broke what was fixed and what went missing"),
        ("e7", "edge", "repeated runs expose flaky cases and a case passes only if every run does"),
    ],
    "plants": {
        "wrong-regex-full-match": (["m1"], "requires the whole output to match the regular expression instead of searching it"),
        "wrong-exact-case-sensitive": (["e1"], "compares exact answers without lower-casing them"),
        "wrong-exact-contains": (["e1"], "accepts an output that merely contains the expected answer"),
        "wrong-json-loose-type": (["e2"], "compares the field and the expected value as text, so the string 3 equals the number 3"),
        "wrong-json-fence-ok": (["e2"], "strips a code fence before parsing, so fenced output passes"),
        "wrong-judge-first-digit": (["e3"], "takes the first digit it finds in the judge's reply instead of requiring a bare score"),
        "wrong-judge-strict-threshold": (["e3"], "requires a score above the threshold instead of at it"),
        "wrong-model-error-passes": (["e4"], "counts a run in which the model raised an error as a pass"),
        "wrong-tag-total": (["e5"], "counts a case in its tag total only when it passed"),
        "wrong-meets-at-or-below": (["e5"], "fails the overall criterion when the pass rate equals the minimum"),
        "wrong-meets-missing-tag-ok": (["e5"], "lets a criterion pass when no case carries its tag"),
        "wrong-compare-rate-only": (["e6"], "calls a run fine whenever the pass rate did not fall, even with a regression"),
        "wrong-compare-removed-ignored": (["e6"], "ignores cases that disappeared from the set"),
        "wrong-flaky-any-run": (["e7"], "passes a case when any of its repeated runs passes"),
    },
}

PRACTICES[f"{X}/43-debugging-claude-applications/unit-01/practice-1"] = {
    "name": "diagnose", "suite": "DiagnoseTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each documented http error maps to a type an origin and a recovery"),
        ("e1", "edge", "a 429 is a rate limit or a spend cap and other statuses fall back by class"),
        ("e2", "edge", "a successful response can still fail by its stop reason"),
        ("e3", "edge", "an empty end turn is the integration when text followed the tool result and the model otherwise"),
        ("e4", "edge", "a parse failure is the integration when a json object is in the text and the model when not"),
        ("e5", "edge", "tool failures split into a model that called a missing tool and our tool that raised"),
        ("e6", "edge", "the first failure names the cause and a later good response marks it recovered"),
        ("e7", "edge", "a dropped connection has no status and belongs to the service side"),
    ],
    "plants": {
        "wrong-retry-400": (["m1"], "retries a malformed request with back-off instead of fixing it"),
        "wrong-413-service": (["m1"], "blames the service for a request that is too large"),
        "wrong-429-always-retry": (["e1"], "treats a spend-cap 429 as an ordinary rate limit"),
        "wrong-spend-400-ignored": (["e1"], "treats a 400 spend-limit message as a malformed request"),
        "wrong-maxtokens-model": (["e2"], "blames the model for a response cut off by the caller's own max_tokens"),
        "wrong-refusal-retry": (["e2"], "retries a refusal with back-off instead of using a fallback model"),
        "wrong-empty-always-model": (["e3"], "blames the model for every empty end turn"),
        "wrong-empty-text-anywhere": (["e3"], "blames the integration whenever text and a tool result are both present, in any order"),
        "wrong-parse-always-model": (["e4"], "blames the model for every parse failure"),
        "wrong-parse-braces-only": (["e4"], "calls any text with braces a JSON object without parsing it"),
        "wrong-unknown-tool-integration": (["e5"], "blames the integration for a tool name the model invented"),
        "wrong-tool-error-flagged": (["e5"], "reports a tool result flagged is_error as a failure of our code"),
        "wrong-last-failure": (["e6"], "reports the last failure in the trace instead of the first"),
        "wrong-recovered-ignored": (["e6"], "never reports a recovery"),
        "wrong-recovered-empty": (["e6"], "counts an empty end turn as a recovery"),
        "wrong-network-integration": (["e7"], "blames the integration for a dropped connection"),
    },
}

# --- PRACTICES ABOVE ---


# ===== Level 3: modules 45 to 56 =====
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
    "name": "mcp_setup", "suite": "McpSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
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
    "name": "explorer_setup", "suite": "ExplorerSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
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

# ===== Level 3: modules 57 to 62 =====
PRACTICES[f"{X}/57-memory-files-and-rules/unit-01/practice-1"] = {
    "name": "memory_setup", "suite": "MemorySetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the conventions of each area load for exactly the files that area governs"),
        ("e1", "edge", "the root file is short and holds only what every task needs"),
        ("e2", "edge", "the testing rule follows the file type and not the folder"),
        ("e3", "edge", "the import names a file that exists and loads at launch"),
        ("e4", "edge", "personal lines sit in personal files and the local file is ignored"),
        ("e5", "edge", "a rule that must always hold is a permission rule and not a sentence"),
        ("e6", "edge", "every rule scopes itself with paths that are valid and match something"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-api-unscoped": (["m1", "e6"], "leaves the API rule without paths, so it loads in every session"),
        "wrong-api-bare-folder": (["m1", "e6"], "scopes the API rule with a bare folder name, which is not a glob and matches no file"),
        "wrong-testing-folder": (["m1", "e2"], "scopes the testing rule to one folder, so test files elsewhere miss it"),
        "wrong-testing-ts-only": (["m1", "e2"], "scopes the testing rule to the ts files and leaves the tsx files out"),
        "wrong-terraform-everything": (["m1"], "scopes the Terraform rule to every file"),
        "wrong-root-keeps-testing": (["m1", "e1"], "leaves one testing convention in the root file"),
        "wrong-root-long": (["e1"], "pads the root file past fifty lines"),
        "wrong-import-typo": (["e3"], "misspells the path of the import, so it imports nothing"),
        "wrong-import-in-code-span": (["e3"], "writes the import inside a code span, where it is not expanded"),
        "wrong-personal-in-root": (["e4"], "puts a personal preference into the shared root file"),
        "wrong-local-not-ignored": (["e4"], "leaves the local instructions file out of the ignore file"),
        "wrong-no-migration-deny": (["e5"], "leaves the migrations protected by a sentence only"),
        "wrong-write-rule": (["e5"], "writes the migrations rule for the Write tool, whose path rules are never matched"),
        "wrong-home-path": (["e7"], "writes a personal home path into the root file"),
    },
}

PRACTICES[f"{X}/58-commands-and-skills/unit-01/practice-1"] = {
    "name": "skill_setup", "suite": "SkillSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the review skill is a forked skill with an explicit task and an argument"),
        ("e1", "edge", "the release skill is started only by a person and pre approves patterns"),
        ("e2", "edge", "tools are taken away with disallowed tools and allowed tools only pre approves"),
        ("e3", "edge", "arguments fill the placeholders of every file"),
        ("e4", "edge", "every file creates its own slash command and the personal variant has a new name"),
        ("e5", "edge", "each piece of guidance lives where it loads the way it is used"),
        ("e6", "edge", "every skill says when to use it and stays inside the listing budget"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-fork-guidelines-only": (["m1"], "writes guidelines instead of numbered steps in a forked skill"),
        "wrong-no-fork": (["m1"], "runs the review in the conversation instead of a forked context"),
        "wrong-no-agent": (["m1"], "leaves the subagent type out of the forked skill"),
        "wrong-no-hint": (["m1"], "leaves the argument hint out of the review skill"),
        "wrong-no-placeholder": (["m1", "e3"], "never uses the pull request number in the steps"),
        "wrong-allowed-restricts": (["e2"], "relies on allowed tools to keep the review from editing"),
        "wrong-disallow-scoped": (["e2"], "writes scoped rules in disallowed tools, which leave Edit and Write in place"),
        "wrong-review-bare-bash": (["e2"], "pre approves the whole Bash tool in the review skill"),
        "wrong-tag-bare-bash": (["e1"], "pre approves the whole Bash tool in the release skill"),
        "wrong-tag-auto": (["e1"], "lets Claude start the release skill on its own"),
        "wrong-tag-no-arguments": (["e3"], "uses a named argument without declaring it"),
        "wrong-name-collision": (["e4"], "gives the personal variant the name of the release skill"),
        "wrong-mine-shadows": (["e4"], "gives the personal variant the name of the team skill, so it replaces it for you"),
        "wrong-standup-no-hint": (["e4"], "leaves the hint out of the standup command"),
        "wrong-placement-rule-in-memory": (["e5"], "places the test file conventions in the always loaded file"),
        "wrong-placement-personal-in-project": (["e5"], "places the personal variant in the shared project folder"),
        "wrong-no-use-when": (["e6"], "describes the review skill without saying when to use it"),
        "wrong-long-description": (["e6"], "writes a description longer than the listing keeps"),
        "wrong-home-path": (["e7"], "writes a personal home path into the placement notes"),
    },
}

PRACTICES[f"{X}/61-criteria-and-examples/unit-01/practice-1"] = {
    "name": "review_spec", "suite": "ReviewSpecTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the prompt puts criteria first then examples then the diff last"),
        ("e1", "edge", "vague criteria are refused in both the report and the skip text"),
        ("e2", "edge", "a criterion needs report skip and a concrete severity example for high and low"),
        ("e3", "edge", "two to four examples with a report and a skip each carrying a reason"),
        ("e4", "edge", "a category with enough reviews and low precision is disabled"),
        ("e5", "edge", "the most dismissed patterns are listed by count then name and capped at three"),
        ("e6", "edge", "an attended run asks only what it cannot assume and states its assumptions"),
        ("e7", "edge", "an unattended run never asks it states assumptions or stops"),
    ],
    "plants": {
        "wrong-diff-first": (["m1"], "puts the diff before the criteria"),
        "wrong-vague-report-only": (["e1"], "checks the report text for vague phrases and not the skip text"),
        "wrong-vague-short-list": (["e1"], "leaves be conservative out of the vague phrases"),
        "wrong-skip-blank": (["e2"], "accepts a blank report or skip text"),
        "wrong-severity-high-only": (["e2"], "asks for a high severity example and not for a low one"),
        "wrong-examples-up-to-six": (["e3"], "allows up to six examples"),
        "wrong-all-report-examples": (["e3"], "accepts a set of examples that are all reports"),
        "wrong-reason-optional": (["e3"], "accepts an example whose reason is blank"),
        "wrong-unknown-category": (["e3"], "accepts a report example for a category that is not a criterion"),
        "wrong-disable-few": (["e4"], "disables a category on too few reviews"),
        "wrong-precision-inverted": (["e4"], "computes the share dismissed and calls it precision"),
        "wrong-boundary-disables": (["e4"], "disables a category whose precision is exactly the limit"),
        "wrong-top-by-name": (["e5"], "lists dismissed patterns by name and not by count"),
        "wrong-top-uncapped": (["e5"], "lists every dismissed pattern"),
        "wrong-counts-accepted": (["e5"], "counts the patterns of accepted findings as dismissed"),
        "wrong-ask-defaults": (["e6"], "asks for fields that have a default"),
        "wrong-no-assumptions": (["e6"], "assumes defaults without stating them"),
        "wrong-unattended-asks": (["e7"], "asks a question in a run that nobody attends"),
        "wrong-unattended-guess": (["e7"], "proceeds in an unattended run without a value that has no default"),
        "wrong-blank-is-present": (["e6"], "treats a field that holds only spaces as given"),
    },
}

PRACTICES[f"{X}/62-structured-output-at-the-architect-level/unit-01/practice-1"] = {
    "name": "extraction", "suite": "ExtractionTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a document with every value present and quoted comes back valid on the first attempt"),
        ("e1", "edge", "a value the document does not give is null and needs no quote while an invented value fails as ungrounded"),
        ("e2", "edge", "a retry carries the original document the failed record and only the errors a second look can fix"),
        ("e3", "edge", "a required value that the model reports as absent is not retried and goes to review"),
        ("e4", "edge", "retries stop after the limit and the document is marked failed"),
        ("e5", "edge", "a total that differs from the line items is a semantic error and a flagged conflict goes to review without a retry"),
        ("e6", "edge", "currency takes unclear and other with a detail and rejects anything else as a syntax error"),
        ("e7", "edge", "chunk results merge by keeping the first value and recording a conflict when two chunks disagree"),
        ("e8", "edge", "accuracy counts every document and not only the validated ones"),
        ("e9", "edge", "the request forces a tool where the model allows it and falls back to auto with a reply check where it does not"),
    ],
    "plants": {
        "wrong-grounding-skipped": (["e1"], "accepts a quote that does not appear in the document"),
        "wrong-null-needs-quote": (["e1"], "demands a quote for a value that is null"),
        "wrong-feedback-all-errors": (["e2"], "sends every error back, including the ones a second look cannot fix"),
        "wrong-feedback-no-previous": (["e2"], "leaves the failed record out of the retry"),
        "wrong-retry-absent": (["e3"], "retries a value that the model reported as absent"),
        "wrong-absent-failed": (["e3"], "marks a document with only absent values as failed instead of sending it to review"),
        "wrong-extra-retry": (["e4"], "makes one retry more than the limit"),
        "wrong-sum-unchecked": (["e5"], "never compares the line items with the calculated total"),
        "wrong-conflict-error": (["e5"], "treats a conflict the model flagged as a semantic error"),
        "wrong-conflict-valid": (["e5"], "marks a document with a flagged conflict as valid"),
        "wrong-unclear-needs-quote": (["e6"], "demands a quote for a currency that is unclear"),
        "wrong-other-no-detail": (["e6"], "accepts the currency other without a detail"),
        "wrong-currency-open": (["e6"], "accepts any currency string"),
        "wrong-merge-last-wins": (["e7"], "lets a later chunk replace an earlier value"),
        "wrong-merge-no-conflict": (["e7"], "never records a conflict between chunks"),
        "wrong-accuracy-validated-only": (["e8"], "divides by the validated documents and calls it accuracy"),
        "wrong-accuracy-missing-skipped": (["e8"], "leaves the documents without a result out of the count"),
        "wrong-force-always": (["e9"], "forces a tool on models that reject a forced choice"),
        "wrong-any-for-single": (["e9"], "uses any where one named tool is enough"),
        "wrong-no-verify": (["e9"], "does not ask for a reply check on the fallback"),
    },
}

PRACTICES[f"{X}/60-claude-code-in-ci/unit-01/practice-1"] = {
    "name": "ci_review", "suite": "CiReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a valid run posts the findings above the floor and outside the disabled categories"),
        ("e1", "edge", "a failed run fails the job instead of passing it silently"),
        ("e2", "edge", "an answer that breaks the schema fails the job and names the path of the problem"),
        ("e3", "edge", "a finding at the failing severity blocks the merge and lower ones only comment"),
        ("e4", "edge", "the prompt lists earlier findings and existing tests and asks for new or unaddressed issues only"),
        ("e5", "edge", "the schema file is valid draft 07 and requires every field of a finding"),
        ("e6", "edge", "the workflow runs claude headless with json output a schema a turn limit read only tools and the context file"),
        ("e7", "edge", "the project file states what to report and what to skip with a severity example for each level"),
        ("e8", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-gate-nonzero-ignored": (["e1"], "ignores a non zero exit status when the output looks fine"),
        "wrong-gate-error-ignored": (["e1"], "ignores an error subtype and posts whatever the output holds"),
        "wrong-gate-no-output-ok": (["e1"], "passes a run that succeeded without a structured output"),
        "wrong-gate-no-schema": (["e2"], "never checks the answer against the schema"),
        "wrong-gate-floor-ignored": (["m1"], "posts findings below the severity floor"),
        "wrong-gate-disabled-ignored": (["m1"], "posts findings of a disabled category"),
        "wrong-gate-never-blocks": (["e3"], "never fails the job on a high finding"),
        "wrong-gate-blocks-on-any": (["e3"], "fails the job on any posted finding"),
        "wrong-prompt-no-new-only": (["e4"], "leaves out the instruction to report only new or unaddressed findings"),
        "wrong-prompt-no-prior": (["e4"], "leaves the earlier findings out of the prompt"),
        "wrong-prompt-no-tests": (["e4"], "leaves the existing tests out of the prompt"),
        "wrong-prompt-diff-first": (["e4"], "puts the diff before the instructions"),
        "wrong-schema-severity-open": (["e5"], "leaves the severity open to any string"),
        "wrong-schema-draft-2020": (["e5"], "declares a newer schema version than the SDK validates"),
        "wrong-schema-min-length": (["e5"], "uses a string constraint that structured outputs do not support"),
        "wrong-schema-pattern-optional": (["e5"], "makes the detected pattern optional"),
        "wrong-schema-extra-allowed": (["e5"], "lets a finding carry extra keys"),
        "wrong-no-print": (["e6"], "runs claude without the print flag, which waits for input"),
        "wrong-no-json": (["e6"], "leaves the json output format out"),
        "wrong-no-schema-flag": (["e6"], "leaves the schema out of the command"),
        "wrong-no-max-turns": (["e6"], "leaves the turn limit out"),
        "wrong-bash-tool": (["e6"], "pre approves the whole Bash tool in a review"),
        "wrong-no-bare": (["e6"], "runs without bare mode, so the host configuration loads"),
        "wrong-bare-no-context": (["e6"], "uses bare mode without passing the project file"),
        "wrong-literal-key": (["e6", "e8"], "writes the API key into the workflow"),
        "wrong-no-timeout": (["e6"], "leaves the job without a timeout"),
        "wrong-write-permission": (["e6"], "gives the review job write access to the contents"),
        "wrong-vague-criteria": (["e7"], "writes be conservative into the criteria"),
        "wrong-no-severity-example": (["e7"], "leaves the high severity without an example"),
        "wrong-no-fixtures": (["e7"], "leaves the fixtures folder out of the testing standards"),
        "wrong-home-path": (["e8"], "writes a personal home path into the project file"),
    },
}

# ===== Level 3: module 63 =====
PRACTICES[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"] = {
    "name": "batch_review", "suite": "BatchReviewTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_batch_review.py", "tsfile": "batchReview.test.ts",
    "cases": [
        ("m1", "main", "the interval between submissions leaves room for the window and the handling"),
        ("e1", "edge", "an sla without room for a batch is refused"),
        ("e2", "edge", "a blocking check or a tool loop needs the synchronous api"),
        ("e3", "edge", "only the items that did not succeed are resubmitted by custom id"),
        ("e4", "edge", "an item over the limit is chunked and a rejected request is fixed first"),
        ("e5", "edge", "a multi file review gets a local pass per file and one integration pass"),
        ("e6", "edge", "the same finding from two passes is one finding with the highest severity and the lowest confidence"),
        ("e7", "edge", "a finding is accepted only when two independent passes agree with confidence"),
    ],
    "plants": {
        "wrong-ignores-handling": (["m1", "e1"], "leaves the handling time out of the interval"),
        "wrong-oversized-resubmitted": (["e4"], "resubmits an oversized item unchanged instead of chunking it"),
        "wrong-resubmit-all": (["e3"], "resubmits the items that succeeded too"),
        "wrong-no-integration-pass": (["e5"], "plans local passes only and no integration pass"),
        "wrong-lone-confident-accepted": (["e7"], "accepts a finding that one pass reported with high confidence"),
    },
}

_c = PRACTICES[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"]["plants"]
_c["wrong-chunk-at-limit"] = (["e4"], "chunks an entry that is exactly at the limit")
_c["wrong-interval-boundary"] = (["m1"], "refuses a submission interval of one hour")

# ===== Level 3: module 64 =====
PRACTICES[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"] = {
    "name": "context_builder", "suite": "ContextBuilderTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "trimming keeps only the named fields with their exact values in the named order"),
        ("e1", "edge", "a field that the record does not have is skipped"),
        ("e2", "edge", "a newer fact replaces the old one and the old value is kept as history"),
        ("e3", "edge", "an older fact that arrives late does not replace the current one"),
        ("e4", "edge", "the case facts of another customer never enter the context"),
        ("e5", "edge", "the context puts case facts first then the summary then the recent messages"),
        ("e6", "edge", "a summary that loses an exact value is reported"),
        ("e7", "edge", "the window drops the oldest messages and keeps a tool call with its result"),
    ],
    "plants": {
        "wrong-no-trim": (["m1", "e1"], "returns the whole tool record instead of the named fields"),
        "wrong-older-overwrites": (["e3"], "lets a fact that arrives late replace a newer one"),
        "wrong-all-customers": (["e4"], "puts the case facts of every customer into the context"),
        "wrong-facts-last": (["e5"], "places the case facts after the summary and the recent messages"),
        "wrong-pair-split": (["e7"], "keeps a tool result without the call that produced it"),
        "wrong-loose-summary-check": (["e6"], "counts a value as kept when only its first two characters appear"),
    },
}

PRACTICES[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"]["plants"]["wrong-equal-date-ignored"] = (["e2"], "ignores a fact dated the same day as the stored one")

# ===== Level 3: module 65 =====
PRACTICES[f"{X}/65-escalation-and-ambiguity/unit-01/practice-1"] = {
    "name": "escalation", "suite": "EscalationTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a customer who asks for a person is escalated at once even when the agent could resolve it"),
        ("e1", "edge", "frustration alone does not escalate and the reply acknowledges it"),
        ("e2", "edge", "a request the policy does not cover is escalated and a covered one is resolved"),
        ("e3", "edge", "several matching records need a clarifying question and never a guess"),
        ("e4", "edge", "an explicit request for a person outranks an ambiguous match"),
        ("e5", "edge", "no progress after the attempt limit escalates and below it does not"),
        ("e6", "edge", "sentiment and confidence scores never change the decision"),
        ("e7", "edge", "the clarifying question names only the fields that tell the matches apart"),
        ("e8", "edge", "the hand off carries the structured facts and no transcript and refuses a case without an id"),
    ],
    "plants": {
        "wrong-investigate-first": (["m1", "e4"], "escalates on an explicit request only when the policy also has a gap"),
        "wrong-angry-escalates": (["e6"], "escalates an angry customer because of the sentiment"),
        "wrong-low-confidence-escalates": (["e6"], "escalates when the model reports low confidence"),
        "wrong-picks-first-match": (["e3", "e6"], "never asks which of several matching customers is meant"),
        "wrong-policy-gap-resolved": (["e2"], "resolves a request the policy does not cover"),
        "wrong-no-acknowledgement": (["e1"], "does not acknowledge a frustrated customer"),
        "wrong-clarify-all-fields": (["e7"], "asks about fields on which the matches agree"),
        "wrong-handoff-transcript": (["e8"], "attaches the whole transcript to the hand-off"),
    },
}

# ===== Level 3: module 66 =====
PRACTICES[f"{X}/66-errors-across-agents/unit-01/practice-1"] = {
    "name": "error_flow", "suite": "ErrorFlowTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a transient failure is retried locally and the success is reported with its attempts"),
        ("e1", "edge", "a valid empty result is a success with no findings and never an error"),
        ("e2", "edge", "a permission or invalid query error is not retried and carries what was attempted and its alternatives"),
        ("e3", "edge", "a failure that survives the retries carries the partial results of the last attempt"),
        ("e4", "edge", "the coordinator uses partial results tries an alternative or flags a gap and never stops the run"),
        ("e5", "edge", "the coverage note separates supported topics from gaps and names the cause"),
        ("e6", "edge", "a topic with no result is a gap that was not searched"),
    ],
    "plants": {
        "wrong-empty-is-error": (["e1"], "reports a search with no matches as a failure"),
        "wrong-retry-permission": (["e2"], "retries a permission error as if it were transient"),
        "wrong-drop-partial": (["e3"], "throws away the partial results of a failed search"),
        "wrong-generic-error": (["e2", "e3"], "reports only that the search failed, without its type and the query attempted"),
        "wrong-stop-on-failure": (["e4"], "abandons the whole run when one topic failed"),
        "wrong-gap-as-supported": (["e5"], "lists a failed topic among the well-supported ones"),
        "wrong-missing-topic-skipped": (["e6"], "leaves a topic that was never searched out of the note"),
    },
}

# ===== Level 3: module 67 =====
PRACTICES[f"{X}/67-exploring-a-large-codebase/unit-01/practice-1"] = {
    "name": "recovery", "suite": "RecoveryTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a finding is recorded once per area and fact in first seen order"),
        ("e1", "edge", "the scratchpad groups findings under their area"),
        ("e2", "edge", "the manifest lists every agent with its state file and status and refuses bad input"),
        ("e3", "edge", "a finished agent with its state file is reused and not run again"),
        ("e4", "edge", "a running or failed agent with a state file is resumed from it"),
        ("e5", "edge", "an agent whose state file is missing is restarted from scratch"),
        ("e6", "edge", "the resume prompt carries the task and the state lines and nothing else"),
        ("e7", "edge", "the compact command names what to keep"),
    ],
    "plants": {
        "wrong-duplicate-findings": (["m1"], "records the same fact twice for one area"),
        "wrong-ungrouped-scratchpad": (["e1"], "repeats an area heading for every finding"),
        "wrong-manifest-unsorted": (["e2"], "lists the agents in the order they were given"),
        "wrong-manifest-unvalidated": (["e2"], "accepts a status the manifest does not know"),
        "wrong-rerun-done": (["e3", "e5"], "resumes an agent that already finished"),
        "wrong-restart-running": (["e4"], "restarts an agent that has a state file to resume from"),
        "wrong-ignore-missing-file": (["e5"], "trusts the manifest without checking that the state file exists"),
        "wrong-no-continue-line": (["e6"], "leaves the instruction to continue out of the prompt"),
        "wrong-compact-without-focus": (["e7"], "compacts without telling the command what to keep"),
    },
}

# ===== Level 3: module 68 =====
PRACTICES[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"] = {
    "name": "review_routing", "suite": "ReviewRoutingTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "accuracy is reported per document type and field next to the overall figure"),
        ("e1", "edge", "a weak segment is hidden by a high overall figure and found by the breakdown"),
        ("e2", "edge", "automation needs every segment to pass and enough samples in each"),
        ("e3", "edge", "the threshold is the lowest confidence whose accepted items meet the target precision"),
        ("e4", "edge", "no threshold exists when no confidence level meets the target"),
        ("e5", "edge", "the stratified sample takes the best ranked items of every stratum"),
        ("e6", "edge", "low confidence and conflicts go to review with the weakest first"),
        ("e7", "edge", "review capacity is respected and the rest wait in a backlog"),
        ("e8", "edge", "an irreversible action needs a person whatever the confidence"),
    ],
    "plants": {
        "wrong-overall-percent": (["m1", "e1"], "gives every segment the overall percentage"),
        "wrong-ignores-undersampled": (["e2"], "approves automation for a segment with too few samples"),
        "wrong-highest-confidence": (["e3"], "picks the highest qualifying confidence instead of the lowest"),
        "wrong-strict-target": (["e4"], "demands more than the target precision"),
        "wrong-first-n-sample": (["e5"], "samples by identifier instead of by rank"),
        "wrong-conflict-auto": (["e6"], "lets a confident extraction with a conflict through"),
        "wrong-id-order": (["e6"], "orders the review queue by identifier instead of weakest first"),
        "wrong-ignores-capacity": (["e7"], "sends everything to review regardless of capacity"),
        "wrong-irreversible-by-amount": (["e8"], "checks only the amount for an irreversible action"),
    },
}

_c = PRACTICES[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"]["plants"]
_c["wrong-minn-boundary"] = (["e2"], "lets a segment with exactly the minimum number of records fail unnoticed")
_c["wrong-threshold-boundary"] = (["e2"], "fails a segment that is exactly at the threshold")
_c["wrong-route-boundary"] = (["e6"], "sends an extraction exactly at the threshold to review")

# ===== Level 3: module 69 =====
PRACTICES[f"{X}/69-provenance-and-uncertainty/unit-01/practice-1"] = {
    "name": "ledger", "suite": "LedgerTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "findings are merged per claim with every source kept once"),
        ("e1", "edge", "a finding without a source or a date is refused"),
        ("e2", "edge", "two values from the same date are a conflict that keeps both"),
        ("e3", "edge", "two values from different dates are a change not a conflict"),
        ("e4", "edge", "the coverage note separates what is well supported from what is not"),
        ("e5", "edge", "a planned claim with no finding is a gap with its reason"),
        ("e6", "edge", "financial data is rendered as a table"),
        ("e7", "edge", "news is rendered as prose that says when sources disagree"),
        ("e8", "edge", "technical findings are a list and an unknown content type is refused"),
    ],
    "plants": {
        "wrong-first-wins": (["e2", "e3"], "keeps only the first value of a claim and drops the others"),
        "wrong-ignores-dates": (["e3", "e4"], "calls every difference a conflict whatever the dates"),
        "wrong-sources-collapsed": (["m1", "e4"], "keeps only the first source of a value"),
        "wrong-date-optional": (["e1"], "does not require a date"),
        "wrong-accepts-incomplete": (["e1"], "merges a finding with a missing field"),
        "wrong-gaps-hidden": (["e5"], "leaves the planned claims with no finding out of the note"),
        "wrong-single-source-supported": (["e4"], "calls a claim with one source well supported"),
        "wrong-unknown-kind-accepted": (["e8"], "renders an unknown content type"),
        "wrong-no-table": (["e6"], "renders financial data as prose"),
        "wrong-conflict-unmarked": (["e7"], "leaves the disagreement out of the prose"),
    },
}

# ===== Level 3: modules 70 to 74 (the scenario capstones) =====
PRACTICES[f"{X}/70-scenario-customer-support-agent/unit-01/practice-1"] = {
    "name": "audit", "suite": "AuditTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed set of sessions gets every rate and the first fix"),
        ("e1", "edge", "no sessions give zero rates and no diagnosis"),
        ("e2", "edge", "a protected call before a successful identity check is a skipped prerequisite"),
        ("e3", "edge", "a refund over the limit counts only when it was made"),
        ("e4", "edge", "money first then tool descriptions then escalation criteria"),
        ("e5", "edge", "the target boundary and rounding of the first contact rate"),
        ("e6", "edge", "a wrong tool counts sessions and ignores steps with no known right tool"),
    ],
    "plants": {
        "wrong-prereq-ignores-order": (["m1", "e2"], "counts every protected call as a skipped prerequisite, even after the customer was identified"),
        "wrong-failed-identity-opens": (["e2"], "treats a failed identity check as an identification"),
        "wrong-over-limit-any-outcome": (["e3"], "counts a refund over the limit in a session where it was refused and escalated"),
        "wrong-diagnosis-criteria-first": (["e4"], "puts the escalation criteria before the tool descriptions in the order of fixes"),
        "wrong-target-strict": (["e5"], "requires the rate to be above the target and not at it"),
        "wrong-wrong-tool-steps": (["e6"], "counts wrong steps and not the sessions that have one"),
        "wrong-no-rounding": (["m1", "e5"], "leaves the first contact rate unrounded"),
        "wrong-over-counts-needs-human": (["m1"], "counts every escalation as an over-escalation, even one that needed a person"),
    },
}

PRACTICES[f"{X}/71-scenario-code-generation-with-claude-code/unit-01/practice-1"] = {
    "name": "team_setup", "suite": "TeamSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each convention loads for exactly the files of its area"),
        ("e1", "edge", "the root file is short and holds only what every task needs"),
        ("e2", "edge", "the review command is shared read only and says what it does"),
        ("e3", "edge", "the settings protect the environment file and approve no whole tool"),
        ("e4", "edge", "the modes table sends open design work to plan mode and clear small work to direct"),
        ("e5", "edge", "every rule scopes itself with a glob that matches a file"),
        ("e6", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-components-unscoped": (["m1", "e5"], "leaves the components rule without paths, so it loads in every session"),
        "wrong-handlers-wide": (["m1"], "scopes the handlers rule to every ts file, so it also loads for the database files"),
        "wrong-tests-folder": (["m1"], "scopes the testing rule to one folder, so test files elsewhere miss it"),
        "wrong-tests-ts-only": (["m1"], "scopes the testing rule to the ts files and leaves the tsx files out"),
        "wrong-database-bare-folder": (["m1", "e5"], "scopes the database rule with a bare folder name, which reaches none of the sample files"),
        "wrong-root-keeps-hooks": (["e1"], "leaves one component convention in the root file"),
        "wrong-root-long": (["e1"], "pads the root file past twenty-five lines"),
        "wrong-review-bare-bash": (["e2"], "pre approves the whole Bash tool in the review command"),
        "wrong-review-no-description": (["e2"], "leaves the description out of the review command"),
        "wrong-no-env-deny": (["e3"], "leaves the environment file readable"),
        "wrong-bash-allow": (["e3"], "approves the whole Bash tool in the settings"),
        "wrong-monolith-direct": (["e4"], "sends the restructuring of the monolith to direct execution"),
        "wrong-typo-plan": (["e4"], "sends a one-line typo fix through plan mode"),
        "wrong-home-path": (["e6"], "writes a personal home path into the root file"),
    },
}

PRACTICES[f"{X}/72-scenario-multi-agent-research-system/unit-01/practice-1"] = {
    "name": "synthesis", "suite": "SynthesisTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a full run with agreement returns claims with every source and a complete status"),
        ("e1", "edge", "no results leave every scope a gap and say not researched"),
        ("e2", "edge", "a scope the plan never covered and a scope whose search failed read differently in the note"),
        ("e3", "edge", "two values for one claim are a conflict that names both sources and no claim"),
        ("e4", "edge", "partial results are kept and flagged but do not cover the scope and a retry that worked clears the error"),
        ("e5", "edge", "a result with no findings does not cover its scope"),
        ("e6", "edge", "claims sources and scopes come out in a fixed order without duplicates"),
    ],
    "plants": {
        "wrong-complete-by-errors": (["e1", "e5"], "calls the run complete when there are no errors, although a scope was never researched"),
        "wrong-conflict-first-wins": (["e3"], "keeps the first value of a claim and drops the other source"),
        "wrong-partial-covers": (["e4"], "counts a scope with only partial results as covered"),
        "wrong-empty-covers": (["e5"], "counts a scope whose result holds no findings as covered"),
        "wrong-partial-dropped": (["e4"], "drops the partial findings of a failed search"),
        "wrong-resolved-error-kept": (["e4"], "keeps the error of a scope that a later result covered"),
        "wrong-sources-duplicated": (["e6"], "lists the same source twice for one claim"),
        "wrong-claims-unsorted": (["e6"], "returns the claims in the order they arrived"),
        "wrong-note-silent": (["e1", "e2", "e5"], "writes an all-clear note whatever is missing"),
        "wrong-partial-flag-any": (["e4"], "flags a claim as partial when any of its findings was partial"),
    },
}

PRACTICES[f"{X}/73-scenario-developer-productivity/unit-01/practice-1"] = {
    "name": "mcp_setup", "suite": "McpSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "every mcp tool reference belongs to a configured server"),
        ("e1", "edge", "credentials come from the environment and the token has no default"),
        ("e2", "edge", "the explorer is read only and says when to use it"),
        ("e3", "edge", "the scaffolder writes only in the generated folder"),
        ("e4", "edge", "the tickets server is read only for agents"),
        ("e5", "edge", "the environment file is denied and no whole tool is approved"),
        ("e6", "edge", "the team note lists every variable and says which session to start"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-unknown-server": (["m1", "e2"], "gives the explorer a tool of a server that is not configured"),
        "wrong-settings-ghost": (["m1"], "allows a ticket tool under a server name that is not configured"),
        "wrong-literal-token": (["e1"], "writes the token into the header"),
        "wrong-token-default": (["e1"], "gives the token variable a default value"),
        "wrong-explorer-bash": (["e2"], "gives the explorer the Bash tool"),
        "wrong-explorer-inherits": (["e2"], "leaves the tools line out, so the explorer inherits every tool"),
        "wrong-explorer-description": (["e2"], "describes the explorer without saying when to use it"),
        "wrong-scaffolder-bash": (["e3"], "gives the scaffolder the Bash tool"),
        "wrong-edit-bare": (["e3", "e5"], "allows the Edit tool for every path"),
        "wrong-write-wide": (["e3"], "adds a Write path rule for the source folder, which Claude Code never consults"),
        "wrong-tickets-wildcard": (["e4"], "allows every ticket tool, creating and deleting included"),
        "wrong-no-delete-deny": (["e4"], "leaves deleting a ticket open"),
        "wrong-no-env-deny": (["e5"], "leaves the environment file readable"),
        "wrong-bash-allow": (["e5"], "approves the whole Bash tool"),
        "wrong-note-missing-variable": (["e6"], "leaves one variable out of the team note"),
        "wrong-rewritten-resumes": (["e6"], "tells the team to resume a session after the code was rewritten"),
        "wrong-home-path": (["e7"], "writes a personal home path into the team note"),
    },
}

PRACTICES[f"{X}/74-scenario-claude-code-in-ci/unit-01/practice-1"] = {
    "name": "ci_setup", "suite": "CiSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "who waits decides between real time and batch"),
        ("e1", "edge", "a review has a pass for each file and then an integration pass"),
        ("e2", "edge", "a review runs in a fresh session and is given the earlier findings"),
        ("e3", "edge", "every claude command is headless json and bounded"),
        ("e4", "edge", "the review can only read"),
        ("e5", "edge", "the criteria name what to report what to skip and an example for each severity"),
        ("e6", "edge", "the test prompt passes the existing tests and says what a useful test is"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-review-batch": (["m1"], "runs the blocking merge check as a batch"),
        "wrong-debt-realtime": (["m1"], "runs the overnight report in real time"),
        "wrong-single-pass": (["e1"], "reviews all files in one pass with no integration pass"),
        "wrong-integration-first": (["e1"], "puts the integration pass before the per-file passes"),
        "wrong-shared-session": (["e2"], "runs the review in the session that wrote the code"),
        "wrong-no-prior-findings": (["e2"], "gives a re-run no earlier findings"),
        "wrong-no-print-flag": (["e3"], "leaves the -p flag out of the review command"),
        "wrong-no-schema": (["e3"], "leaves the schema out of the review command"),
        "wrong-unbounded": (["e3"], "leaves the turn limit out of the review command"),
        "wrong-review-bash": (["e4"], "gives the review the Bash tool"),
        "wrong-allowed-edit": (["e4"], "approves the Edit tool in the review command"),
        "wrong-severity-free-text": (["e3"], "makes severity free text in the schema"),
        "wrong-no-suggestion": (["e3"], "stops requiring a suggestion in a finding"),
        "wrong-no-skip-list": (["e5"], "leaves out the list of what to skip"),
        "wrong-no-example": (["e5"], "leaves the high severity without an example"),
        "wrong-vague-criteria": (["e5"], "adds a vague instruction to be conservative"),
        "wrong-prompt-no-existing": (["e6"], "does not pass the existing tests to the test prompt"),
        "wrong-prompt-no-useful": (["e6"], "does not say what a useful test is"),
        "wrong-home-path": (["e7"], "writes a personal home path into the criteria"),
    },
}

# ===== Level 3: module 75 (the scenario capstone S6) =====
PRACTICES[f"{X}/75-scenario-structured-data-extraction/unit-01/practice-1"] = {
    "name": "run_audit", "suite": "RunAuditTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed run gets every count both accuracies the segments and the first fix"),
        ("e1", "edge", "an empty run has zero figures no segments and never meets the target"),
        ("e2", "edge", "the run meets the target at exactly the target and not below it"),
        ("e3", "edge", "a kind needs at least the minimum number of documents to be automated"),
        ("e4", "edge", "a kind is automated at exactly the target accuracy and not below it"),
        ("e5", "edge", "the figure is overstated only when the validated accuracy exceeds the all document accuracy by more than the gap"),
        ("e6", "edge", "each shape counts documents once and an unchecked total counts only documents accepted as valid"),
        ("e7", "edge", "the first fix follows the order of what costs most"),
        ("e8", "edge", "percentages are whole numbers rounded half up"),
    ],
    "plants": {
        "wrong-validated-denominator": (["m1", "e5"], "divides the correct documents by the valid ones and calls that the accuracy of the run"),
        "wrong-target-strict": (["e2"], "requires the accuracy to be above the target and not at it"),
        "wrong-segment-strict": (["e4"], "automates a kind only when its accuracy is above the target and not at it"),
        "wrong-min-n-strict": (["m1", "e3"], "automates a kind only when it has more documents than the minimum and not at least that many"),
        "wrong-gap-inclusive": (["e5"], "calls the figure overstated when the gap equals the limit"),
        "wrong-unchecked-any-status": (["e6"], "counts an unchecked total in documents that were never accepted as valid"),
        "wrong-fix-order": (["m1", "e7"], "puts the wasted retries before the invented values in the order of fixes"),
        "wrong-rounding-floor": (["e8"], "rounds a percentage down and not half up"),
        "wrong-empty-meets-target": (["e1"], "says an empty run meets the target"),
    },
}

# ===== Level 3: module 76 (the conversational assistant capstone) =====
PRACTICES[f"{X}/76-scenario-conversational-ai-assistant/unit-01/practice-1"] = {
    "name": "conversation_review", "suite": "ConversationReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed batch gets every count the segments and a verdict"),
        ("e1", "edge", "an empty batch has zero figures and is held for lack of data"),
        ("e2", "edge", "a safety signal counts as missed unless it went to a person as a safety hand off"),
        ("e3", "edge", "a conversation is overlong only above the turn limit and only when nobody took over"),
        ("e4", "edge", "repeated questions are acceptable at exactly the limit and not above it"),
        ("e5", "edge", "a segment is weak only below the resolution floor and not at it"),
        ("e6", "edge", "a segment needs the minimum number of conversations before it can be called weak"),
        ("e7", "edge", "a hand off is over escalation only when no person was needed and no safety signal was present"),
        ("e8", "edge", "only conversations the assistant settled alone count as resolved"),
        ("e9", "edge", "percentages are whole numbers rounded half up"),
    ],
    "plants": {
        "wrong-safety-any-hand-off": (["e2"], "counts a safety signal as handled when any person took over and not a safety hand off"),
        "wrong-overlong-inclusive": (["e3"], "calls a conversation overlong when it has exactly the turn limit"),
        "wrong-repeat-strict": (["e1", "e4"], "accepts repeated questions only below the limit and not at it"),
        "wrong-weak-inclusive": (["m1", "e5"], "calls a segment weak when it is exactly at the resolution floor"),
        "wrong-weak-ignores-min-n": (["m1", "e6"], "calls a small segment weak whatever its size"),
        "wrong-min-n-strict": (["e6"], "needs more conversations than the minimum and not at least that many"),
        "wrong-over-escalated-counts-risk": (["e7"], "counts a safety hand off as over escalation"),
        "wrong-resolved-ignores-hand-off": (["e8"], "counts a conversation a person settled as resolved by the assistant"),
        "wrong-rounding-floor": (["m1", "e9"], "rounds a percentage down and not half up"),
        "wrong-safety-never-holds": (["e2"], "ships the assistant although a safety signal was missed"),
    },
}

# ===== Level 3: module 77 (the agentic tool builder capstone) =====
PRACTICES[f"{X}/77-scenario-agentic-tool-builder/unit-01/practice-1"] = {
    "name": "tool_review", "suite": "ToolReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a well formed read only tool is approved and leaves an audit line"),
        ("e1", "edge", "a description needs at least the minimum number of words"),
        ("e2", "edge", "the timeout and the memory may equal their limits and not exceed them"),
        ("e3", "edge", "a name is lower case snake case of at most sixty four characters"),
        ("e4", "edge", "forbidden calls in the code refuse the tool and are all listed in order"),
        ("e5", "edge", "a permission the code uses without declaring it or a denied one refuses the tool"),
        ("e6", "edge", "a declared write is approved only with a gate and a read alone is approved outright"),
        ("e7", "edge", "a refusal beats a revision and a revision beats a gate"),
        ("e8", "edge", "the permissions the code uses are reported in alphabetical order"),
    ],
    "plants": {
        "wrong-description-inclusive": (["e1"], "calls a description too short when it has exactly the minimum number of words"),
        "wrong-timeout-inclusive": (["e2"], "flags a timeout that equals the limit"),
        "wrong-memory-inclusive": (["e2"], "flags a memory request that equals the limit"),
        "wrong-name-too-long": (["e3"], "rejects a name of exactly sixty four characters"),
        "wrong-forbidden-first-only": (["e4"], "lists only the first forbidden call"),
        "wrong-undeclared-ignored": (["e5"], "does not refuse a permission that the code uses and the proposal does not declare"),
        "wrong-denied-ignored": (["e5"], "does not refuse a denied permission that the proposal declares"),
        "wrong-gate-skipped": (["e6"], "approves a declared write outright with no gate"),
        "wrong-revise-before-refuse": (["e7"], "sends a refused tool back for revision because it also has a finding"),
        "wrong-used-unsorted": (["e8"], "reports the permissions in the order of the scan table and not alphabetically"),
    },
}


# ===== Level 4: modules 79 to 84 =====
PRACTICES[f"{X}/80-end-to-end-and-multi-agent-architecture/unit-01/practice-1"] = {
    "name": "architecture_review", "suite": "ArchitectureReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a sound design passes review with no findings"),
        ("e1", "edge", "a design without a feedback loop or a stage is rejected"),
        ("e2", "edge", "autonomy is flagged only when the path is known"),
        ("e3", "edge", "several agents need independent parts and no shared context"),
        ("e4", "edge", "an unapproved write is a finding only when an audit is needed"),
        ("e5", "edge", "output that nobody validates is flagged"),
        ("e6", "edge", "findings are ordered by severity then rule and the verdict follows the worst"),
        ("e7", "edge", "the cheapest design that is not rejected wins and ties go by name"),
    ],
    "plants": {
        "wrong-no-feedback-check": (["e1"], "never reports a design without a feedback loop"),
        "wrong-input-stage-skipped": (["e1"], "does not require an input stage"),
        "wrong-autonomy-always": (["e2"], "flags every agent design, whether or not the path is known"),
        "wrong-team-shared-only": (["e3"], "flags a team only for a shared context and not for parts that depend on each other"),
        "wrong-team-single-agent": (["e3"], "applies the team rule to a design with one agent"),
        "wrong-write-without-audit": (["e4"], "flags an unapproved write even when no audit is needed"),
        "wrong-validate-inverted": (["e5"], "flags output that has a validation step and not output that lacks one"),
        "wrong-order-by-rule": (["e6"], "orders the findings by rule name and ignores severity"),
        "wrong-any-finding-rejects": (["e6"], "rejects a design for a medium finding"),
        "wrong-rejected-allowed": (["e7"], "lets a rejected design win on price"),
        "wrong-tie-by-position": (["e7"], "breaks a price tie by list position and not by name"),
    },
}

PRACTICES[f"{X}/81-reliability-of-multi-agent-systems/unit-01/practice-1"] = {
    "name": "reliable_agents", "suite": "ReliableAgentsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "tasks run in order and receive the results of the tasks they need"),
        ("e1", "edge", "every retry of a task carries the same idempotency key"),
        ("e2", "edge", "retries stop at the limit and a fatal failure is not retried"),
        ("e3", "edge", "a failure stays inside its branch and dependents are skipped"),
        ("e4", "edge", "a breaker stops calls to an agent that keeps failing and a success resets it"),
        ("e5", "edge", "a fallback degrades one task with its own key and is not checkpointed"),
        ("e6", "edge", "a second run resumes from the checkpoint and retries only what failed"),
        ("e7", "edge", "an unexpected crash is not swallowed and keeps the work already checkpointed"),
    ],
    "plants": {
        "wrong-retry-new-key": (["e1"], "sends a new idempotency key with every retry"),
        "wrong-fatal-retried": (["e2"], "retries a failure that cannot be fixed by retrying"),
        "wrong-attempts-off-by-one": (["e2"], "makes one attempt fewer than the limit"),
        "wrong-dependents-still-run": (["e3"], "runs a task whose dependency failed"),
        "wrong-failure-stops-run": (["e3"], "stops the whole run at the first failed task"),
        "wrong-breaker-total": (["e4"], "counts every failure of an agent and never resets on a success"),
        "wrong-breaker-never-opens": (["e4"], "keeps calling an agent that has failed past the threshold"),
        "wrong-fallback-same-key": (["e5"], "sends the primary's idempotency key to the fallback agent"),
        "wrong-fallback-checkpointed": (["e5"], "checkpoints a degraded result as if it were final"),
        "wrong-resume-reruns": (["e6"], "runs the tasks that the checkpoint already holds"),
        "wrong-crash-swallowed": (["e7"], "turns an unexpected crash into a failed task"),
        "wrong-checkpoint-late": (["e7"], "writes the checkpoint only when the whole run ends"),
    },
}

PRACTICES[f"{X}/82-models-prompts-and-context-as-design-choices/unit-01/practice-1"] = {
    "name": "prompt_plan", "suite": "PromptPlanTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "static modules come first and the breakpoint follows the last one"),
        ("e1", "edge", "a variable in a static module is refused"),
        ("e2", "edge", "dynamic variables are filled and a missing one is refused"),
        ("e3", "edge", "the lowest priority dynamic module is dropped first and a tie drops the later one"),
        ("e4", "edge", "static modules are never dropped and a budget they exceed is refused"),
        ("e5", "edge", "a prefix under the minimum gets no breakpoint"),
        ("e6", "edge", "the cheapest model that meets the tier and the latency wins and ties go by name"),
        ("e7", "edge", "only an identical static prefix can be reused"),
    ],
    "plants": {
        "wrong-static-reversed": (["m1"], "puts the static modules in reverse order"),
        "wrong-breakpoint-after-all": (["m1"], "marks the breakpoint after the last block, dynamic ones included"),
        "wrong-variable-in-static-allowed": (["e1"], "accepts a variable inside a static module"),
        "wrong-missing-variable-blank": (["e2"], "replaces a missing variable with nothing"),
        "wrong-drop-highest-priority": (["e3"], "drops the module with the highest priority first"),
        "wrong-drop-tie-earlier": (["e3"], "drops the earlier of two modules with the same priority"),
        "wrong-over-budget-ignored": (["e4"], "returns a prompt over the budget when only static modules are left"),
        "wrong-minimum-exclusive": (["e5"], "needs more than the minimum for a breakpoint"),
        "wrong-model-tier-ignored": (["e6"], "ignores the capability tier the workload needs"),
        "wrong-model-latency-ignored": (["e6"], "ignores the latency limit"),
        "wrong-model-tie-by-position": (["e6"], "breaks a price tie by list position and not by name"),
        "wrong-reuse-first-block-only": (["e7"], "compares only the first block of the prefix"),
        "wrong-reuse-needs-whole-prompt": (["e7"], "reuses nothing unless the whole prompt, dynamic blocks included, is identical"),
    },
}

PRACTICES[f"{X}/83-deployment-architecture-and-data-handling/unit-01/practice-1"] = {
    "name": "data_policy", "suite": "DataPolicyTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a compliant deployment has no findings"),
        ("e1", "edge", "residency is pinned by the request or by the region the platform allows"),
        ("e2", "edge", "zero data retention is an arrangement of the providers own platforms and not of every model"),
        ("e3", "edge", "protected health information needs a covered platform an agreement and deidentified input"),
        ("e4", "edge", "a multi tenant service needs a workspace for each tenant"),
        ("e5", "edge", "an audit log must not store prompts that hold sensitive data"),
        ("e6", "edge", "audit retention stays between the minimum and the maximum"),
        ("e7", "edge", "purge only what is past the retention limit and not on hold"),
        ("e8", "edge", "a request is served only by a deployment that keeps its data in the region"),
    ],
    "plants": {
        "wrong-geo-unset-passes": (["e1"], "treats a request with no inference geo as pinned to the US"),
        "wrong-eu-pin-allowed": (["e1"], "accepts a European residency requirement on the first-party API"),
        "wrong-any-region-for-us": (["e1"], "accepts any cloud region for a US residency requirement"),
        "wrong-cloud-zdr-ok": (["e2"], "treats the zero data retention flag as valid on a cloud provider's platform"),
        "wrong-fable-zdr-ok": (["e2"], "ignores that the top-tier model needs 30-day retention"),
        "wrong-baa-optional": (["e3"], "does not ask for a signed agreement for protected health information on the API"),
        "wrong-aws-hipaa-ok": (["e3"], "accepts protected health information on a platform without HIPAA readiness"),
        "wrong-redact-not-safe": (["e3"], "accepts only tokenising and rejects redaction as de-identification"),
        "wrong-tenancy-ignored": (["e4"], "does not check the isolation of tenants"),
        "wrong-audit-phi-only": (["e5"], "flags stored prompts only when health data is in scope and not for other identifiers"),
        "wrong-retention-long-inclusive": (["e6"], "flags a retention equal to the maximum as too long"),
        "wrong-retention-short-inclusive": (["e6"], "flags a retention equal to the minimum as too short"),
        "wrong-purge-held": (["e7"], "purges an entry that is on legal hold"),
        "wrong-purge-boundary": (["e7"], "purges an entry that is exactly at the limit"),
        "wrong-global-fallback": (["e8"], "serves a request from a global deployment when the regional one is missing"),
        "wrong-pick-first-listed": (["e8"], "picks the first listed deployment and not the first by name"),
    },
}

PRACTICES[f"{X}/84-cost-and-capacity-engineering/unit-01/practice-1"] = {
    "name": "gateway_budget", "suite": "GatewayBudgetTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a request follows the route table of the gateway"),
        ("e1", "edge", "a team near its budget is moved to a cheaper model and a team over it is refused"),
        ("e2", "edge", "a request is admitted warned or blocked against the budget"),
        ("e3", "edge", "showback adds each teams tokens at the price of the model and refuses an unknown model"),
        ("e4", "edge", "showback rounds each teams total to a cent once"),
        ("e5", "edge", "a caller with a hard latency limit gets accept and poll when the slow case does not fit"),
        ("e6", "edge", "a model pinned by a team is honoured only when the policy allows it"),
    ],
    "plants": {
        "wrong-route-ignores-table": (["m1"], "sends every task to the default model"),
        "wrong-warn-keeps-model": (["e1"], "keeps the expensive model for a team that is near its budget"),
        "wrong-block-still-routes": (["e1"], "routes a request of a team that is over its budget"),
        "wrong-warn-strict": (["e2"], "warns only above 80 percent and not at it"),
        "wrong-block-at-budget": (["e2"], "blocks a request that lands exactly on the budget"),
        "wrong-zero-budget-open": (["e2"], "lets a team with no budget through"),
        "wrong-showback-unknown-free": (["e3"], "prices an unknown model at nothing"),
        "wrong-showback-by-name": (["e3"], "lists teams by name and not by cost"),
        "wrong-showback-row-rounding": (["e4"], "rounds every row to a cent before adding them"),
        "wrong-showback-floors": (["e4"], "rounds a team's total down and not to the nearest cent"),
        "wrong-delivery-strict": (["e5"], "chooses accept and poll when the slow case exactly fits"),
        "wrong-delivery-no-margin": (["e5"], "leaves the safety margin out of the comparison"),
        "wrong-pin-always-honoured": (["e6"], "honours any model a team pins"),
    },
}
# --- L4 79-84 END ---

# ===== Level 4: modules 85 to 89 =====
PRACTICES[f"{X}/85-retrieval-pipelines-at-design-level/unit-01/practice-1"] = {
    "name": "pipeline", "suite": "PipelineTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "sections become chunks that carry their title and section and the version of their document"),
        ("e1", "edge", "a long section splits at sentence ends under the word limit and every part keeps the prefix"),
        ("e2", "edge", "a code outweighs a word and a word in no chunk matches nothing"),
        ("e3", "edge", "a search for a reader never returns a chunk of a document that reader may not see"),
        ("e4", "edge", "the mechanism follows the corpus size then the data shape then the query pattern"),
        ("e5", "edge", "a reindex keeps unchanged documents replaces changed ones adds new ones and drops removed ones"),
        ("e6", "edge", "stale lists the chunks whose document changed or vanished"),
        ("e7", "edge", "recall counts every labelled question and a question with no results is a miss"),
    ],
    "plants": {
        "wrong-no-prefix": (["m1"], "leaves the title and section out of the chunk text"),
        "wrong-no-split": (["e1"], "never splits a long section"),
        "wrong-code-weight": (["e2"], "scores a code like any other word"),
        "wrong-filter-after-cut": (["e3"], "applies the access filter after it has taken the best k, so a reader gets fewer results than exist"),
        "wrong-always-index": (["e4"], "builds an index even for a corpus that fits a cached prompt"),
        "wrong-table-chunked": (["e4"], "cuts a table into chunks and searches them instead of querying it"),
        "wrong-reindex-additive": (["e5"], "keeps the old chunks of a document that changed"),
        "wrong-reindex-keeps-removed": (["e5"], "keeps the chunks of a document that no longer exists"),
        "wrong-stale-ignores-removed": (["e6"], "does not call the chunks of a removed document stale"),
        "wrong-recall-answered-only": (["e7"], "divides by the questions that had results instead of all labelled questions"),
    },
}

PRACTICES[f"{X}/86-integration-choices-access-and-capability-bloat/unit-01/practice-1"] = {
    "name": "capability", "suite": "CapabilityTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an agent loses the tools its role does not need and the risky ones among them are named"),
        ("e1", "edge", "a tool that is held and needed but never used is reported as dormant and never removed"),
        ("e2", "edge", "a small set loads whole and a large one defers when it has ten tools or over ten thousand tokens"),
        ("e3", "edge", "the number of tools kept loaded stays between three and five and ties are broken by name"),
        ("e4", "edge", "the mechanism follows the counterpart then the path then the number of clients"),
        ("e5", "edge", "a call needs the users scope and the agents scope and an unknown tool is refused"),
        ("e6", "edge", "the gateway checks the credential then the model then the tool then the rate"),
        ("e7", "edge", "the gateway keeps a record of every decision with the team or unknown and no content"),
    ],
    "plants": {
        "wrong-risky-everything": (["m1"], "names every removed tool as risky"),
        "wrong-dormant-removed": (["e1"], "removes a needed tool because nobody used it"),
        "wrong-defer-count-only": (["e2"], "ignores the size of the definitions when it decides whether to defer"),
        "wrong-keep-unclamped": (["e3"], "keeps as many tools loaded as it is asked to, with no floor or ceiling"),
        "wrong-ties-by-insertion": (["e3"], "breaks a tie in usage by the order the tools were listed"),
        "wrong-mechanism-path-first": (["e4"], "answers a call in code before it asks whether the counterpart is an agent"),
        "wrong-auth-user-only": (["e5"], "lets any call through that the user may make, whatever the agent holds"),
        "wrong-gateway-rate-first": (["e6"], "counts the rate before it checks the model"),
        "wrong-gateway-audit-empty-on-deny": (["e7"], "keeps no record of a denied request"),
    },
}

PRACTICES[f"{X}/87-observability-at-scale/unit-01/practice-1"] = {
    "name": "triage", "suite": "TriageTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "every trace gets one reason and a healthy trace is kept by its id"),
        ("e1", "edge", "an error outranks a slow root which outranks retries which outrank a flag"),
        ("e2", "edge", "the root cause is the deepest failing span and its path starts at the root"),
        ("e3", "edge", "a stale or empty retrieval is blamed only when no span failed"),
        ("e4", "edge", "drift reports a move in either direction over the tolerance and never divides by zero"),
        ("e5", "edge", "an alert needs consecutive windows over the threshold and a dip starts the count again"),
        ("e6", "edge", "a log record drops the content fields unless they are allowed by name"),
        ("e7", "edge", "a requests trail joins the events of every component in time order"),
    ],
    "plants": {
        "wrong-error-after-slow": (["e1"], "labels a trace that failed and was slow as slow"),
        "wrong-no-feedback": (["m1"], "drops a trace the user flagged as a bad answer"),
        "wrong-retries-ignored": (["e1"], "does not keep a trace in which one tool was called three times"),
        "wrong-root-reporter": (["e2"], "blames the first failing span, which only reported its child's error"),
        "wrong-stale-first": (["e3"], "blames a stale retrieval even when another span failed"),
        "wrong-drift-up-only": (["e4"], "misses a metric that fell"),
        "wrong-alert-no-reset": (["e5"], "keeps counting windows across a window that was under the threshold"),
        "wrong-redact-keeps-content": (["e6"], "keeps prompts and tool inputs in the log record"),
        "wrong-redact-ignores-allowed": (["e6"], "drops a content field even when it is allowed by name"),
        "wrong-trail-unsorted": (["e7"], "lists a request's events in the order they were received"),
    },
}

PRACTICES[f"{X}/88-evaluation-and-optimisation/unit-01/practice-1"] = {
    "name": "evalkit", "suite": "EvalKitTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a segment table reports accuracy and error cost with the costliest segment first"),
        ("e1", "edge", "a segment with no cost entry costs one per error and no results give an empty table"),
        ("e2", "edge", "a percentile uses the nearest rank and does not need sorted input"),
        ("e3", "edge", "a test with fewer cases than the minimum in either arm decides nothing"),
        ("e4", "edge", "a difference is called only when it clears the 95 percent bar and the better side is named"),
        ("e5", "edge", "a shadow run is held for a regression in a protected segment or for more losses than gains"),
        ("e6", "edge", "diagnosis checks the evidence then the grounding then the format then the stronger model"),
        ("e7", "edge", "model choice takes the cheapest option that meets the accuracy floor and the latency limit"),
    ],
    "plants": {
        "wrong-pct-truncated": (["m1"], "rounds an accuracy down instead of to the nearest whole percent"),
        "wrong-sorted-by-name": (["m1"], "orders the report by segment name and not by error cost"),
        "wrong-default-cost-zero": (["e1"], "makes the errors of a segment with no cost entry free"),
        "wrong-percentile-floor": (["e2"], "takes the rank rounded down, so the 95th percentile of four values is the third"),
        "wrong-percentile-unsorted": (["e2"], "reads the percentile from the values in the order they arrived"),
        "wrong-ab-min-ignored": (["e3"], "gives a verdict on a handful of cases"),
        "wrong-ab-always-new": (["e4"], "names the new version as better whenever it clears the bar, even when it is worse"),
        "wrong-ab-90-percent": (["e4"], "uses the 90 percent bar and calls a difference that could be chance"),
        "wrong-gate-net-only": (["e5"], "ships a version that lost a right answer in a protected segment because it gained as many elsewhere"),
        "wrong-gate-protected-only": (["e5"], "ships a version that lost more answers than it gained in segments that are not protected"),
        "wrong-diagnose-grounding-late": (["e6"], "checks the format before it asks whether the answer is supported by the evidence"),
        "wrong-choose-ignores-latency": (["e7"], "picks a model that is too slow because it meets the accuracy floor"),
        "wrong-choose-ties-by-order": (["e7"], "breaks a tie in cost by the order the options were listed"),
    },
}

PRACTICES[f"{X}/89-migration-and-model-upgrades-at-scale/unit-01/practice-1"] = {
    "name": "rollout", "suite": "RolloutTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a change that regresses nothing and stays inside its limits gets a go with no reasons"),
        ("e1", "edge", "a must pass case that fails blocks the change and the ids are listed in order"),
        ("e2", "edge", "a protected segment that lost answers blocks the change even when gains elsewhere match the losses"),
        ("e3", "edge", "more losses than gains blocks the change and both counts are named"),
        ("e4", "edge", "a cost rise over the limit blocks the change and a rise exactly at the limit does not"),
        ("e5", "edge", "the tail is the nearest rank 95th percentile and a single slow case does not block"),
        ("e6", "edge", "a roll out advances when healthy holds with too few requests and rolls back to zero when errors pass the limit"),
        ("e7", "edge", "the retirement calendar counts days ranks the nearest first and names the level"),
        ("e8", "edge", "migration removes the settings the new model refuses and names each change"),
    ],
    "plants": {
        "wrong-must-pass-ignored": (["e1"], "lets a change through although a case that must pass now fails"),
        "wrong-protected-ignored": (["e2"], "ignores a loss in a protected segment when the totals are equal"),
        "wrong-net-loss-ignored": (["e3"], "does not block a change that loses more answers than it gains"),
        "wrong-cost-at-limit": (["e4"], "blocks a cost rise that equals the limit"),
        "wrong-cost-rounded": (["e4"], "rounds the cost rise up to the next whole percent instead of down"),
        "wrong-p95-max": (["e5"], "judges the tail by the slowest single case"),
        "wrong-rollout-min-ignored": (["e6"], "moves a stage on before it has seen enough requests"),
        "wrong-rollout-no-rollback": (["e6"], "keeps advancing a roll-out whose errors passed the limit"),
        "wrong-days-by-name": (["e7"], "lists the models by name and not by how soon they retire"),
        "wrong-level-boundary": (["e7"], "calls a model with exactly fourteen days left merely something to migrate"),
        "wrong-migrate-keeps-sampling": (["e8"], "leaves the sampling settings in the migrated request"),
        "wrong-migrate-keeps-forced-tool": (["e8"], "leaves a forced tool choice in the migrated request"),
    },
}
# --- PRACTICES ABOVE ---


def camel(title):
    words = title.split()
    return words[0] + "".join(w[:1].upper() + w[1:] for w in words[1:])


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modules", default=".*")
    rx = re.compile(ap.parse_args().modules)
    selected = {p: s for p, s in PRACTICES.items() if rx.match(p.split("/")[1])}
    if not selected:
        sys.exit("no practice matches")
    problems = 0
    for practice, spec in selected.items():
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
