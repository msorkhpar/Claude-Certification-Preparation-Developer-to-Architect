"""Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md."""
from claude_agent_sdk import ClaudeAgentOptions, ResultMessage, query


def resolve_name(name, index):
    # TODO: the id of the one session with this name; refuse an unknown or a shared name with ValueError.
    return None


def plan_session(record, current, now, fork=False):
    # TODO: fresh, fresh_with_summary, resume_with_notice or resume, with the changed, deleted and added files.
    return None


def change_notice(plan):
    # TODO: the text that tells a resumed session which files differ.
    return None


def build_summary(findings, decisions, open_questions, files):
    # TODO: the fixed-layout summary for a fresh session.
    return None


def first_prompt(plan, task, summary=""):
    # TODO: put the notice or the summary in front of the task.
    return None


def session_options(plan, **extra):
    # TODO: resume by id, fork together with it, nothing for a fresh start.
    return None


def continue_options(sessions_in_directory, **extra):
    # TODO: continue only when exactly one session exists.
    return None


async def run_session(prompt, options):
    # TODO: run one query; return {"session_id", "result", "error"}.
    return None
