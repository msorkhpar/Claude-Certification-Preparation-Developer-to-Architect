"""An injection-resistant tool gate. See ../../statement.md."""


def screen(text):
    """The names of the injection signals found in the text, in the fixed order: override, role-tag, exfiltrate, reveal."""
    return []


def wrap_untrusted(tool_use_id, source, content):
    """A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged."""
    return {"type": "tool_result", "tool_use_id": tool_use_id, "content": content}


def redact(text):
    """Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]."""
    return text


class Gate:
    def __init__(self, root, allowed_hosts, allowed_email_domains):
        self.root = root
        self.allowed_hosts = allowed_hosts
        self.allowed_email_domains = allowed_email_domains

    @property
    def tainted(self):
        return False

    @property
    def audit(self):
        return []

    def mark_untrusted(self, source):
        pass

    def decide(self, actor, tool, args):
        return {"decision": "allow", "reason": "ok"}

    def alerts(self):
        return []


def hook_response(decision):
    """The PreToolUse hook answer for a decision: exit code, standard output, standard error."""
    return {"exit_code": 0, "stdout": "", "stderr": ""}
