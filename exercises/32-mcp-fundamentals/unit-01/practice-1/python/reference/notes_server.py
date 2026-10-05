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
    """Refuse a bad note with a ToolError (given the title and the text, both already stripped)."""
    if not title:
        raise ToolError("title is required")
    if not text:
        raise ToolError("text is required")
    if len(text) > MAX_TEXT:
        raise ToolError(f"text is too long (max {MAX_TEXT})")


def validate_search(query: str, limit: int) -> None:
    """Refuse a bad search with a ToolError (given the stripped query and the limit)."""
    if not query:
        raise ToolError("query is required")
    if not 1 <= limit <= 20:
        raise ToolError("limit must be between 1 and 20")


def find_hits(query: str) -> list[str]:
    """The lines `{id}. {title}` of the notes whose title or text contains the query, in any letter case, in id order."""
    needle = query.lower()
    return [f"{i}. {title}" for i, (title, text) in enumerate(NOTES, 1) if needle in title.lower() or needle in text.lower()]


def format_hits(hits: list[str], limit: int, query: str) -> str:
    """The answer of a search: at most `limit` hit lines joined by newlines, or the no-match sentence."""
    return "\n".join(hits[:limit]) if hits else f'No notes match "{query}"'


def count_text(count: int) -> str:
    """The text of the count resource: 0 notes, 1 note, 2 notes."""
    return f"{count} note" + ("" if count == 1 else "s")


def note_text(id: str) -> str:
    """The text of one note, or a ResourceNotFoundError "No note {id}" when the id is not a whole number of an existing note."""
    if not id.isdigit() or not 1 <= int(id) <= len(NOTES):
        raise ResourceNotFoundError(f"No note {id}")
    title, text = NOTES[int(id) - 1]
    return f"{title}\n\n{text}"


def review_text(tone: str) -> str:
    """The text of the review prompt."""
    if not NOTES:
        return "There are no notes to review."
    return f"Review these notes in a {tone} tone:\n" + "\n".join(f"- {title}" for title, _ in NOTES)


def default_limit() -> int:
    """How many hits a search returns when the caller gives no limit."""
    return 5


def search_annotations():
    """The annotations that tell a client search_notes only reads."""
    return ToolAnnotations(read_only_hint=True)


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
