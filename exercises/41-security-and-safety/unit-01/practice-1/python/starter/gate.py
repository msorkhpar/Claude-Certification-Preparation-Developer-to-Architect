"""An injection-resistant tool gate. See ../../statement.md."""
import logging
import json
import re

log = logging.getLogger(__name__)

TOOLS = {"read_file", "write_file", "bash", "fetch", "send_email"}
SIGNALS = {
    "override": re.compile(r"\b(ignore|disregard|forget)\b.{0,40}\b(previous|prior|above|earlier|system)\b.{0,20}\b(instructions?|prompts?|rules)\b", re.I | re.S),
    "role-tag": re.compile(r"<\s*/?\s*(system|assistant|tool_result|instructions?)\s*>", re.I),
    "exfiltrate": re.compile(r"\b(send|email|forward|post|upload)\b.{0,60}\b(to|at)\b.{0,40}[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+", re.I | re.S),
    "reveal": re.compile(r"\b(reveal|print|show|repeat)\b.{0,40}\b(system prompt|password|secret|api key)\b", re.I | re.S),
}
SHELL_TRICKS = (";", "&", "|", ">", "<", "`", "$(", "\n")


def screen(text):
    """The names of the injection signals found in the text, in the fixed order of SIGNALS."""
    return [name for name, pattern in SIGNALS.items() if pattern.search(text)]


def wrap_untrusted(tool_use_id, source, content):
    """TODO 1 of 6 (unlocks m1 and e1): wrap untrusted text as a tool_result block.

    Receives the tool_use_id, where the text came from and the text. Returns a tool_result dict. Clean text becomes one line of
    compact JSON (`separators=(",", ":")`) with the keys source, trust ("untrusted") and content, in that order. When `screen`
    finds a signal, return the error block from the statement instead and leave the text out of it.
    Example: wrap_untrusted("t1", "web page", "hi")
      -> {"type": "tool_result", "tool_use_id": "t1", "content": '{"source":"web page","trust":"untrusted","content":"hi"}'}
    """
    log.debug("wrap_untrusted input: %r", content)
    return {"type": "tool_result", "tool_use_id": tool_use_id, "content": content}


def _luhn(digits):
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:
            d = d * 2 - 9 if d * 2 > 9 else d * 2
        total += d
    return total % 10 == 0


def redact(text):
    """Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]."""
    text = re.sub(r"sk-ant-[A-Za-z0-9_-]{8,}", "[SECRET]", text)
    text = re.sub(r"\bAKIA[0-9A-Z]{16}\b", "[SECRET]", text, flags=re.A)
    text = re.sub(r"Bearer [A-Za-z0-9._-]{16,}", "Bearer [SECRET]", text)
    text = re.sub(r"[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)+", "[EMAIL]", text)

    def card(m):
        digits = re.sub(r"[ -]", "", m.group(0))
        return "[CARD]" if _is_card(digits) else m.group(0)

    return re.sub(r"\b(?:[0-9][ -]?){12,18}[0-9]\b", card, text, flags=re.A)


def _resolve(root, path):
    path = path.replace("\\", "/")
    stack = []
    for part in ((path if path.startswith("/") else root + "/" + path)).split("/"):
        if part in ("", "."):
            continue
        if part == "..":
            if stack:
                stack.pop()
        else:
            stack.append(part)
    return "/" + "/".join(stack)


def _is_secret(path):
    """TODO 2 of 6 (unlocks e2): does this path name a secret file?

    Receives a path and returns True for a file named `.env`, a file starting `.env.` unless it is `.env.example`, any file inside
    a folder called `secrets`, and a name ending in `.pem` or `.key`; False otherwise. Backslashes count as `/`.
    Example: _is_secret("app/.env.local") -> True, _is_secret(".env.example") -> False
    """
    return False


def _command_allowed(words):
    """TODO 3 of 6 (finish this to pass e3): is the first word of a bash command on the allow-list?

    Receives the command split into words. Returns True for `ls`, `cat`, `pytest`, and `git` followed by `status`, `diff` or
    `log`; False for anything else, including an empty list. (The checks for sudo, rm, chaining and secret arguments are written.)
    Example: _command_allowed(["git", "log", "--oneline"]) -> True, _command_allowed(["git", "push"]) -> False
    """
    return False


def _is_card(digits):
    """TODO 4 of 6 (finish this to pass e6): is this run of digits a card number?

    Receives the digits with spaces and hyphens already removed. Returns True when there are 13 to 19 of them and `_luhn` accepts
    them (a long number that fails the check is not a card). Example: _is_card("4111111111111111") -> True, _is_card("4111111111111112") -> False
    """
    return False


