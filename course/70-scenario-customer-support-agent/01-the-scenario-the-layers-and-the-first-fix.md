# The support agent: the scenario, its layers and the first fix

**Level:** Architect · **Module 70:** Scenario: customer support agent · **Page 1 of 2**
**Exams:** A1, A2, A5; S1

**After this page you can** describe the exam's support agent and the four tools it works through, route a symptom in its logs to the failure shape behind it and to the first fix, say which layer of the dispatcher stops which failure and in which order the layers run, and read a run of seven incidents through that dispatcher.

Checked on 2026-10-04 against Anthropic's engineering article on building effective agents and the Architect exam guide (version 1.0, scenario 1 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin with no model and no client: the model is a script of the calls it asks for, so the example is about the code that runs the tools. The page is a capstone: it uses what modules 45, 48, 52, 53 and 54 taught, and it names modules 64 (Keeping what matters in long conversations) and 65 (Escalation and ambiguity) where the exam's third domain needs them.

## Why it matters

The Architect exam draws four of its six scenarios for each sitting, so a given scenario turns up in about two sittings out of three. The support agent is the one that touches every domain: the loop and its prerequisites, the tool descriptions and errors, and the judgement of when to hand a case to a person. Its questions have one shape. A log line is shown, a team member proposes a fix, and the options are four fixes of very different weight. The skill is not knowing that a gate exists. It is naming the failure behind the line and picking the proportionate fix first, and knowing which of the four candidate fixes cannot work at all.

## The idea

### The scenario in plain words

A company builds a customer support resolution agent on the Claude Agent SDK. It handles requests that are ambiguous by nature: returns, billing disputes, account problems. It reaches the company's systems through four custom tools served over the Model Context Protocol: `get_customer`, `lookup_order`, `process_refund` and `escalate_to_human`. The target is a first-contact resolution of 80 percent or better, together with the discipline to escalate what an agent should not decide. Those two aims pull against each other: an agent that never escalates reaches a high rate by deciding things it should not, and one that escalates everything is safe and useless.

The four tools sort into three kinds. `get_customer` and `lookup_order` read. `process_refund` changes the world and cannot be undone. `escalate_to_human` is the way out. The design of the agent follows that sort: reading is cheap and free, the money tool sits behind everything that can be checked, and the way out is never gated.

### Read a symptom as a failure shape

Every question of the scenario starts from something a team would see in production. The table gives the shapes this course has taught, with the fix to make first and where it was taught.

| What the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| In some conversations the agent calls `lookup_order` or `process_refund` without ever calling `get_customer`, and refunds land on the wrong account | A step order that is only requested | A prerequisite enforced in the code that runs the tools | Module 48 |
| `check my order #4471` often gets `get_customer`, and both tools have a one-line description and accept similar identifiers | Descriptions that leave the model nothing to choose by | Longer descriptions: the formats each tool accepts, an example request and when to use its neighbour instead | Module 52 |
| A lookup times out and the agent apologises, or retries a call that cannot work | An error with no category | A structured error: what failed, whether it can be retried, what to try next | Module 53 |
| A refund above the limit was issued | A limit that lives in the prompt | A cap in the dispatcher, and a person above it | Modules 48 and 54 |
| The agent escalates easy cases and settles policy exceptions alone, and the rate sits well under 80 percent | Unclear decision boundaries | Explicit escalation criteria with worked examples | Module 61 and module 65 (Escalation and ambiguity) |
| Two customers share a name and the agent acts on the first | An ambiguous match treated as a unique one | Return the matches and ask the customer for something that tells them apart | Module 65 (Escalation and ambiguity) |
| Late in a long chat the order number is forgotten, or facts of an earlier customer show up | Case facts that live only in the transcript, and a state shared between cases | Keep the facts outside the transcript, and one state per case | Module 64 (Keeping what matters in long conversations) |

The sixth and seventh rows are where this module goes beyond what the earlier pages built, and the example runs both. The last column is the answer to "where did we learn this", and the rest of this page is about the order in which the fixes are made.

### Money before rate, and the proportionate fix before the large one

When two failure shapes appear together, the order of the fixes is not the order of how many sessions each affects. A refund that moved without a check cannot be called back, and a session that escalated for no reason cost a person a few minutes. The first fix goes to what cannot be undone. Within the rest, the first fix is the cheapest change that addresses the cause the logs show. The exam's third sample question for this scenario is built on exactly this: an agent escalates simple replacements with photo evidence and tries to settle cases that need a policy exception, and the options are explicit criteria with examples, a self-reported confidence score, a separate classifier trained on past tickets, and sentiment analysis. The key is the criteria, because the cause is a decision boundary nobody wrote down. The score is rejected because the model is already confidently wrong on the hard cases, the classifier because it is machinery for a problem nobody has tried to fix with words, and sentiment because how upset a customer is does not tell you how hard the case is.

The practice on page 2 turns this reasoning into a function: it reads a month of recorded sessions, counts each failure shape, and returns the first fix in that order.

### The layers of the dispatcher

Page 1 of module 48 put the checks of an agent in the code that runs its tools. For the whole support agent the dispatcher has more to do, and the order in which it does it matters. The example's dispatcher runs these steps for each call the model asks for:

