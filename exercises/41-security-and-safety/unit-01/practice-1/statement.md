# Practice: an injection-resistant tool gate

An agent that reads the web, mail or files is exposed to text that someone else wrote and that may carry instructions. The defence is
not one clever prompt. It is layers around the model: untrusted text is kept apart from your instructions, every tool call is checked
against least privilege before it runs, a session that has read untrusted text gets less freedom, secrets never reach logs, and every
decision is recorded. In this practice you write the code layer: a small gate that sits between the model's tool calls and the tools.
It is a design the course proposes from the documented advice (untrusted content only in tool results and JSON-encoded, least privilege,
screening, a hook that decides before the permission prompt); it is not a feature of any product. Pick your language folder (`python`,
`typescript`, `java` or `kotlin`), open `starter/` and edit the file there. Nothing here calls a model or the network: the tests call
the gate the way a hook would.

## The given parts

The Java and Kotlin folders give you `Json` (parse text into maps, lists, strings, numbers, booleans and null, and write them back
compactly, keys in insertion order). Python and TypeScript have JSON built in. Decisions and records are plain data: a map with string
keys in every language.

## What to write

Names are written in Python style; TypeScript uses camelCase (`wrapUntrusted`, `markUntrusted`, `hookResponse`, `tainted` as a getter,
`audit` as a getter); Java and Kotlin put the three functions on the class as static or companion members (`Gate.screen`,
`Gate.wrapUntrusted`, `Gate.redact`, `Gate.hookResponse`) and use `isTainted()` / `isTainted`, `audit()` and `alerts()` as methods.

- `screen(text)` returns the names of the injection signals found in the text, in this order: `override`, `role-tag`, `exfiltrate`,
  `reveal`. All four patterns ignore letter case and let `.` match a newline.
  - `override`: `\b(ignore|disregard|forget)\b.{0,40}\b(previous|prior|above|earlier|system)\b.{0,20}\b(instructions?|prompts?|rules)\b`
  - `role-tag`: `<\s*/?\s*(system|assistant|tool_result|instructions?)\s*>`
  - `exfiltrate`: `\b(send|email|forward|post|upload)\b.{0,60}\b(to|at)\b.{0,40}[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+`
  - `reveal`: `\b(reveal|print|show|repeat)\b.{0,40}\b(system prompt|password|secret|api key)\b`
- `wrap_untrusted(tool_use_id, source, content)` returns a tool result block. For clean text it is
  `{"type": "tool_result", "tool_use_id": ..., "content": <one line of compact JSON>}` where the JSON is the object
  `{"source": source, "trust": "untrusted", "content": content}` with those keys in that order. When `screen` finds a signal it
  returns `{"type": "tool_result", "tool_use_id": ..., "is_error": true, "content": "Content from <source> withheld: possible prompt
  injection (<signals joined by ", ">)"}` and nothing of the text.
- `redact(text)` replaces, in this order: `sk-ant-[A-Za-z0-9_-]{8,}` with `[SECRET]`; `AKIA` followed by 16 capital letters or digits,
  as a whole word, with `[SECRET]`; `Bearer ` followed by 16 or more of `[A-Za-z0-9._-]` with `Bearer [SECRET]`; an address,
  `[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+`, with `[EMAIL]`; and a card number with `[CARD]`. A card number is 13 to 19
  digits, written with at most one space or hyphen between digits, that passes the Luhn check; a long number that fails the check is
  left as it is.