def _result(decision, reason="ok"):
    return {"decision": decision, "reason": reason}


class Gate:
    def __init__(self, root, allowed_hosts, allowed_email_domains):
        self.root = _resolve("/", root)
        self.allowed_hosts = [h.lower() for h in allowed_hosts]
        self.allowed_email_domains = [d.lower() for d in allowed_email_domains]
        self._tainted = False
        self._audit = []

    @property
    def tainted(self):
        return self._tainted

    @property
    def audit(self):
        return list(self._audit)

    def mark_untrusted(self, source):
        self._tainted = True

    def _inside(self, path):
        return path == self.root or path.startswith(self.root + "/")

    def decide(self, actor, tool, args):
        result = self._decide(tool, args)
        clean = {k: (redact(v) if isinstance(v, str) else v) for k, v in args.items()}
        self._audit.append({"actor": actor, "tool": tool, "decision": result["decision"], "reason": result["reason"], "args": clean})
        return result

    def _decide(self, tool, args):
        if tool not in TOOLS:
            return _result("deny", "unknown tool")
        if tool in ("read_file", "write_file"):
            path = _resolve(self.root, str(args.get("path", "")))
            if not self._inside(path):
                return _result("deny", "outside the project")
            if _is_secret(path):
                return _result("deny", "secret file")
            if tool == "write_file":
                if {".git", ".claude"} & set(path.split("/")):
                    return _result("deny", "protected path")
                if self._tainted:
                    return _result("ask", "untrusted content in this session")
            return _result("allow")
        if tool == "bash":
            return self._bash(str(args.get("command", "")))
        if tool == "fetch":
            return self._fetch(str(args.get("url", "")))
        return self._email(args)

    def _bash(self, command):
        words = command.split()
        if any(w.rsplit("/", 1)[-1] in ("sudo", "rm") for w in words):
            return _result("deny", "dangerous command")
        if any(t in command for t in SHELL_TRICKS):
            return _result("deny", "chaining or redirection")
        if not _command_allowed(words):
            return _result("deny", "command not allowed")
        if any(_is_secret(w) for w in words[1:] if not w.startswith("-")):
            return _result("deny", "secret file")
        if words[0] == "pytest" and self._tainted:
            return _result("ask", "untrusted content in this session")
        return _result("allow")

    def _fetch(self, url):
        m = re.fullmatch(r"([A-Za-z][A-Za-z0-9+.-]*)://([^/?#]*)([^?#]*)(\?[^#]*)?(#.*)?", url)
        if not m or m.group(1).lower() != "https":
            return _result("deny", "https only")
        if "@" in m.group(2):
            return _result("deny", "credentials in the URL")
        host = re.sub(r":[0-9]+$", "", m.group(2).lower())
        if not any(host == h or host.endswith("." + h) for h in self.allowed_hosts):
            return _result("deny", "host not allowed")
        if self._tainted and (m.group(4) or m.group(5)):
            return _result("ask", "data could leave in the URL")
        return _result("allow")

    def _email(self, args):
        to = str(args.get("to", ""))
        domain = to.rsplit("@", 1)[-1].lower() if "@" in to else ""
        text = f"{args.get('subject', '')}\n{args.get('body', '')}"
        return self._email_decision(domain, text)

    def _email_decision(self, domain, text):
        """TODO 5 of 6 (finish this to pass e4 and e5): decide a send_email call.

        Receives the recipient's lower-case domain and the subject and body joined by a newline. Returns `_result(decision, reason)`:
        deny `recipient not allowed` when the domain is not in `self.allowed_email_domains`; deny `sensitive data in the body` when
        `redact(text)` differs from the text; deny `a person must send it` when the session is tainted (`self._tainted`); else allow.
        Example: a clean mail to "example.com" in a tainted session -> {"decision": "deny", "reason": "a person must send it"}
        """
        return _result("allow")

    def alerts(self):
        """TODO 6 of 6 (finish this to pass e7): actors with three or more denials.

        Reads `self._audit` (records with "actor" and "decision"). Returns a list of {"actor", "denials"}, one per actor with three or
        more `deny` records (an `ask` is not a denial), with that actor's current count, ordered by when each reached three.
        Example: alice denied 3 times, then bob 3 times, then alice once more -> [{"actor": "alice", "denials": 4}, {"actor": "bob", "denials": 3}]
        """
        return []


def hook_response(decision):
    """The PreToolUse hook answer for a decision: exit code, standard output, standard error."""
    if decision["decision"] == "allow":
        return {"exit_code": 0, "stdout": "", "stderr": ""}
    out = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": decision["decision"], "permissionDecisionReason": decision["reason"]}}
    return {"exit_code": 0, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}