1. **The way out first.** `escalate_to_human` is answered at once, with no prerequisite, from the state the dispatcher holds. A person must be reachable even when identity cannot be established.
2. **The stall guard.** If the same call, with the same arguments, has been made three times in a row, the dispatcher stops the run and escalates with the trigger `stalled`. This is the iteration cap of module 45 made useful: the cap only ends a run, and the guard ends it with a hand-off.
3. **Identity.** `get_customer` finds the customer. One match sets the verified customer. Several matches set nothing and return the refusal `ambiguous_match` with a sentence that tells the model to ask for an e-mail address and not to pick one. No match returns `not_found` with the same advice.
4. **Everything that needs a customer is refused until there is one.** The refusal is `identity_required`, and it names the next step.
5. **Ownership.** An order that belongs to someone else is refused as `order_not_owned`, and the message does not say whose it is.
6. **The order was looked up** before a refund (`order_not_checked`), and **the amount is within the limit**; above it the refusal is `needs_human`, which tells the model that the next step is to escalate.
7. **The backend call**, and a result with a code, a flag for whether a retry can help, and a sentence. A retryable error (a timeout of the order service) is retried once by the loop, which is the loop's business, not the model's. A permanent error is never retried.

Two properties hold the layers together. First, the state belongs to the case: the example builds a new desk for each conversation, so a customer's identity, the orders looked up and the refunds made cannot reach another case, which is the scope isolation that module 65 (Escalation and ambiguity) teaches against cross-customer contamination, and the reason a long-lived shared state is a defect. Second, the hand-off record is built from the desk's own state. It carries the trigger, the model's reason as one field, whether identity was verified, the orders checked, the refunds made and the refusals, so a person does not read the transcript to find out what happened (module 48, page 2).

What stays outside the dispatcher is judgement. Whether a damaged-item claim deserves a refund, how to word a refusal kindly, and whether a request falls into a gap the policy does not cover are decisions for the model, guided by criteria and examples in the prompt, and above the limit for a person. The line between the two is the one module 48 drew: a rule with a right answer goes in code, and a judgement goes to the model.

### The three triggers for a person

The exam names three reasons to escalate, and each has a different home.

| Trigger | What decides it | In the example |
|---|---|---|
| The customer asks for a person | Nothing but the request: the agent honours it, and does not first investigate or argue | The model's `escalate_to_human` call is accepted with no prerequisite |
| A gap or a conflict in the policy | The model's judgement, from criteria and examples written down | A refund above the limit: the dispatcher refuses with `needs_human` and the model escalates |
| Stalled progress | Code, from the calls themselves | The same call three times in a row |

The building-effective-agents article gives the idea its general form: "Agents can then pause for human feedback at checkpoints or when encountering blockers." The same article names what an agent should rely on while it works: "it's crucial for the agents to gain 'ground truth' from the environment at each step (such as tool call results or code execution) to assess its progress." The dispatcher's results are that ground truth, and the stall guard is a rule about it: when the results stop changing, there is no progress.

Sentiment, a count of exclamation marks and the model's own confidence are not on the list. They are the tempting triggers the exam rejects, because none of them says whether the case is within what the agent may decide.

### The example

The example runs seven incidents, each a script of the calls a model asked for, through the dispatcher above. It prints one line per incident, and an extra line with the hand-off record when the case was escalated. The incidents are the ones from the table: a model that skips identity, a transient fault, a refund over the limit, someone else's order, two customers with the same name, a customer who asks for a person, and a run that makes no progress. The customers, the orders and the incidents are invented, and the limit of 100.00, the stall count of three and the codes are this course's design.

