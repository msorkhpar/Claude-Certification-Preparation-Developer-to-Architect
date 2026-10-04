"""The turn logic of a conversational assistant in miniature: a message is routed in code before the model sees it, the window keeps the pinned facts and the newest turns
that fit, and a memory that crosses sessions is read per customer and marked when it is old.

The words are made up and the routing is a plain list of phrases: this example is about where each decision lives, not about what a model would say. The shapes (a route,
a window, a recalled fact with a status) are this course's design, not an Anthropic interface.
"""
from datetime import date

RISK = ("hurt myself", "end my life", "emergency")
ASKS_FOR_PERSON = ("human", "a person", "an agent")
STORE = {"ada": [("address", "12 Elm Road", "2026-09-20"), ("plan", "Plus", "2025-12-01")]}


def route(message, misses=0):
    """Decided in code, in this order: a signal of risk, a request for a person, a stalled conversation, otherwise the model answers."""
    text = message.lower()
    if any(phrase in text for phrase in RISK):
        return "handoff:safety"
    if any(phrase in text for phrase in ASKS_FOR_PERSON):
        return "handoff:requested"
    if misses >= 2:
        return "handoff:stalled"
    return "answer"


def window(turns, budget, facts):
    """The pinned facts always stay; of the turns, the newest ones that fit the budget (counted in words) stay, as one block at the end."""
    kept, used = [], 0
    for turn in reversed(turns):
        words = len(turn.split())
        if used + words > budget:
            break
        kept.insert(0, turn)
        used += words
    return {"kept": kept, "dropped": len(turns) - len(kept), "facts": facts}


def recall(user, today, max_age_days=30):
    """The facts saved for this customer only, each marked current or to be verified when it is older than the limit."""
    out = []
    for key, value, saved in STORE.get(user, []):
        age = (date.fromisoformat(today) - date.fromisoformat(saved)).days
        out.append((key, value, "current" if age <= max_age_days else "verify"))
    return out


def main():
    for message, misses in (("Where is my parcel?", 0), ("I want to talk to a human", 0), ("I feel like I might hurt myself", 0), ("what?", 2)):
        print(f"route '{message}'" + (f" after {misses} misses" if misses else "") + f": {route(message, misses)}")
    turns = ["Hello", "My parcel has not arrived", "It was due on Monday", "Can you check the order", "Order 1234 please"]
    facts = ["order 1234: parcel due 2026-09-28", "address: 12 Elm Road"]
    w = window(turns, 12, facts)
    print(f"window: {len(turns)} turns, budget 12 words -> kept {len(w['kept'])}, dropped {w['dropped']}, facts kept {len(w['facts'])}")
    for user in ("ada", "bob"):
        found = recall(user, "2026-10-04")
        print(f"recall {user} on 2026-10-04: " + (", ".join(f"{k}={v} ({s})" for k, v, s in found) or "nothing stored"))


if __name__ == "__main__":
    main()
