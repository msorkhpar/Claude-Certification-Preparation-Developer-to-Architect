"""A notes server for the Model Context Protocol, over stdio. See ../../statement.md."""
import logging

from mcp.server.mcpserver import MCPServer
from mcp.server.mcpserver.exceptions import ResourceNotFoundError, ToolError
from mcp.types import ToolAnnotations

log = logging.getLogger(__name__)

MAX_TEXT = 500
server = MCPServer("notes", version="1.0.0")
NOTES: list[tuple[str, str]] = []  # (title, text); the id of a note is its position, counting from 1


def validate_note(title: str, text: str) -> None:
    """GAP 1 of 9 (unlocks e2 and e7): refuse a bad note.

    Receives the title and the text, both already stripped. Returns nothing; raises ToolError("title is required") for an empty title,
    "text is required" for an empty text and f"text is too long (max {MAX_TEXT})" for a text longer than MAX_TEXT, checked in that order.
    Example: validate_note("", "x") raises ToolError("title is required"); validate_note("T", "x") returns None.
    """
    return None


def validate_search(query: str, limit: int) -> None:
    """GAP 2 of 9 (unlocks e2): refuse a bad search.

    Receives the stripped query and the limit. Returns nothing; raises ToolError("query is required") for an empty query and
    "limit must be between 1 and 20" for a limit outside 1 to 20, in that order.
    Example: validate_search("x", 21) raises ToolError("limit must be between 1 and 20").
    """
    return None


def find_hits(query: str) -> list[str]:
    """GAP 3 of 9 (unlocks m1 and e3): the notes that match a search.

    Receives the stripped query. Returns the lines `{id}. {title}` of the notes (NOTES, ids count from 1) whose title or text contains
    the query in any letter case, in id order.
    Example: with notes ("Alpha", "x") and ("beta", "ALPHA again"), find_hits("alpha") -> ["1. Alpha", "2. beta"]
    """
    return []


def format_hits(hits: list[str], limit: int, query: str) -> str:
    """GAP 4 of 9 (unlocks e3): the answer of a search.

    Receives the hit lines, the limit and the stripped query. Returns at most `limit` lines joined by newlines; with no hits the sentence
    No notes match "<query>".
    Example: format_hits(["1. A", "2. B"], 1, "a") -> "1. A"; format_hits([], 5, "zeta") -> 'No notes match "zeta"'
    """
    return ""


def count_text(count: int) -> str:
    """GAP 5 of 9 (unlocks e5): the text of the count resource.

    Receives the number of notes. Returns "0 notes", "1 note", "2 notes" and so on.
    Example: count_text(1) -> "1 note"
    """
    return ""


def note_text(id: str) -> str:
    """GAP 6 of 9 (unlocks m1 and e5): the text of one note.

    Receives the id from the URI as a string. Returns the title, an empty line, then the text. An id that is not a whole number of an
    existing note ("0", "3" of two notes, "abc") raises ResourceNotFoundError(f"No note {id}").
    Example: with one note ("Plan", "ship it"), note_text("1") -> "Plan\n\nship it"
    """
    return ""


def review_text(tone: str) -> str:
    """GAP 7 of 9 (unlocks e6): the text of the review prompt.

    Receives the tone. With no notes it is "There are no notes to review."; otherwise "Review these notes in a <tone> tone:" and one
    line "- <title>" per note, each after a newline.
    Example: with one note titled "Plan", review_text("brief") -> "Review these notes in a brief tone:\n- Plan"
    """
    return ""


def default_limit() -> int:
    """GAP 8 of 9 (unlocks e1): how many hits a search returns when the caller gives no limit.

    Takes nothing; returns that number, which is also the default the tool's input schema advertises.
    Example: default_limit() -> 5
    """
    return 0


def search_annotations():
    """GAP 9 of 9 (unlocks e4): the annotations that tell a client search_notes only reads.

    Takes nothing; returns ToolAnnotations with read_only_hint True.
    Example: search_annotations().read_only_hint -> True
    """
    return None


@server.tool(description="Save a note with a title and a text.", annotations=ToolAnnotations(read_only_hint=False, destructive_hint=False, idempotent_hint=False))
def add_note(title: str, text: str) -> str:
    log.debug("add_note input: %r %r", title, text)
    title, text = title.strip(), text.strip()
    validate_note(title, text)
    NOTES.append((title, text))
    return f"Saved note {len(NOTES)}: {title}"


@server.tool(description="Find notes whose title or text contains the query.", annotations=search_annotations())
def search_notes(query: str, limit: int = default_limit()) -> str:
    validate_search(query.strip(), limit)
    return format_hits(find_hits(query.strip()), limit, query.strip())


@server.resource("notes://count", description="How many notes there are.")
def count() -> str:
    return count_text(len(NOTES))


@server.resource("notes://note/{id}", description="One note by id.")
def note(id: str) -> str:
    return note_text(id)


@server.prompt(description="Ask for a review of the notes.")
def review_notes(tone: str = "brief") -> str:
    return review_text(tone)


if __name__ == "__main__":
    server.run()