<!-- example: m70-support-desk tabs: python,typescript,java,kotlin -->
```python
"""A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.

The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
design, not an Anthropic interface.
"""
LIMIT = 10_000  # cents: a refund above it is a person's decision
STALL = 3       # the same call this many times in a row is no progress

CUSTOMERS = {"C1": ("Ana Silva", "ana@example.com"), "C2": ("Ana Silva", "ana.s@example.com"), "C3": ("Ben Ortiz", "ben@example.com")}
ORDERS = {"O1": ("C3", 5000), "O2": ("C3", 30000), "O3": ("C1", 4000)}


class Desk:
    """One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another."""

    def __init__(self):
        self.customer, self.checked, self.refunds, self.refused = None, [], [], []
        self.backend, self.recent, self.escalation, self.last_code, self.retries = [], [], None, None, 0
        self.flaky = {"O2"}  # the first lookup of this order fails with a transient fault

    def fail(self, code, message, retryable=False):
        self.last_code = code
        if code not in ("transient", "not_found"):
            self.refused.append(code)
        return {"ok": False, "code": code, "retryable": retryable, "message": message}

    def escalate(self, trigger, reason):
        """The record a person reads, built from the desk's own state and not from the model's account."""
        self.escalation = {"trigger": trigger, "reason": reason, "customer": self.customer, "verified": self.customer is not None,
                           "orders": sorted(self.checked), "refunds": list(self.refunds), "refused": list(self.refused)}
        return {"ok": True, "content": "handed over"}

    def call(self, tool, args):
        self.last_code = None
        self.recent = (self.recent + [(tool, sorted(args.items()))])[-STALL:]
        if tool == "escalate_to_human":  # the way to a person never waits for a prerequisite
            return self.escalate(args["trigger"], args["reason"])
        if len(self.recent) == STALL and len(set(map(str, self.recent))) == 1:
            return self.escalate("stalled", f"{tool} repeated {STALL} times without progress")
        if tool == "get_customer":
            found = [c for c, (name, email) in CUSTOMERS.items() if args["query"] in (name, email)]
            if len(found) > 1:
                return self.fail("ambiguous_match", f"{len(found)} customers match. Ask for the e-mail address. Do not pick one.")
            if not found:
                return self.fail("not_found", "No customer matches. Ask for the e-mail address.")
            self.customer = found[0]
            return {"ok": True, "content": f"customer_id={found[0]}"}
        if self.customer is None:
            return self.fail("identity_required", "Identify the customer with get_customer before this action.")
        owner, total = ORDERS.get(args.get("order_id"), (None, 0))
        if tool == "lookup_order":
            self.backend.append(tool)
            if args["order_id"] in self.flaky:
                self.flaky.discard(args["order_id"])
                return self.fail("transient", "The order service timed out. Retry.", retryable=True)
            if owner is None:
                return self.fail("not_found", "No such order.")
            if owner != self.customer:
                return self.fail("order_not_owned", "That order does not belong to the verified customer.")
            self.checked.append(args["order_id"])
            return {"ok": True, "content": f"total_cents={total}"}
        if tool == "process_refund":
            if args["order_id"] not in self.checked:
                return self.fail("order_not_checked", "Look up the order before refunding it.")
            if args["amount_cents"] > LIMIT:
                return self.fail("needs_human", "A refund above the limit is decided by a person. Escalate.")
            self.backend.append(tool)
            self.refunds.append(f"{args['order_id']}:{args['amount_cents']}")
            return {"ok": True, "content": f"refund_id=R{len(self.refunds)}"}
        return self.fail("unknown_tool", f"No tool named {tool}.")


def run(script):
    """The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk."""
    desk = Desk()
    for tool, args in script:
        result = desk.call(tool, args)
        if not result["ok"] and result.get("retryable"):
            desk.retries += 1
            result = desk.call(tool, args)
    outcome = "escalated" if desk.escalation else "asked" if desk.last_code == "ambiguous_match" else "resolved"
    return desk, outcome


def join(items):
    return ",".join(items) or "-"


INCIDENTS = [
    ("skips identity", [("lookup_order", {"order_id": "O1"}), ("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O1"}),
                        ("process_refund", {"order_id": "O1", "amount_cents": 2000})]),
    ("transient fault", [("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O2"}), ("process_refund", {"order_id": "O2", "amount_cents": 8000})]),
    ("over the limit", [("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O2"}), ("process_refund", {"order_id": "O2", "amount_cents": 25000}),
                        ("escalate_to_human", {"trigger": "needs_human", "reason": "refund of 250.00 asked"})]),
    ("someone else's order", [("get_customer", {"query": "ana@example.com"}), ("lookup_order", {"order_id": "O1"})]),
    ("two customers match", [("get_customer", {"query": "Ana Silva"})]),
    ("asks for a person", [("escalate_to_human", {"trigger": "customer_request", "reason": "customer asked for a person"})]),
    ("no progress", [("get_customer", {"query": "ben@example.com"})] + [("lookup_order", {"order_id": "O9"})] * 3),
]


def main():
    for name, script in INCIDENTS:
        desk, outcome = run(script)
        print(f"{name:<21} outcome={outcome:<9} refused={join(desk.refused)} retries={desk.retries} refunds={join(desk.refunds)} backend={join(desk.backend)}")
        if desk.escalation:
            e = desk.escalation
            print(f"  handoff: trigger={e['trigger']} verified={'yes' if e['verified'] else 'no'} customer={e['customer'] or '-'} orders={join(e['orders'])} refunds={join(e['refunds'])} refused={join(e['refused'])}")


if __name__ == "__main__":
    main()
```
```text
skips identity        outcome=resolved  refused=identity_required retries=0 refunds=O1:2000 backend=lookup_order,process_refund
transient fault       outcome=resolved  refused=- retries=1 refunds=O2:8000 backend=lookup_order,lookup_order,process_refund
over the limit        outcome=escalated refused=needs_human retries=1 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=needs_human verified=yes customer=C3 orders=O2 refunds=- refused=needs_human
someone else's order  outcome=resolved  refused=order_not_owned retries=0 refunds=- backend=lookup_order
two customers match   outcome=asked     refused=ambiguous_match retries=0 refunds=- backend=-
asks for a person     outcome=escalated refused=- retries=0 refunds=- backend=-
  handoff: trigger=customer_request verified=no customer=- orders=- refunds=- refused=-
no progress           outcome=escalated refused=- retries=0 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=stalled verified=yes customer=C3 orders=- refunds=- refused=-
```
```typescript
// A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
//
// The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
// runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
// design, not an Anthropic interface.
export const LIMIT = 10_000; // cents: a refund above it is a person's decision
export const STALL = 3; // the same call this many times in a row is no progress

const CUSTOMERS: Record<string, [string, string]> = { C1: ["Ana Silva", "ana@example.com"], C2: ["Ana Silva", "ana.s@example.com"], C3: ["Ben Ortiz", "ben@example.com"] };
const ORDERS: Record<string, [string, number]> = { O1: ["C3", 5000], O2: ["C3", 30000], O3: ["C1", 4000] };

export type Args = Record<string, any>;
export type Call = [string, Args];
export type Result = { ok: boolean; code?: string; retryable?: boolean; message?: string; content?: string };
export type Escalation = { trigger: string; reason: string; customer: string | null; verified: boolean; orders: string[]; refunds: string[]; refused: string[] };

/** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
export class Desk {
  customer: string | null = null;
  checked: string[] = [];
  refunds: string[] = [];
  refused: string[] = [];
  backend: string[] = [];
  recent: string[] = [];
  escalation: Escalation | null = null;
  lastCode: string | null = null;
  retries = 0;
  flaky = new Set(["O2"]); // the first lookup of this order fails with a transient fault

  fail(code: string, message: string, retryable = false): Result {
    this.lastCode = code;
    if (code !== "transient" && code !== "not_found") this.refused.push(code);
    return { ok: false, code, retryable, message };
  }

  /** The record a person reads, built from the desk's own state and not from the model's account. */
  escalate(trigger: string, reason: string): Result {
    this.escalation = { trigger, reason, customer: this.customer, verified: this.customer !== null, orders: [...this.checked].sort(), refunds: [...this.refunds], refused: [...this.refused] };
    return { ok: true, content: "handed over" };
  }

  call(tool: string, args: Args): Result {
    this.lastCode = null;
    this.recent = [...this.recent, JSON.stringify([tool, Object.entries(args).sort()])].slice(-STALL);
    if (tool === "escalate_to_human") return this.escalate(args.trigger, args.reason); // the way to a person never waits for a prerequisite
    if (this.recent.length === STALL && new Set(this.recent).size === 1) return this.escalate("stalled", `${tool} repeated ${STALL} times without progress`);
    if (tool === "get_customer") {
      const found = Object.entries(CUSTOMERS).filter(([, [name, email]]) => args.query === name || args.query === email).map(([id]) => id);
      if (found.length > 1) return this.fail("ambiguous_match", `${found.length} customers match. Ask for the e-mail address. Do not pick one.`);
      if (found.length === 0) return this.fail("not_found", "No customer matches. Ask for the e-mail address.");
      this.customer = found[0];
      return { ok: true, content: `customer_id=${found[0]}` };
    }
    if (this.customer === null) return this.fail("identity_required", "Identify the customer with get_customer before this action.");
    const [owner, total] = ORDERS[args.order_id] ?? [null, 0];
    if (tool === "lookup_order") {
      this.backend.push(tool);
      if (this.flaky.delete(args.order_id)) return this.fail("transient", "The order service timed out. Retry.", true);
      if (owner === null) return this.fail("not_found", "No such order.");
      if (owner !== this.customer) return this.fail("order_not_owned", "That order does not belong to the verified customer.");
      this.checked.push(args.order_id);
      return { ok: true, content: `total_cents=${total}` };
    }
    if (tool === "process_refund") {
      if (!this.checked.includes(args.order_id)) return this.fail("order_not_checked", "Look up the order before refunding it.");
      if (args.amount_cents > LIMIT) return this.fail("needs_human", "A refund above the limit is decided by a person. Escalate.");
      this.backend.push(tool);
      this.refunds.push(`${args.order_id}:${args.amount_cents}`);
      return { ok: true, content: `refund_id=R${this.refunds.length}` };
    }
    return this.fail("unknown_tool", `No tool named ${tool}.`);
  }
}

/** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
export function run(script: Call[]): [Desk, string] {
  const desk = new Desk();
  for (const [tool, args] of script) {
    const result = desk.call(tool, args);
    if (!result.ok && result.retryable) {
      desk.retries += 1;
      desk.call(tool, args);
    }
  }
  const outcome = desk.escalation ? "escalated" : desk.lastCode === "ambiguous_match" ? "asked" : "resolved";
  return [desk, outcome];
}

const join = (items: string[]) => items.join(",") || "-";
const BEN: Call = ["get_customer", { query: "ben@example.com" }];
export const INCIDENTS: Array<[string, Call[]]> = [
  ["skips identity", [["lookup_order", { order_id: "O1" }], BEN, ["lookup_order", { order_id: "O1" }], ["process_refund", { order_id: "O1", amount_cents: 2000 }]]],
  ["transient fault", [BEN, ["lookup_order", { order_id: "O2" }], ["process_refund", { order_id: "O2", amount_cents: 8000 }]]],
  ["over the limit", [BEN, ["lookup_order", { order_id: "O2" }], ["process_refund", { order_id: "O2", amount_cents: 25000 }],
    ["escalate_to_human", { trigger: "needs_human", reason: "refund of 250.00 asked" }]]],
  ["someone else's order", [["get_customer", { query: "ana@example.com" }], ["lookup_order", { order_id: "O1" }]]],
  ["two customers match", [["get_customer", { query: "Ana Silva" }]]],
  ["asks for a person", [["escalate_to_human", { trigger: "customer_request", reason: "customer asked for a person" }]]],
  ["no progress", [BEN, ...Array.from({ length: 3 }, (): Call => ["lookup_order", { order_id: "O9" }])]],
];

function main() {
  for (const [name, script] of INCIDENTS) {
    const [desk, outcome] = run(script);
    console.log(`${name.padEnd(21)} outcome=${outcome.padEnd(9)} refused=${join(desk.refused)} retries=${desk.retries} refunds=${join(desk.refunds)} backend=${join(desk.backend)}`);
    if (desk.escalation) {
      const e = desk.escalation;
      console.log(`  handoff: trigger=${e.trigger} verified=${e.verified ? "yes" : "no"} customer=${e.customer ?? "-"} orders=${join(e.orders)} refunds=${join(e.refunds)} refused=${join(e.refused)}`);
    }
  }
}

if (import.meta.main) main();
```
```text
skips identity        outcome=resolved  refused=identity_required retries=0 refunds=O1:2000 backend=lookup_order,process_refund
transient fault       outcome=resolved  refused=- retries=1 refunds=O2:8000 backend=lookup_order,lookup_order,process_refund
over the limit        outcome=escalated refused=needs_human retries=1 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=needs_human verified=yes customer=C3 orders=O2 refunds=- refused=needs_human
someone else's order  outcome=resolved  refused=order_not_owned retries=0 refunds=- backend=lookup_order
two customers match   outcome=asked     refused=ambiguous_match retries=0 refunds=- backend=-
asks for a person     outcome=escalated refused=- retries=0 refunds=- backend=-
  handoff: trigger=customer_request verified=no customer=- orders=- refunds=- refused=-
no progress           outcome=escalated refused=- retries=0 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=stalled verified=yes customer=C3 orders=- refunds=- refused=-
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
 *
 * <p>The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
 * runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
 * design, not an Anthropic interface.
 */
public final class SupportDesk {
    static final int LIMIT = 10_000; // cents: a refund above it is a person's decision
    static final int STALL = 3; // the same call this many times in a row is no progress

    static final Map<String, String[]> CUSTOMERS = new LinkedHashMap<>();
    static final Map<String, Object[]> ORDERS = new LinkedHashMap<>();

    static {
        CUSTOMERS.put("C1", new String[] {"Ana Silva", "ana@example.com"});
        CUSTOMERS.put("C2", new String[] {"Ana Silva", "ana.s@example.com"});
        CUSTOMERS.put("C3", new String[] {"Ben Ortiz", "ben@example.com"});
        ORDERS.put("O1", new Object[] {"C3", 5000});
        ORDERS.put("O2", new Object[] {"C3", 30000});
        ORDERS.put("O3", new Object[] {"C1", 4000});
    }

    record Call(String tool, Map<String, Object> args) {}

    record Result(boolean ok, String code, boolean retryable, String message, String content) {}

    record Escalation(String trigger, String reason, String customer, boolean verified, List<String> orders, List<String> refunds, List<String> refused) {}

    /** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
    static final class Desk {
        String customer;
        final List<String> checked = new ArrayList<>(), refunds = new ArrayList<>(), refused = new ArrayList<>(), backend = new ArrayList<>(), recent = new ArrayList<>();
        Escalation escalation;
        String lastCode;
        int retries;
        final java.util.Set<String> flaky = new java.util.HashSet<>(List.of("O2")); // the first lookup of this order fails with a transient fault

        Result fail(String code, String message, boolean retryable) {
            lastCode = code;
            if (!code.equals("transient") && !code.equals("not_found")) refused.add(code);
            return new Result(false, code, retryable, message, null);
        }

        Result fail(String code, String message) {
            return fail(code, message, false);
        }

        /** The record a person reads, built from the desk's own state and not from the model's account. */
        Result escalate(String trigger, String reason) {
            escalation = new Escalation(trigger, reason, customer, customer != null, checked.stream().sorted().toList(), List.copyOf(refunds), List.copyOf(refused));
            return new Result(true, null, false, null, "handed over");
        }

        Result call(String tool, Map<String, Object> args) {
            lastCode = null;
            recent.add(tool + new TreeMap<>(args));
            if (recent.size() > STALL) recent.remove(0);
            if (tool.equals("escalate_to_human")) return escalate((String) args.get("trigger"), (String) args.get("reason")); // the way to a person never waits for a prerequisite
            if (recent.size() == STALL && recent.stream().distinct().count() == 1) return escalate("stalled", tool + " repeated " + STALL + " times without progress");
            if (tool.equals("get_customer")) {
                List<String> found = new ArrayList<>();
                CUSTOMERS.forEach((id, c) -> {
                    if (args.get("query").equals(c[0]) || args.get("query").equals(c[1])) found.add(id);
                });
                if (found.size() > 1) return fail("ambiguous_match", found.size() + " customers match. Ask for the e-mail address. Do not pick one.");
                if (found.isEmpty()) return fail("not_found", "No customer matches. Ask for the e-mail address.");
                customer = found.get(0);
                return new Result(true, null, false, null, "customer_id=" + found.get(0));
            }
            if (customer == null) return fail("identity_required", "Identify the customer with get_customer before this action.");
            String id = (String) args.get("order_id");
            Object[] order = ORDERS.get(id);
            if (tool.equals("lookup_order")) {
                backend.add(tool);
                if (flaky.remove(id)) return fail("transient", "The order service timed out. Retry.", true);
                if (order == null) return fail("not_found", "No such order.");
                if (!order[0].equals(customer)) return fail("order_not_owned", "That order does not belong to the verified customer.");
                checked.add(id);
                return new Result(true, null, false, null, "total_cents=" + order[1]);
            }
            if (tool.equals("process_refund")) {
                if (!checked.contains(id)) return fail("order_not_checked", "Look up the order before refunding it.");
                int amount = (Integer) args.get("amount_cents");
                if (amount > LIMIT) return fail("needs_human", "A refund above the limit is decided by a person. Escalate.");
                backend.add(tool);
                refunds.add(id + ":" + amount);
                return new Result(true, null, false, null, "refund_id=R" + refunds.size());
            }
            return fail("unknown_tool", "No tool named " + tool + ".");
        }
    }

    /** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
    static Map.Entry<Desk, String> run(List<Call> script) {
        Desk desk = new Desk();
        for (Call c : script) {
            Result result = desk.call(c.tool(), c.args());
            if (!result.ok() && result.retryable()) {
                desk.retries++;
                desk.call(c.tool(), c.args());
            }
        }
        String outcome = desk.escalation != null ? "escalated" : "ambiguous_match".equals(desk.lastCode) ? "asked" : "resolved";
        return Map.entry(desk, outcome);
    }

    static String join(List<String> items) {
        return items.isEmpty() ? "-" : String.join(",", items);
    }

    static Call call(String tool, Object... kv) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) args.put((String) kv[i], kv[i + 1]);
        return new Call(tool, args);
    }

    static final Call BEN = call("get_customer", "query", "ben@example.com");

    static final Map<String, List<Call>> INCIDENTS = new LinkedHashMap<>();

    static {
        INCIDENTS.put("skips identity", List.of(call("lookup_order", "order_id", "O1"), BEN, call("lookup_order", "order_id", "O1"), call("process_refund", "order_id", "O1", "amount_cents", 2000)));
        INCIDENTS.put("transient fault", List.of(BEN, call("lookup_order", "order_id", "O2"), call("process_refund", "order_id", "O2", "amount_cents", 8000)));
        INCIDENTS.put("over the limit", List.of(BEN, call("lookup_order", "order_id", "O2"), call("process_refund", "order_id", "O2", "amount_cents", 25000),
            call("escalate_to_human", "trigger", "needs_human", "reason", "refund of 250.00 asked")));
        INCIDENTS.put("someone else's order", List.of(call("get_customer", "query", "ana@example.com"), call("lookup_order", "order_id", "O1")));
        INCIDENTS.put("two customers match", List.of(call("get_customer", "query", "Ana Silva")));
        INCIDENTS.put("asks for a person", List.of(call("escalate_to_human", "trigger", "customer_request", "reason", "customer asked for a person")));
        INCIDENTS.put("no progress", List.of(BEN, call("lookup_order", "order_id", "O9"), call("lookup_order", "order_id", "O9"), call("lookup_order", "order_id", "O9")));
    }

    public static void main(String[] args) {
        INCIDENTS.forEach((name, script) -> {
            var run = run(script);
            Desk desk = run.getKey();
            System.out.println(String.format("%-21s outcome=%-9s refused=%s retries=%d refunds=%s backend=%s", name, run.getValue(), join(desk.refused), desk.retries, join(desk.refunds), join(desk.backend)));
            if (desk.escalation != null) {
                Escalation e = desk.escalation;
                System.out.println("  handoff: trigger=" + e.trigger() + " verified=" + (e.verified() ? "yes" : "no") + " customer=" + (e.customer() == null ? "-" : e.customer())
                    + " orders=" + join(e.orders()) + " refunds=" + join(e.refunds()) + " refused=" + join(e.refused()));
            }
        });
    }
}
```
```text
skips identity        outcome=resolved  refused=identity_required retries=0 refunds=O1:2000 backend=lookup_order,process_refund
transient fault       outcome=resolved  refused=- retries=1 refunds=O2:8000 backend=lookup_order,lookup_order,process_refund
over the limit        outcome=escalated refused=needs_human retries=1 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=needs_human verified=yes customer=C3 orders=O2 refunds=- refused=needs_human
someone else's order  outcome=resolved  refused=order_not_owned retries=0 refunds=- backend=lookup_order
two customers match   outcome=asked     refused=ambiguous_match retries=0 refunds=- backend=-
asks for a person     outcome=escalated refused=- retries=0 refunds=- backend=-
  handoff: trigger=customer_request verified=no customer=- orders=- refunds=- refused=-
no progress           outcome=escalated refused=- retries=0 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=stalled verified=yes customer=C3 orders=- refunds=- refused=-
```
```kotlin
/**
 * A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
 *
 * The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
 * runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
 * design, not an Anthropic interface.
 */
const val LIMIT = 10_000 // cents: a refund above it is a person's decision
const val STALL = 3 // the same call this many times in a row is no progress

val CUSTOMERS = mapOf("C1" to ("Ana Silva" to "ana@example.com"), "C2" to ("Ana Silva" to "ana.s@example.com"), "C3" to ("Ben Ortiz" to "ben@example.com"))
val ORDERS = mapOf("O1" to ("C3" to 5000), "O2" to ("C3" to 30000), "O3" to ("C1" to 4000))

data class Call(val tool: String, val args: Map<String, Any>)

data class Result(val ok: Boolean, val code: String? = null, val retryable: Boolean = false, val message: String? = null, val content: String? = null)

data class Escalation(val trigger: String, val reason: String, val customer: String?, val verified: Boolean, val orders: List<String>, val refunds: List<String>, val refused: List<String>)

/** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
class Desk {
    var customer: String? = null
    val checked = mutableListOf<String>()
    val refunds = mutableListOf<String>()
    val refused = mutableListOf<String>()
    val backend = mutableListOf<String>()
    val recent = mutableListOf<String>()
    var escalation: Escalation? = null
    var lastCode: String? = null
    var retries = 0
    val flaky = mutableSetOf("O2") // the first lookup of this order fails with a transient fault

    fun fail(code: String, message: String, retryable: Boolean = false): Result {
        lastCode = code
        if (code != "transient" && code != "not_found") refused += code
        return Result(false, code, retryable, message)
    }

    /** The record a person reads, built from the desk's own state and not from the model's account. */
    fun escalate(trigger: String, reason: String): Result {
        escalation = Escalation(trigger, reason, customer, customer != null, checked.sorted(), refunds.toList(), refused.toList())
        return Result(true, content = "handed over")
    }

    fun call(tool: String, args: Map<String, Any>): Result {
        lastCode = null
        recent += tool + args.toSortedMap()
        if (recent.size > STALL) recent.removeAt(0)
        if (tool == "escalate_to_human") return escalate(args["trigger"] as String, args["reason"] as String) // the way to a person never waits for a prerequisite
        if (recent.size == STALL && recent.distinct().size == 1) return escalate("stalled", "$tool repeated $STALL times without progress")
        if (tool == "get_customer") {
            val found = CUSTOMERS.filter { (_, c) -> args["query"] == c.first || args["query"] == c.second }.keys.toList()
            if (found.size > 1) return fail("ambiguous_match", "${found.size} customers match. Ask for the e-mail address. Do not pick one.")
            if (found.isEmpty()) return fail("not_found", "No customer matches. Ask for the e-mail address.")
            customer = found[0]
            return Result(true, content = "customer_id=${found[0]}")
        }
        if (customer == null) return fail("identity_required", "Identify the customer with get_customer before this action.")
        val id = args["order_id"] as String? ?: ""
        val order = ORDERS[id]
        if (tool == "lookup_order") {
            backend += tool
            if (flaky.remove(id)) return fail("transient", "The order service timed out. Retry.", true)
            if (order == null) return fail("not_found", "No such order.")
            if (order.first != customer) return fail("order_not_owned", "That order does not belong to the verified customer.")
            checked += id
            return Result(true, content = "total_cents=${order.second}")
        }
        if (tool == "process_refund") {
            if (id !in checked) return fail("order_not_checked", "Look up the order before refunding it.")
            val amount = args["amount_cents"] as Int
            if (amount > LIMIT) return fail("needs_human", "A refund above the limit is decided by a person. Escalate.")
            backend += tool
            refunds += "$id:$amount"
            return Result(true, content = "refund_id=R${refunds.size}")
        }
        return fail("unknown_tool", "No tool named $tool.")
    }
}

/** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
fun run(script: List<Call>): Pair<Desk, String> {
    val desk = Desk()
    for ((tool, args) in script) {
        val result = desk.call(tool, args)
        if (!result.ok && result.retryable) {
            desk.retries += 1
            desk.call(tool, args)
        }
    }
    val outcome = if (desk.escalation != null) "escalated" else if (desk.lastCode == "ambiguous_match") "asked" else "resolved"
    return desk to outcome
}

fun join(items: List<String>) = items.joinToString(",").ifEmpty { "-" }

fun call(tool: String, vararg kv: Pair<String, Any>) = Call(tool, mapOf(*kv))

val BEN = call("get_customer", "query" to "ben@example.com")

val INCIDENTS = listOf(
    "skips identity" to listOf(call("lookup_order", "order_id" to "O1"), BEN, call("lookup_order", "order_id" to "O1"), call("process_refund", "order_id" to "O1", "amount_cents" to 2000)),
    "transient fault" to listOf(BEN, call("lookup_order", "order_id" to "O2"), call("process_refund", "order_id" to "O2", "amount_cents" to 8000)),
    "over the limit" to listOf(BEN, call("lookup_order", "order_id" to "O2"), call("process_refund", "order_id" to "O2", "amount_cents" to 25000),
        call("escalate_to_human", "trigger" to "needs_human", "reason" to "refund of 250.00 asked")),
    "someone else's order" to listOf(call("get_customer", "query" to "ana@example.com"), call("lookup_order", "order_id" to "O1")),
    "two customers match" to listOf(call("get_customer", "query" to "Ana Silva")),
    "asks for a person" to listOf(call("escalate_to_human", "trigger" to "customer_request", "reason" to "customer asked for a person")),
    "no progress" to listOf(BEN, call("lookup_order", "order_id" to "O9"), call("lookup_order", "order_id" to "O9"), call("lookup_order", "order_id" to "O9")),
)

fun main() {
    for ((name, script) in INCIDENTS) {
        val (desk, outcome) = run(script)
        println("${name.padEnd(21)} outcome=${outcome.padEnd(9)} refused=${join(desk.refused)} retries=${desk.retries} refunds=${join(desk.refunds)} backend=${join(desk.backend)}")
        desk.escalation?.let { e ->
            println("  handoff: trigger=${e.trigger} verified=${if (e.verified) "yes" else "no"} customer=${e.customer ?: "-"} orders=${join(e.orders)} refunds=${join(e.refunds)} refused=${join(e.refused)}")
        }
    }
}
```
```text
skips identity        outcome=resolved  refused=identity_required retries=0 refunds=O1:2000 backend=lookup_order,process_refund
transient fault       outcome=resolved  refused=- retries=1 refunds=O2:8000 backend=lookup_order,lookup_order,process_refund
over the limit        outcome=escalated refused=needs_human retries=1 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=needs_human verified=yes customer=C3 orders=O2 refunds=- refused=needs_human
someone else's order  outcome=resolved  refused=order_not_owned retries=0 refunds=- backend=lookup_order
two customers match   outcome=asked     refused=ambiguous_match retries=0 refunds=- backend=-
asks for a person     outcome=escalated refused=- retries=0 refunds=- backend=-
  handoff: trigger=customer_request verified=no customer=- orders=- refunds=- refused=-
no progress           outcome=escalated refused=- retries=0 refunds=- backend=lookup_order,lookup_order
  handoff: trigger=stalled verified=yes customer=C3 orders=- refunds=- refused=-
```
<!-- /example -->

