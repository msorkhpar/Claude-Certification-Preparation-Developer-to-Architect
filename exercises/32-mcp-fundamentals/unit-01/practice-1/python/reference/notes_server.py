"""A notes server for the Model Context Protocol, over stdio. See ../../statement.md."""
from mcp.server.mcpserver import MCPServer
from mcp.server.mcpserver.exceptions import ResourceNotFoundError, ToolError
from mcp.types import ToolAnnotations

MAX_TEXT = 500
server = MCPServer("notes", version="1.0.0")
NOTES: list[tuple[str, str]] = []  # (title, text); the id of a note is its position, counting from 1


@server.tool(description="Save a note with a title and a text.", annotations=ToolAnnotations(read_only_hint=False, destructive_hint=False, idempotent_hint=False))
def add_note(title: str, text: str) -> str:
    title, text = title.strip(), text.strip()
    if not title:
        raise ToolError("title is required")
    if not text:
        raise ToolError("text is required")
    if len(text) > MAX_TEXT:
        raise ToolError(f"text is too long (max {MAX_TEXT})")
    NOTES.append((title, text))
    return f"Saved note {len(NOTES)}: {title}"


@server.tool(description="Find notes whose title or text contains the query.", annotations=ToolAnnotations(read_only_hint=True))
def search_notes(query: str, limit: int = 5) -> str:
    if not query.strip():
        raise ToolError("query is required")
    if not 1 <= limit <= 20:
        raise ToolError("limit must be between 1 and 20")
    needle = query.strip().lower()
    hits = [f"{i}. {title}" for i, (title, text) in enumerate(NOTES, 1) if needle in title.lower() or needle in text.lower()]
    return "\n".join(hits[:limit]) if hits else f'No notes match "{query.strip()}"'


@server.resource("notes://count", description="How many notes there are.")
def count() -> str:
    return f"{len(NOTES)} note" + ("" if len(NOTES) == 1 else "s")


@server.resource("notes://note/{id}", description="One note by id.")
def note(id: str) -> str:
    if not id.isdigit() or not 1 <= int(id) <= len(NOTES):
        raise ResourceNotFoundError(f"No note {id}")
    title, text = NOTES[int(id) - 1]
    return f"{title}\n\n{text}"


@server.prompt(description="Ask for a review of the notes.")
def review_notes(tone: str = "brief") -> str:
    if not NOTES:
        return "There are no notes to review."
    return f"Review these notes in a {tone} tone:\n" + "\n".join(f"- {title}" for title, _ in NOTES)


if __name__ == "__main__":
    server.run()