- `Gate(root, allowed_hosts, allowed_email_domains)` decides tool calls. `decide(actor, tool, args)` returns
  `{"decision": "allow" | "ask" | "deny", "reason": ...}` and appends a record to the audit. The tools are `read_file(path)`,
  `write_file(path, content)`, `bash(command)`, `fetch(url)` and `send_email(to, subject, body)`; any other name is
  `deny` with the reason `unknown tool`. An allow has the reason `ok`. A missing string argument counts as an empty string.
  - **Paths** (`read_file`, `write_file`): turn backslashes into `/`, join a relative path to `root`, drop `.` parts and apply `..`
    (never above the top). A path not equal to `root` and not under `root + "/"` is `deny`, `outside the project`. A secret file is
    `deny`, `secret file`: a name `.env`, or starting `.env.` unless it is `.env.example`, a folder called `secrets` anywhere in the
    path, or a name ending in `.pem` or `.key`. A write into a folder called `.git` or `.claude` is `deny`, `protected path`.
  - **`bash`**: first, a word that is `sudo` or `rm` (or ends in `/sudo` or `/rm`) is `deny`, `dangerous command`. Then a command
    containing any of `;` `&` `|` `>` `<` a backtick, `$(` or a newline is `deny`, `chaining or redirection`. Then the first word must be
    `ls`, `cat`, `pytest` or `git`, and `git` only with `status`, `diff` or `log` as its next word; anything else, and an empty command,
    is `deny`, `command not allowed`. An argument that does not start with `-` and names a secret file is `deny`, `secret file`.
  - **`fetch`**: read the URL as `scheme://authority path ?query #fragment` with the pattern
    `([A-Za-z][A-Za-z0-9+.-]*)://([^/?#]*)([^?#]*)(\?[^#]*)?(#.*)?` over the whole text. A URL that does not match, or whose scheme
    is not `https`, is `deny`, `https only`. An authority with an `@` is `deny`, `credentials in the URL`. The host is the authority
    in lower case without a `:port`; it must equal an allowed host or end with a dot and an allowed host, else `deny`,
    `host not allowed`.
  - **`send_email`**: the recipient's domain (after the last `@`, lower case) must be in the allowed domains, else `deny`,
    `recipient not allowed`. If `redact(subject + "\n" + body)` differs from the text, `deny`, `sensitive data in the body`.
  - **Taint**: `mark_untrusted(source)` marks the session. Afterwards `write_file`, and `bash` with `pytest`, return `ask` with
    `untrusted content in this session`; a `fetch` whose URL has a query or a fragment returns `ask` with
    `data could leave in the URL`; `send_email` returns `deny` with `a person must send it`. Reads, the other bash commands and a
    plain `fetch` stay `allow`. Every `deny` above still wins over an `ask`: check the other rules first. `tainted` tells whether the
    session is marked; a new `Gate` is clean.
  - **Audit**: `audit` is the list of records so far, each `{"actor", "tool", "decision", "reason", "args"}`, where `args` is the
    call's arguments with every string value passed through `redact` and other values left as they are.
  - `alerts()` lists the actors with three or more `deny` records, each as `{"actor", "denials"}` with the actor's current count,
    ordered by when each reached three. An `ask` is not a denial.
- `hook_response(decision)` gives the answer of a `PreToolUse` hook as `{"exit_code", "stdout", "stderr"}`. An `allow` is exit code 0 with
  nothing printed. A `deny` or an `ask` is exit code 0 with the compact JSON
  `{"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": <decision>, "permissionDecisionReason": <reason>}}` on
  standard output and an empty standard error.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Untrusted text is delivered as one JSON string that names its source and says it is untrusted |
| `e1` | The screen names the signals, ignores clean text, and a flagged result is withheld with an error that shows none of the text |
| `e2` | Paths stay inside the project, secrets and protected folders are refused, a sibling folder with a similar name is outside |
| `e3` | Bash is limited to read-only commands; dangerous, chained and redirected commands and secret arguments are refused |
| `e4` | Fetch obeys the host list (a lookalike host fails) and refuses credentials; email obeys the domain list and the body check |
| `e5` | After untrusted content entered the session, changes ask, an email is refused, reads still work, and a denial stays a denial |
| `e6` | Redaction covers keys, addresses and Luhn-valid cards, leaves other numbers, and the audit never holds a raw secret |
| `e7` | The hook answer has the documented shapes and repeated denials by one actor raise an alert, while questions do not |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