The first incident shows the gate: the refusal is counted, and the model's later calls, in the right order, produce the refund. The transient fault shows one retry done by the loop and the case resolved. The third incident shows the limit: its order lookup meets the same transient fault and is retried once, the refund is refused with `needs_human`, nothing reached the backend for it, and the escalation record names the verified customer, the order looked up, no refund made and the one refusal. The fourth shows an order of another customer refused, and the fifth shows two matches become a question, with no customer set and the outcome `asked`. The sixth shows that a person can be reached with no identity at all, and that the record says so (`verified=no`). The last shows the stall guard turning three identical lookups into a hand-off. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Add a routing layer that enables only the tools suited to the request type."** It is tempting because fewer tools sound safer. The exam rejects it for the skipped-identity problem: it changes which tools exist, and the fault is the order in which they are called. The rule belongs in the code that runs them.
2. **"Add examples that show `get_customer` being called first."** It is tempting because examples are cheap. The exam rejects it: examples make the order more likely and not certain, and money moves when they fail.
3. **"Merge the two lookup tools into one."** It is tempting because the model confuses them. The exam rejects it as a first step: it is a valid design that costs far more than the cause needs, and the cause (a thin description) is fixed by writing a better one.
4. **"Escalate when the customer sounds upset, or when the agent reports low confidence."** It is tempting because both are easy to measure. The exam rejects both: neither tells whether the case needs a person, and a confidence the model reports itself is already wrong on the cases that matter.

