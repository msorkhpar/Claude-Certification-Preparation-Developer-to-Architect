"""Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md."""
from claude_agent_sdk import ClaudeAgentOptions, ResultMessage, query
import logging

log = logging.getLogger(__name__)

WEEK_SECONDS = 7 * 24 * 3600  # a session idle for longer than this is not resumed
STALE_SHARE = 0.5  # more than this share of the analysed files changed or gone: start fresh


def resolve_name(name, index):
    """The id of the one session with this exact name; an unknown or a shared name is refused."""
    ids = [entry["id"] for entry in index if entry["name"] == name]
    if not ids:
        raise ValueError(f"no session is named {name}")
    if len(ids) > 1:
        raise ValueError(f"{len(ids)} sessions are named {name}: resume by id")
    return ids[0]


def plan_session(record, current, now, fork=False):
    """Decide how to continue from a saved record ({id, name, last_used, files: {path: digest}}) given the files as they are now."""
    log.debug("plan_session input: %r", record)
    if record is None:
        return {"action": "fresh", "session_id": None, "changed": [], "deleted": [], "added": [], "fork": False}
    before = record["files"]
    # TODO 1 of 10 (finish this to pass m1, e1): the differences. Receives the saved digests `before` and the digests
    #   `current` (path -> digest). Fill `changed` (in both, digest differs), `deleted` (saved, now gone) and `added` (new
    #   now), each sorted by path. Example: before {a: 1, b: 2}, current {a: 9, c: 3} -> changed [a], deleted [b], added
    #   [c].
    changed, deleted, added = [], [], []
    # TODO 2 of 10 (finish this to pass m1, e2): the share. Return the share of the saved files that changed or are
    #   gone: (changed + deleted) / saved files, and 0 when nothing was saved. New files do not count. Example: 4 saved, 1
    #   changed, 1 deleted -> 0.5.
    share = 0.0
    # TODO 3 of 10 (finish this to pass e3): the age rule. Start fresh with a summary when the share is above
    #   STALE_SHARE or the session has been idle for more than WEEK_SECONDS (now minus last_used). Example: idle 7 days
    #   exactly -> resume; 7 days and 1 second -> fresh_with_summary.
    if share > STALE_SHARE:
        action = "fresh_with_summary"
    elif changed or deleted or added:
        action = "resume_with_notice"
    else:
        action = "resume"
    resumed = action in ("resume", "resume_with_notice")
    # TODO 4 of 10 (finish this to pass e4): the fork flag of the plan. `resumed` is true when the action is resume or
    #   resume_with_notice. The plan forks only when fork was asked for and the session is resumed. Example: fork asked on
    #   a stale session -> fork false.
    return {"action": action, "session_id": record["id"] if resumed else None, "changed": changed, "deleted": deleted, "added": added, "fork": bool(fork)}


def change_notice(plan):
    """What to tell a resumed session: the files that differ from what it analysed, and to read them again."""
    lines = []
    # TODO 5 of 10 (finish this to pass e5): the notice lines. For each of the lists changed, deleted and added of the
    #   plan that is not empty, add one line `- label: a, b` (labels changed, deleted, new). Example: changed [a.py] -> `-
    #   changed: a.py`.
    if not lines:
        return ""
    return "\n".join(["Since your earlier analysis:", *lines, "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."])


def _clean(items):
    seen = []
    # TODO 6 of 10 (finish this to pass e6): the cleaning of a list for the summary. Receives the items (text or none).
    #   Return them trimmed, without blank items and without repeats, in first-seen order. Example: [" a ", "", "a", "b"]
    #   -> [a, b].
    seen = [str(item) for item in items or []]
    return seen


def build_summary(findings, decisions, open_questions, files):
    """A fixed-layout summary for a fresh session: conclusions and the state they rest on, never raw tool output."""
    parts = []
    for title, items in (("Findings", _clean(findings)), ("Decisions", _clean(decisions)), ("Open questions", _clean(open_questions))):
        parts.append(f"## {title}\n" + ("\n".join(f"- {item}" for item in items) if items else "- none"))
    listed = [f"- {path} ({files[path]})" for path in sorted(files)]
    parts.append("## Files\n" + ("\n".join(listed) if listed else "- none"))
    return "\n\n".join(parts)


def first_prompt(plan, task, summary=""):
    # TODO 7 of 10 (finish this to pass e5): the first prompt. Return the text the next run starts with: for
    #   resume_with_notice the change notice, a blank line, then the task; for fresh_with_summary the summary, a blank
    #   line, then the task; otherwise the task. Example: action resume -> the task unchanged.
    return task


def session_options(plan, **extra):
    """SDK options for a plan: resume by id, and fork together with it. A fresh start sets neither."""
    options = dict(extra)
    # TODO 8 of 10 (finish this to pass e7): the options of a plan. When the plan has a session id, set the resume
    #   option to it, and the fork option to true only when the plan forks; set neither for a fresh plan. Example: plan
    #   with session s1 and fork true -> resume s1 and fork true.
    return options


def continue_options(sessions_in_directory, **extra):
    """`continue` takes the most recent session of the directory, so it is only safe when there is exactly one."""
    # TODO 9 of 10 (finish this to pass e7): the guard of `continue`. When the directory does not hold exactly one
    #   session, refuse with an error that names the count and says to resume one by id. Example: three sessions -> error
    #   "3 sessions in this directory: resume one by id".
    return {**extra, "continue_conversation": True}


async def run_session(prompt, options):
    """Run one single-shot query and keep the session id, which every result carries, and the error the SDK raises after an error result."""
    session_id, result, error = None, None, None
    try:
        async for message in query(prompt=prompt, options=ClaudeAgentOptions(**options)):
            # TODO 10 of 10 (finish this to pass e8): the reading of the result message. When a message is the result
            #   message, keep its session id and its result text. Example: a result with session s9 -> session_id s9, even
            #   if the SDK then raises an error.
            pass
    except Exception as exc:  # the error result, if there was one, has already been read
        error = str(exc)
    return {"session_id": session_id, "result": result, "error": error}
