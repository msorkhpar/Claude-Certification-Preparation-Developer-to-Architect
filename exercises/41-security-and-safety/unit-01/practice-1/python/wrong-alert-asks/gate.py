"""An injection-resistant tool gate. See ../../statement.md."""
import json
import re

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
    """A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged."""
    signals = screen(content)
    if signals:
        return {"type": "tool_result", "tool_use_id": tool_use_id, "is_error": True,
                "content": f"Content from {source} withheld: possible prompt injection ({', '.join(signals)})"}
    payload = json.dumps({"source": source, "trust": "untrusted", "content": content}, separators=(",", ":"))
    return {"type": "tool_result", "tool_use_id": tool_use_id, "content": payload}


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
        return "[CARD]" if 13 <= len(digits) <= 19 and _luhn(digits) else m.group(0)

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
    parts = path.replace("\\", "/").split("/")
    base = parts[-1]
    return (base == ".env" or (base.startswith(".env.") and base != ".env.example") or "secrets" in parts[:-1] or base.endswith((".pem", ".key")))


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
        if not words or words[0] not in ("ls", "cat", "pytest", "git") or (words[0] == "git" and (len(words) < 2 or words[1] not in ("status", "diff", "log"))):
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
        if domain not in self.allowed_email_domains:
            return _result("deny", "recipient not allowed")
        text = f"{args.get('subject', '')}\n{args.get('body', '')}"
        if redact(text) != text:
            return _result("deny", "sensitive data in the body")
        if self._tainted:
            return _result("deny", "a person must send it")
        return _result("allow")

    def alerts(self):
        """Actors with three or more denials, ordered by when each reached three, with their denial count."""
        counts, order = {}, []
        for record in self._audit:
            if record["decision"] != "allow":
                counts[record["actor"]] = counts.get(record["actor"], 0) + 1
                if counts[record["actor"]] == 3:
                    order.append(record["actor"])
        return [{"actor": a, "denials": counts[a]} for a in order]


def hook_response(decision):
    """The PreToolUse hook answer for a decision: exit code, standard output, standard error."""
    if decision["decision"] == "allow":
        return {"exit_code": 0, "stdout": "", "stderr": ""}
    out = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": decision["decision"], "permissionDecisionReason": decision["reason"]}}
    return {"exit_code": 0, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}