## Quiz

1. A month of support-agent sessions shows two problems: in 3 percent a refund was issued before the customer was identified, and in 9 percent the agent escalated cases that policy lets it settle. Which fix is made first?
   - **a**: Train a separate model on past tickets first, so it can decide which cases need a person
   - **b**: Write escalation criteria first, because that problem touches the larger share of sessions
   - **c**: Rewrite the descriptions of the tools first, since both problems start with tool choice
   - **d**: Enforce the call sequence in the dispatcher, because moved money cannot be recalled

2. A customer asks about a purchase that belongs to someone else. How does the support example's dispatcher refuse the call?
   - **a**: As `order_not_owned`, and the message does not name the owner
   - **b**: As `order_not_owned`, and the message names the owner so the customer can be asked
   - **c**: As `not_found`, and the message says that the order does not exist
   - **d**: As `needs_human`, and the message tells the model to escalate

3. Two customers share a name, the lookup tool returns both, and the agent proceeds with the first. What should the dispatcher have done?
   - **a**: Choose the customer with the most recent order, as that is the likelier one
   - **b**: Verify both customers, so that either can be served once the agent knows more
   - **c**: Set nobody as verified, and tell the model to ask for something that tells them apart
   - **d**: Raise the retry count of the lookup, since a second try may return one match

