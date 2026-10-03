# Practice: a refund desk that cannot skip identity

A customer-support agent has four tools: verify the customer's identity, look up an order, process a refund and escalate to a person. A
prompt can ask the model to verify identity first, but a prompt is a request, and requests fail at some small rate: on one conversation in a
few hundred, the model calls `process_refund` first. Money has moved by then. The fix is not a stronger sentence. It is a gate in the code
that runs the tools: the prerequisite is checked every time, the call that breaks it never reaches the backend, and the model receives an
error it can act on. When the case needs a person, the agent hands over a structured record, not a transcript. Write that gate. Pick your
language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. Nothing here touches the network: the
backend is a set of functions that the tests script, and the tests play the model by making the calls in the order it would.

## The given parts

| Name | Meaning |
|---|---|
| `backend` | a map from a tool name (`verify_identity`, `lookup_order`, `process_refund`, `escalate`) to a function from the input map to a result map (string and whole-number values); it may throw |
| `verify_identity` result | `verified` is `yes` or `no`; when yes it also has `customer_id` |
| `lookup_order` result | `order_id`, `customer_id`, `total_cents` and `refunded_cents` (whole numbers) |
| `process_refund` input | `order_id` and `amount_cents`; the result has a `refund_id` |

## What to write

A class `RefundDesk(backend, limit_cents=10000)` (`RefundDesk(backend, limitCents)` in TypeScript and Kotlin, a constructor with an optional
limit in Java) with three methods. In Python and TypeScript the names are `state`, `call` and `handoff`; they are the same in the others.

- `call(name, args)` returns `{"content": text, "is_error": bool, "blocked": code or null}`.
  - A result that came from the backend has the content `key=value; key=value` (the result's pairs in order, joined by `; `), `is_error`
    false and `blocked` null. A backend call that throws gives the exception's message as content, `is_error` true and `blocked` null,
    and changes nothing in the desk. Every call that reaches the backend, even one that throws, is logged by name.
  - A call that the desk refuses never reaches the backend. Its result is `BLOCKED {code}: {message}` with `is_error` true and `blocked` set
    to the code. Every refusal is also kept, in order, as `{"tool", "code"}`.
  - Check in this order. A tool name that is not one of the four: `unknown_tool`, message `Unknown tool: NAME`. `escalate` is always allowed
    and goes straight to the backend. Otherwise, when the desk is locked: `locked`. `verify_identity` goes to the backend (see below).
    Every other call needs a verified customer: `identity_required`. Then `lookup_order` goes to the backend. Then `process_refund` is
    checked: the order must have been looked up in this session (`order_not_checked`); `amount_cents` must be a whole number above zero, and
    a boolean, a decimal, a string or a missing value is not (`bad_amount`); it must not be more than the order's `total_cents` minus its
    `refunded_cents` (`exceeds_order`); and it must not be above `limit_cents` (`needs_human`: a refund over the limit is never executed).
    Only then does the backend run.
  - Identity. A `verify_identity` result with `verified` equal to `yes` and a `customer_id` makes that customer the verified one and resets
    the count of failures. Any other result clears the verified customer, adds a failure and, at three failures in a row, locks the desk.
    The desk never trusts an earlier success once a check has failed.
  - Orders. A `lookup_order` result whose `customer_id` is not the verified customer is refused with `order_not_owned`, and nothing of it is
    kept or shown. A result that belongs to the customer is remembered, and its `refunded_cents` grows with each refund the desk makes.
  - The messages: `identity_required` "Verify the customer's identity before this action."; `order_not_owned` "That order does not belong
    to the verified customer."; `order_not_checked` "Look up the order before refunding it."; `bad_amount` "The amount must be a positive
    whole number of cents."; `exceeds_order` "The amount is more than what is left to refund on the order."; `needs_human` "Refunds over the
    limit need a person."; `locked` "Too many failed identity checks; escalate to a person."
  - A successful refund is kept as `{"order_id", "amount_cents", "refund_id"}`.
- `state()` returns a snapshot: `{"customer": the verified id or null, "locked", "failures", "orders": {id: the order as looked up, with its
  current refunded_cents}, "refunds": [...], "blocked": [...], "backend_calls": [tool names]}`. A change to the snapshot must not change the desk.
- `handoff(reason)` returns the record a person needs: `{"customer_id", "identity_verified": bool, "reason", "orders_checked": [ids],
  "refunds_done": [...], "blocked": [...], "recommended_action"}`. The recommended action is `verify_identity_manually` when the desk is
  locked, otherwise `review_refund` when the most recent refusal was `needs_human`, otherwise `review_case`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A verified customer looks up an order and is refunded within the limit; the desk records it |
| `e1` | A refund before identity is verified is refused in code and never reaches the backend |
| `e2` | Another customer's order is not shown, not remembered and not refundable; lookup also needs identity |
| `e3` | A refund is checked against the looked-up order, the amount and what is left |
| `e4` | A refund over the limit is not executed, and the hand-off says what a person needs to decide |
| `e5` | A failed check unlocks nothing; three in a row lock the desk, and only escalation still works |
| `e6` | Unknown tools and backend errors are reported, and the hand-off lists every refusal |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
