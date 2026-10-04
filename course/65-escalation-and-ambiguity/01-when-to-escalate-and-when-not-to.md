# When to escalate, and when not to

**Level:** Architect · **Module 65:** Escalation and ambiguity · **Page 1 of 2**
**Exams:** A5.2; S1

**After this page you can** name the three triggers that justify handing a case to a person and the two signals that do not, honour an explicit request for a person without investigating first, acknowledge frustration and keep working when the problem is within the agent's reach, treat a policy that is silent as a reason to escalate, and write escalation criteria with examples into an agent's instructions.

Checked on 2026-10-04 against the exam guide's task statement 5.2 and the scenario S1 description, and against the Claude prompting best-practices page on examples. Nothing here called a model: the example is a set of six hand-written cases routed by two rules (`examples/65-escalation-rules`), and its numbers are illustrations and not measurements. No Anthropic documentation page defines when a support agent should escalate. The rules on this page are the guide's, and the exam keys them; where this page uses a product fact, it says which.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* appropriate escalation triggers are "customer requests for a human, policy exceptions/gaps (not just complex cases), and inability to make meaningful progress"; the agent escalates immediately when a customer explicitly demands a person, and offers to resolve when the issue is straightforward and the customer is only frustrated, escalating if the customer then repeats the preference; "sentiment-based escalation and self-reported confidence scores" are "unreliable proxies for actual case complexity"; and the remedy for poor escalation calibration is "explicit escalation criteria with few-shot examples" in the system prompt. *What the product offers now (documentation read 2026-10-04):* the prompting guidance calls examples "one of the most reliable ways to steer Claude's output format, tone, and structure", asks for relevant, diverse examples wrapped in `<example>` tags and suggests three to five; nothing in the product decides escalation for you, so the criteria live in your instructions and your code. On the exam, a question about an agent that escalates too often or too rarely has the answer: write criteria and examples; and a question about a mood or a confidence number has the answer: neither is the signal.

## Why it matters

Two failures look opposite and have one cause. A support agent that escalates whenever the customer sounds upset floods the human queue with simple tickets, and the people on that queue learn that escalations are noise. An agent that escalates only when it is unsure resolves, with confidence, the cases it should never have touched: the refund the policy does not allow, the competitor price it was never authorised to match. Both are routed by a proxy, a mood or a feeling, in place of the properties of the case. Scenario S1 asks for the properties.

## The idea

### Three triggers

An agent hands a case to a person when one of these is true:

1. **The customer asks for a person.** An explicit request is honoured at once. The agent does not first try to solve the problem, ask why, or "just finish this step": the customer has told it what they want, and an investigation before the transfer is the agent deciding it knows better.
2. **The policy does not cover the request.** A policy exception or a gap is a trigger, and it is not the same as a complex case. The guide's example is a customer asking for a competitor's price to be matched when the policy only describes adjustments to the company's own prices. The policy is silent, and an agent that applies the nearest rule is inventing policy. Complexity alone is not a trigger: a long case entirely inside the policy is something the agent can finish.
3. **The agent cannot make progress.** A tool that keeps failing, a verification that cannot be completed, two attempts that moved nothing: after a limit the agent stops and hands off with what it tried. A loop that carries on is the same failure that module 45 bounded in the agent loop.

### Two signals that are not triggers

**Sentiment.** A customer who is angry about a late parcel has a problem the agent can often solve; a customer who is perfectly polite may be asking for something that only a person may approve. Mood tells you how to speak and says little about what the case needs. Routing by it sends easy cases to people and keeps hard ones with the agent.

**The model's own confidence.** A confidence number that the model writes about itself carries its blind spots (module 63 made the same point about review). It is not calibrated to whether the case is within policy, and a model is often most sure when it is most wrong. Calibrated confidence is a measured thing (module 68). For routing a support case, use the criteria.

The example routes six cases both ways and shows the difference: a sentiment rule gets five of the six wrong, and the three criteria get all six right. The cases are invented to make the contrast visible, and the result describes those cases only.

### Frustration is not a request