<details>
<summary>Answer key</summary>

1. **d**. Money that moved wrongly cannot be called back, so its cause is fixed first. *b* is ruled out because the size of a count does not set the order: "The first fix goes to what cannot be undone." *c* is ruled out because the skipped step is a step order and not a tool choice: "A step order that is only requested". *a* is ruled out because a trained model comes before words were tried: "machinery for a problem nobody has tried to fix with words".
2. **a**. The refusal is `order_not_owned` and keeps the owner out of it: "An order that belongs to someone else is refused as `order_not_owned`, and the message does not say whose it is." *b* is ruled out by the same sentence: "the message does not say whose it is". *c* is ruled out because `not_found` is the answer of the identity step: "No match returns `not_found` with the same advice." *d* is ruled out because that refusal belongs to the amount: "above it the refusal is `needs_human`, which tells the model that the next step is to escalate".
3. **c**. Several matches are a question for the customer, and nothing is set. *a* is ruled out because the dispatcher does not pick: "Several matches set nothing and return the refusal". *b* is ruled out because the desk holds one verified customer, set only by a single match: "One match sets the verified customer." *d* is ruled out because the refusal is not a fault that a retry can clear: "A retryable error (a timeout of the order service) is retried once by the loop, which is the loop's business, not the model's."

</details>
