# Practice: a notes server for the Model Context Protocol

An MCP server is a program that a client starts and talks to over its standard input and output (the `stdio` transport), or reaches over
HTTP. It offers three kinds of things: tools that a model can call, resources that a person or an application can read, and prompts that
a person can pick. The official SDKs do the protocol work (the handshake, the JSON-RPC framing, the schema validation); you write what the
server offers. Write a small notes server and prove it the way a real client would: the tests start your server as a separate process and
connect the SDK's own client to it. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file
there (`notes_server.py`, `notes_server.ts`, `NotesServer.java` or `NotesServer.kt`). The server uses the official SDK of its language at the
versions in `docs/VERSIONS.md`; the lesson pages explain the SDK calls and what the course checked. Nothing here touches a network: the
client and the server talk through pipes.

## What the server offers

The server is named `notes`, version `1.0.0`, and keeps its notes in memory. The id of a note is its position, counting from 1. It declares
the tools, resources and prompts capabilities.

- Tool `add_note(title, text)`, with a description. Both arguments are required strings. It strips the spaces around both, then refuses:
  an empty title (`title is required`), an empty text (`text is required`), a text longer than 500 characters (`text is too long (max
  500)`). A refusal is a **tool error**: an ordinary result whose `isError` is true and whose text says why, so the model can read it and
  try again (it is not a protocol error). Otherwise it saves the note and answers `Saved note {id}: {title}`. A refused call does not use an
  id. The tool is annotated as not read-only, not destructive and not idempotent.
- Tool `search_notes(query, limit)`, with a description. `query` is a required string, `limit` an optional integer with the default 5 that
  the tool's input schema states. The query is stripped; an empty one is the tool error `query is required`; a limit outside 1 to 20 is the
  tool error `limit must be between 1 and 20`. A note matches when its title or its text contains the query, in any letter case. The answer
  is one line per match, `{id}. {title}`, in id order, at most `limit` lines; with no match it is `No notes match "{query}"` (the stripped
  query), which is not an error. The tool is annotated as read-only.
- Resource `notes://count` (plain text): `0 notes`, `1 note`, `2 notes`, and so on.
- Resource template `notes://note/{id}` (plain text): the note as `{title}`, an empty line, then `{text}`. An id that is not a whole number
  of an existing note (`0`, `3` of two notes, `abc`) is a protocol error whose message says `No note {id}`.
- Prompt `review_notes` with one optional argument `tone` (default `brief`): one user message. With no notes it says `There are no notes to
  review.`; otherwise `Review these notes in a {tone} tone:` followed by one line `- {title}` per note.

A message you write for a tool error may arrive with a prefix that the SDK adds (some SDKs prepend the tool name); the tests look for your
text inside the result.

## What is already written, and what you write

The starter is a working notes server with nine gaps cut out of it. Everything that is plumbing is written and correct: the server name and
version, the capabilities, the tool, resource and prompt registrations with their descriptions, schemas and annotations, the stripping of the
inputs, the saving of a note and the answer to `add_note`. Each gap is a small function with its signature, a comment that says what it
receives and returns with one example, and the cases it unlocks; it returns a neutral value, so the starter runs and fails the cases on an
assertion. To debug a gap, log its input with the `log` line at the top of the file (the starter already logs the input of `add_note` this way);
a run shows the logged lines under the failing case. Write them in this order (the names are the Python forms; TypeScript, Java and Kotlin use
camel case, `validateNote` is `noteError` there and returns the message or `null` instead of raising):

1. `validate_note` unlocks `e2` and `e7`: the three refusals of a bad note, in order.
2. `validate_search` unlocks `e2`: the query and the limit checks.
3. `find_hits` unlocks `m1` and `e3`: the matching notes as `{id}. {title}` lines, ignoring letter case.
4. `format_hits` unlocks `e3`: the limit and the no-match sentence.
5. `count_text` unlocks `e5`: `0 notes`, `1 note`, `2 notes`.
6. `note_text` unlocks `m1` and `e5`: the note as text, or the `No note {id}` error.
7. `review_text` unlocks `e6`: the prompt text, with and without notes.
8. `default_limit` unlocks `e1`: the limit a search uses when none is given, also advertised in the input schema (`defaultLimit` in the other languages).
9. `search_annotations` unlocks `e4`: the read-only annotation of `search_notes` (`searchAnnotations` in the other languages).

About fifteen lines in all. The rest of the registration is written.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A client can save a note, find it and read it back |
| `e1` | The server declares tools, resources and prompts, and names its tools |
| `e2` | Bad input comes back as a tool error the model can read |
| `e3` | Search ignores case, keeps id order, honours the limit and says when nothing matches |
| `e4` | Tool annotations tell a client which tool only reads |
| `e5` | Resources give the count, a note by id and an error for a missing one |
| `e6` | The prompt lists the notes and defaults the tone |
| `e7` | Ids are sequential and a failed call does not use one |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file. The Java and Kotlin tests start the
server with `java -cp`, using the class path that the build file hands them.