The guide separates two customers. One says "I want to talk to a person": escalate now. The other says "this is the third time, this is unacceptable!" and asks for nothing else: the agent acknowledges the frustration, offers to resolve the issue (a reship, a refund within its limits), and carries on. If the customer then says they want a person, that is a request, and the first rule applies. The agent does not argue the customer out of a request, and does not turn a complaint into one. `decide` in the practice has this shape: `asked_for_person` first, and `acknowledge` set when the mood is not calm.

### Criteria and examples in the instructions

Criteria written in the system prompt, with examples, beat a vague "escalate when appropriate". The criteria are the three triggers in the deployment's own words. The examples are the border cases, each with its decision and its reason, and some of them are cases that look like triggers and are not (an angry customer with a reship request, resolve) next to cases that look easy and are (a polite price-match request, escalate). The example's `escalation_section` builds such a block. The examples carry the judgement that the criteria cannot state.

Keep the decision in code where it can be code. The triggers that are facts (the customer used the words "human agent", the policy lookup returned nothing, the attempt counter reached two) can be checked by the application, which then does not rely on the model's recollection of an instruction.

### The example

<!-- example: m65-escalation-rules tabs: python,typescript,java,kotlin -->
```python
"""Why escalation is decided by criteria, and what to ask when a lookup finds several people.

The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
(illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
"""
# name, sentiment, asked for a person, policy silent, what a careful person would do
CASES = [
    ("price match with another shop", "calm", False, True, "escalate"),
    ("wrong colour, standard exchange", "angry", False, False, "resolve"),
    ("calm request to speak to a person", "calm", True, False, "escalate"),
    ("password reset", "frustrated", False, False, "resolve"),
    ("refund for an item bought elsewhere", "calm", False, True, "escalate"),
    ("angry, wants a person now", "angry", True, False, "escalate"),
]


def by_sentiment(case):
    return "resolve" if case[1] == "calm" else "escalate"


def by_criteria(case):
    return "escalate" if case[2] or case[3] else "resolve"


def errors(rule):
    return [i for i, case in enumerate(CASES, 1) if rule(case) != case[4]]


def pick_most_recent(matches):
    """The heuristic the guide rejects: choose the account with the latest order."""
    return max(matches, key=lambda m: m["last_order"])["id"]


def ask_for_identifier(matches, fields):
    """What the guide asks for: no choice, a request for something that tells the matches apart."""
    return f"I found {len(matches)} accounts for that name. Please give me one of: " + ", ".join(fields) + "."


def escalation_section(criteria, examples):
    """Explicit criteria and examples for the system prompt: when to escalate, and when not to."""
    lines = ["Escalate to a person when:"] + [f"- {c}" for c in criteria] + ["", "Examples:"]
    lines += [f'Customer: "{text}" -> {decision} ({why})' for text, decision, why in examples]
    return "\n".join(lines)


def main():
    for i, case in enumerate(CASES, 1):
        print(f"case {i} ({case[0]}): sentiment rule {by_sentiment(case)}, criteria {by_criteria(case)}, careful person {case[4]}")
    print(f"sentiment rule routed {len(errors(by_sentiment))} of {len(CASES)} wrongly: cases " + ", ".join(map(str, errors(by_sentiment))))
    print(f"criteria routed {len(errors(by_criteria))} of {len(CASES)} wrongly")
    matches = [{"id": "c1", "last_order": 20260901}, {"id": "c2", "last_order": 20260915}]
    print(f"heuristic: the agent acts on {pick_most_recent(matches)} although the customer may be c1")
    print(ask_for_identifier(matches, ["the email on the account", "the postcode"]))
    print(escalation_section(["the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"],
                             [("Can you match the price on another site?", "escalate", "the policy only covers our own prices"), ("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration")]))


if __name__ == "__main__":
    main()
```
```text
case 1 (price match with another shop): sentiment rule resolve, criteria escalate, careful person escalate
case 2 (wrong colour, standard exchange): sentiment rule escalate, criteria resolve, careful person resolve
case 3 (calm request to speak to a person): sentiment rule resolve, criteria escalate, careful person escalate
case 4 (password reset): sentiment rule escalate, criteria resolve, careful person resolve
case 5 (refund for an item bought elsewhere): sentiment rule resolve, criteria escalate, careful person escalate
case 6 (angry, wants a person now): sentiment rule escalate, criteria escalate, careful person escalate
sentiment rule routed 5 of 6 wrongly: cases 1, 2, 3, 4, 5
criteria routed 0 of 6 wrongly
heuristic: the agent acts on c2 although the customer may be c1
I found 2 accounts for that name. Please give me one of: the email on the account, the postcode.
Escalate to a person when:
- the customer asks for a person
- the policy does not cover the request
- two attempts made no progress

Examples:
Customer: "Can you match the price on another site?" -> escalate (the policy only covers our own prices)
Customer: "This is the third time my parcel is late!" -> resolve (a late parcel is within the agent's tools; acknowledge the frustration)
```
```typescript
/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
// name, sentiment, asked for a person, policy silent, what a careful person would do
export type Case = [string, string, boolean, boolean, string];
export const CASES: Case[] = [
  ["price match with another shop", "calm", false, true, "escalate"],
  ["wrong colour, standard exchange", "angry", false, false, "resolve"],
  ["calm request to speak to a person", "calm", true, false, "escalate"],
  ["password reset", "frustrated", false, false, "resolve"],
  ["refund for an item bought elsewhere", "calm", false, true, "escalate"],
  ["angry, wants a person now", "angry", true, false, "escalate"],
];

export const bySentiment = (c: Case): string => (c[1] === "calm" ? "resolve" : "escalate");
export const byCriteria = (c: Case): string => (c[2] || c[3] ? "escalate" : "resolve");

export function errors(rule: (c: Case) => string): number[] {
  return CASES.flatMap((c, i) => (rule(c) !== c[4] ? [i + 1] : []));
}

/** The heuristic the guide rejects: choose the account with the latest order. */
export function pickMostRecent(matches: Array<{ id: string; last_order: number }>): string {
  return matches.reduce((best, m) => (m.last_order > best.last_order ? m : best)).id;
}

/** What the guide asks for: no choice, a request for something that tells the matches apart. */
export function askForIdentifier(matches: unknown[], fields: string[]): string {
  return `I found ${matches.length} accounts for that name. Please give me one of: ` + fields.join(", ") + ".";
}

/** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
export function escalationSection(criteria: string[], examples: Array<[string, string, string]>): string {
  const lines = ["Escalate to a person when:", ...criteria.map((c) => `- ${c}`), "", "Examples:"];
  lines.push(...examples.map(([text, decision, why]) => `Customer: "${text}" -> ${decision} (${why})`));
  return lines.join("\n");
}

function main() {
  CASES.forEach((c, i) => console.log(`case ${i + 1} (${c[0]}): sentiment rule ${bySentiment(c)}, criteria ${byCriteria(c)}, careful person ${c[4]}`));
  console.log(`sentiment rule routed ${errors(bySentiment).length} of ${CASES.length} wrongly: cases ` + errors(bySentiment).join(", "));
  console.log(`criteria routed ${errors(byCriteria).length} of ${CASES.length} wrongly`);
  const matches = [{ id: "c1", last_order: 20260901 }, { id: "c2", last_order: 20260915 }];
  console.log(`heuristic: the agent acts on ${pickMostRecent(matches)} although the customer may be c1`);
  console.log(askForIdentifier(matches, ["the email on the account", "the postcode"]));
  console.log(escalationSection(["the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"],
    [["Can you match the price on another site?", "escalate", "the policy only covers our own prices"], ["This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"]]));
}

if (import.meta.main) main();
```
```text
case 1 (price match with another shop): sentiment rule resolve, criteria escalate, careful person escalate
case 2 (wrong colour, standard exchange): sentiment rule escalate, criteria resolve, careful person resolve
case 3 (calm request to speak to a person): sentiment rule resolve, criteria escalate, careful person escalate
case 4 (password reset): sentiment rule escalate, criteria resolve, careful person resolve
case 5 (refund for an item bought elsewhere): sentiment rule resolve, criteria escalate, careful person escalate
case 6 (angry, wants a person now): sentiment rule escalate, criteria escalate, careful person escalate
sentiment rule routed 5 of 6 wrongly: cases 1, 2, 3, 4, 5
criteria routed 0 of 6 wrongly
heuristic: the agent acts on c2 although the customer may be c1
I found 2 accounts for that name. Please give me one of: the email on the account, the postcode.
Escalate to a person when:
- the customer asks for a person
- the policy does not cover the request
- two attempts made no progress

Examples:
Customer: "Can you match the price on another site?" -> escalate (the policy only covers our own prices)
Customer: "This is the third time my parcel is late!" -> resolve (a late parcel is within the agent's tools; acknowledge the frustration)
```
```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * <p>The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
public final class EscalationRules {
    /** name, sentiment, asked for a person, policy silent, what a careful person would do */
    record Case(String name, String sentiment, boolean asked, boolean policySilent, String truth) {}

    record Account(String id, int lastOrder) {}

    record Example(String text, String decision, String why) {}

    static final List<Case> CASES = List.of(
        new Case("price match with another shop", "calm", false, true, "escalate"),
        new Case("wrong colour, standard exchange", "angry", false, false, "resolve"),
        new Case("calm request to speak to a person", "calm", true, false, "escalate"),
        new Case("password reset", "frustrated", false, false, "resolve"),
        new Case("refund for an item bought elsewhere", "calm", false, true, "escalate"),
        new Case("angry, wants a person now", "angry", true, false, "escalate"));

    static String bySentiment(Case c) {
        return c.sentiment().equals("calm") ? "resolve" : "escalate";
    }

    static String byCriteria(Case c) {
        return c.asked() || c.policySilent() ? "escalate" : "resolve";
    }

    static List<Integer> errors(Function<Case, String> rule) {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < CASES.size(); i++) if (!rule.apply(CASES.get(i)).equals(CASES.get(i).truth())) wrong.add(i + 1);
        return wrong;
    }

    /** The heuristic the guide rejects: choose the account with the latest order. */
    static String pickMostRecent(List<Account> matches) {
        return matches.stream().max(Comparator.comparingInt(Account::lastOrder)).orElseThrow().id();
    }

    /** What the guide asks for: no choice, a request for something that tells the matches apart. */
    static String askForIdentifier(int matches, List<String> fields) {
        return "I found " + matches + " accounts for that name. Please give me one of: " + String.join(", ", fields) + ".";
    }

    /** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
    static String escalationSection(List<String> criteria, List<Example> examples) {
        List<String> lines = new ArrayList<>(List.of("Escalate to a person when:"));
        for (String c : criteria) lines.add("- " + c);
        lines.add("");
        lines.add("Examples:");
        for (Example e : examples) lines.add("Customer: \"" + e.text() + "\" -> " + e.decision() + " (" + e.why() + ")");
        return String.join("\n", lines);
    }

    private static String join(List<Integer> numbers) {
        List<String> parts = new ArrayList<>();
        for (int n : numbers) parts.add(String.valueOf(n));
        return String.join(", ", parts);
    }

    public static void main(String[] args) {
        for (int i = 0; i < CASES.size(); i++) {
            Case c = CASES.get(i);
            System.out.println("case " + (i + 1) + " (" + c.name() + "): sentiment rule " + bySentiment(c) + ", criteria " + byCriteria(c) + ", careful person " + c.truth());
        }
        System.out.println("sentiment rule routed " + errors(EscalationRules::bySentiment).size() + " of " + CASES.size() + " wrongly: cases " + join(errors(EscalationRules::bySentiment)));
        System.out.println("criteria routed " + errors(EscalationRules::byCriteria).size() + " of " + CASES.size() + " wrongly");
        List<Account> matches = List.of(new Account("c1", 20260901), new Account("c2", 20260915));
        System.out.println("heuristic: the agent acts on " + pickMostRecent(matches) + " although the customer may be c1");
        System.out.println(askForIdentifier(matches.size(), List.of("the email on the account", "the postcode")));
        System.out.println(escalationSection(List.of("the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"),
            List.of(new Example("Can you match the price on another site?", "escalate", "the policy only covers our own prices"),
                new Example("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"))));
    }
}
```
```text
case 1 (price match with another shop): sentiment rule resolve, criteria escalate, careful person escalate
case 2 (wrong colour, standard exchange): sentiment rule escalate, criteria resolve, careful person resolve
case 3 (calm request to speak to a person): sentiment rule resolve, criteria escalate, careful person escalate
case 4 (password reset): sentiment rule escalate, criteria resolve, careful person resolve
case 5 (refund for an item bought elsewhere): sentiment rule resolve, criteria escalate, careful person escalate
case 6 (angry, wants a person now): sentiment rule escalate, criteria escalate, careful person escalate
sentiment rule routed 5 of 6 wrongly: cases 1, 2, 3, 4, 5
criteria routed 0 of 6 wrongly
heuristic: the agent acts on c2 although the customer may be c1
I found 2 accounts for that name. Please give me one of: the email on the account, the postcode.
Escalate to a person when:
- the customer asks for a person
- the policy does not cover the request
- two attempts made no progress

Examples:
Customer: "Can you match the price on another site?" -> escalate (the policy only covers our own prices)
Customer: "This is the third time my parcel is late!" -> resolve (a late parcel is within the agent's tools; acknowledge the frustration)
```
```kotlin
/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
/** name, sentiment, asked for a person, policy silent, what a careful person would do */
data class Case(val name: String, val sentiment: String, val asked: Boolean, val policySilent: Boolean, val truth: String)

data class Account(val id: String, val lastOrder: Int)

data class Example(val text: String, val decision: String, val why: String)

val CASES = listOf(
    Case("price match with another shop", "calm", false, true, "escalate"),
    Case("wrong colour, standard exchange", "angry", false, false, "resolve"),
    Case("calm request to speak to a person", "calm", true, false, "escalate"),
    Case("password reset", "frustrated", false, false, "resolve"),
    Case("refund for an item bought elsewhere", "calm", false, true, "escalate"),
    Case("angry, wants a person now", "angry", true, false, "escalate"),
)

fun bySentiment(c: Case): String = if (c.sentiment == "calm") "resolve" else "escalate"

fun byCriteria(c: Case): String = if (c.asked || c.policySilent) "escalate" else "resolve"

fun errors(rule: (Case) -> String): List<Int> = CASES.withIndex().filter { rule(it.value) != it.value.truth }.map { it.index + 1 }

/** The heuristic the guide rejects: choose the account with the latest order. */
fun pickMostRecent(matches: List<Account>): String = matches.maxByOrNull { it.lastOrder }!!.id

/** What the guide asks for: no choice, a request for something that tells the matches apart. */
fun askForIdentifier(matches: Int, fields: List<String>): String = "I found $matches accounts for that name. Please give me one of: " + fields.joinToString(", ") + "."

/** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
fun escalationSection(criteria: List<String>, examples: List<Example>): String {
    val lines = mutableListOf("Escalate to a person when:") + criteria.map { "- $it" } + listOf("", "Examples:") + examples.map { "Customer: \"${it.text}\" -> ${it.decision} (${it.why})" }
    return lines.joinToString("\n")
}

fun main() {
    CASES.forEachIndexed { i, c -> println("case ${i + 1} (${c.name}): sentiment rule ${bySentiment(c)}, criteria ${byCriteria(c)}, careful person ${c.truth}") }
    println("sentiment rule routed ${errors(::bySentiment).size} of ${CASES.size} wrongly: cases " + errors(::bySentiment).joinToString(", "))
    println("criteria routed ${errors(::byCriteria).size} of ${CASES.size} wrongly")
    val matches = listOf(Account("c1", 20260901), Account("c2", 20260915))
    println("heuristic: the agent acts on ${pickMostRecent(matches)} although the customer may be c1")
    println(askForIdentifier(matches.size, listOf("the email on the account", "the postcode")))
    println(escalationSection(listOf("the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"),
        listOf(Example("Can you match the price on another site?", "escalate", "the policy only covers our own prices"), Example("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"))))
}
```
```text
case 1 (price match with another shop): sentiment rule resolve, criteria escalate, careful person escalate
case 2 (wrong colour, standard exchange): sentiment rule escalate, criteria resolve, careful person resolve
case 3 (calm request to speak to a person): sentiment rule resolve, criteria escalate, careful person escalate
case 4 (password reset): sentiment rule escalate, criteria resolve, careful person resolve
case 5 (refund for an item bought elsewhere): sentiment rule resolve, criteria escalate, careful person escalate
case 6 (angry, wants a person now): sentiment rule escalate, criteria escalate, careful person escalate
sentiment rule routed 5 of 6 wrongly: cases 1, 2, 3, 4, 5
criteria routed 0 of 6 wrongly
heuristic: the agent acts on c2 although the customer may be c1
I found 2 accounts for that name. Please give me one of: the email on the account, the postcode.
Escalate to a person when:
- the customer asks for a person
- the policy does not cover the request
- two attempts made no progress

Examples:
Customer: "Can you match the price on another site?" -> escalate (the policy only covers our own prices)
Customer: "This is the third time my parcel is late!" -> resolve (a late parcel is within the agent's tools; acknowledge the frustration)
```
<!-- /example -->

