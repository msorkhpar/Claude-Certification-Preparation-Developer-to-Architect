"""Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.

The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
"""
import re

PATTERNS = [("EMAIL", re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")), ("MEMBER", re.compile(r"\bM-\d{6}\b"))]


def tokenise(text, vault):
    """Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local."""
    for label, pattern in PATTERNS:
        def swap(match):
            value = match.group(0)
            if value not in vault:
                vault[value] = f"<{label}_{sum(t.startswith(f'<{label}_') for t in vault.values()) + 1}>"
            return vault[value]
        text = pattern.sub(swap, text)
    return text


def restore(text, vault):
    for value, token in vault.items():
        text = text.replace(token, value)
    return text


def audit_entry(request_id, original, vault):
    """What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt."""
    return {"request_id": request_id, "chars": len(original), "tokens_issued": len(vault), "prompt_stored": False}


def main():
    original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice."
    vault = {}
    sent = tokenise(original, vault)
    print("sent to the model:", sent)
    reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim."
    print("model reply (illustrative):", reply)
    print("restored for the user:", restore(reply, vault))
    entry = audit_entry("req-001", original, vault)
    print("audit entry:", " ".join(f"{k}={v}" for k, v in entry.items()))
    print("raw values in the entry:", any(value in str(entry) for value in vault))
    print("gap: the name survives tokenising:", "Jane Doe" in sent)


if __name__ == "__main__":
    main()