## Traps

1. **"Escalate when the customer sounds angry; they will want a person."** It is tempting because anger feels urgent. The exam rejects it: the agent acknowledges the frustration and offers to fix what is within its reach, and escalates if the customer asks for a person.
2. **"Escalate when the model's confidence is low, and trust it when it is high."** It is tempting because the number looks like a measurement. The exam rejects it: a self-reported score is not a reliable proxy for the case's difficulty, and the criteria decide.
3. **"The customer asked for a person, but the fix is one click: do it first."** It is tempting because it saves the person's time. The exam rejects it: an explicit request is honoured at once, without an investigation first.
4. **"The policy does not mention competitor prices; apply the closest rule."** It is tempting because the closest rule sounds fair. The exam rejects it: a silent policy is a gap, and the agent escalates and does not invent policy.

## Quiz

1. Scenario S1, customer support resolution agent. A customer writes that they want to speak to a person about a change of delivery address. The agent has a tool that can make the change in one step. What should the agent do?
   - **a**: Hand over to a human straight away, before touching the account
   - **b**: Make the change first, since it takes one step, then offer a transfer
   - **c**: Ask why a human is wanted and transfer only if the reason is unclear
   - **d**: Offer to complete the update and transfer only if the customer repeats the wish

2. Scenario S1, customer support resolution agent. A customer asks the agent to match a competitor's lower price. The policy describes adjustments to the company's own prices and says nothing about competitors. The agent can issue a price adjustment. What should it do?
   - **a**: Apply the nearest own-price rule, as that is the fairest reading
   - **b**: Decline politely, since the policy does not allow the match
   - **c**: Route it to a colleague authorised to rule on the exception
   - **d**: Issue the adjustment and flag it for review afterwards

<details>
<summary>Answer key</summary>

1. **a**. An explicit request for a person is honoured at once, with no investigation first. *b* is ruled out because "an investigation before the transfer is the agent deciding it knows better". *c* is ruled out because the agent "does not first try to solve the problem, ask why", and a question is a delay. *d* is ruled out because "An explicit request is honoured at once"; offering a fix first is the answer for frustration, not for a request.
2. **c**. A silent policy is a gap, and a gap goes to a person who can decide. *a* is ruled out because "an agent that applies the nearest rule is inventing policy". *b* is ruled out because "A policy exception or a gap is a trigger", and declining is also a decision the policy did not make. *d* is ruled out because "the agent escalates and does not invent policy", and acting first leaves the invention in place.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
