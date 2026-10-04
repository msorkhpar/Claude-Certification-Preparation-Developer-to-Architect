# Workflow or agent: the distinction and the first three patterns

**Level:** Developer · **Module 34:** Workflows and agents · **Page 1 of 2**
**Exams:** DV3; A1.6

**After this page you can** tell a workflow from an agent by who controls the path, say when the simplest design is enough, and describe and write three workflow patterns: prompt chaining, routing and parallelization (sectioning and voting).

Checked against Anthropic's engineering article "Building effective agents", published on 2024-12-19, read on 2026-10-03. The article carries a note at its top: "Much of the tooling landscape described in this post has changed since December 2024." The patterns are design patterns and not product features, so they have aged better than its tooling advice, and this course uses only the patterns. The example ran offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0) against a scripted stand-in whose replies are hand-written and labelled illustrative. The article's own examples name Claude Haiku 4.5 and Claude Sonnet 4.5; the example here uses `claude-haiku-4-5` and `claude-sonnet-5-5`.

## Why it matters

"Agent" is used for everything from a three-step script to an autonomous system that runs for hours, and the choice between them decides cost, latency, testability and risk. The exam asks for the distinction, and for the pattern that fits a described task. In practice most systems that are called agents are workflows, and the patterns on these two pages are small enough to write by hand.

## The idea

### Workflows and agents

"At Anthropic, we categorize all these variations as agentic systems", and the article draws one architectural line inside the family.

- "Workflows are systems where LLMs and tools are orchestrated through predefined code paths."
- "Agents, on the other hand, are systems where LLMs dynamically direct their own processes and tool usage, maintaining control over how they accomplish tasks."

The test is who decides the next step. In a workflow the program does: it may call a model many times, with tools, but the sequence is written in code. In an agent the model does, in a loop. The article puts it plainly: agents "are typically just LLMs using tools based on environmental feedback in a loop." A system that calls a model in each of three fixed steps is a workflow, however much it calls itself an agent.

### Start with the simplest thing

"When building applications with LLMs, we recommend finding the simplest solution possible, and only increasing complexity when needed." The reason is a trade: "Agentic systems often trade latency and cost for better task performance, and you should consider when this tradeoff makes sense." Workflows give "predictability and consistency for well-defined tasks", and agents are "the better option when flexibility and model-driven decision-making are needed at scale". The article reserves agents for "open-ended problems where it's difficult or impossible to predict the required number of steps". For many applications, "optimizing single LLM calls with retrieval and in-context examples is usually enough". The same advice covers frameworks. They simplify calling a model, defining tools and chaining calls, but "they often create extra layers of abstraction that can obscure the underlying prompts and responses, making them harder to debug". The article's advice: "We suggest that developers start by using LLM APIs directly", and if you use a framework, understand the code under it. Its warning is "Incorrect assumptions about what's under the hood are a common source of customer error."

### The building block

Every pattern below is built from one part, the **augmented LLM**: a model "enhanced with augmentations such as retrieval, tools, and memory". The article asks for two things from it, tailoring the augmentations to the use case and giving the model an easy, well-documented interface. Module 32 and module 33 showed one way to supply that interface. For the patterns, assume each call has these augmentations.

### Prompt chaining

A task is split into a sequence of steps, "where each LLM call processes the output of the previous one". Between steps a program can run a check, a gate, to confirm the work is on track. Use it when the task decomposes cleanly into fixed subtasks, to trade latency for accuracy by making each call easier. Examples: write marketing copy and then translate it, or write an outline, check it against criteria and then write the document.

### Routing

"Routing classifies an input and directs it to a specialized followup task." It separates concerns, so a prompt can be tuned for one kind of input without hurting the others. It works "where there are distinct categories that are better handled separately, and where classification can be handled accurately", by a model or by a traditional classifier. Examples: sending customer questions to different prompts and tools, or sending easy questions to a small, cheap model and hard ones to a more capable one.

The failure to design for is a label that matches no route. The model's reply is text, so it may carry a capital letter, a full stop or a word nobody planned. The program normalizes the label, looks it up, and sends anything unknown to a default route.

### Parallelization: sectioning and voting

When a program can run model calls at the same time and combine them in code, it has two variations.

- **Sectioning**: "Breaking a task into independent subtasks run in parallel." An example is a guardrail, where one call answers and another screens the question. This "tends to perform better than having the same LLM call handle both guardrails and the core response."
- **Voting**: "Running the same task multiple times to get diverse outputs." An example is a code review where several prompts flag a vulnerability, and a threshold balances false positives against false negatives.

The code does the combining. A sectioned answer is kept or dropped by a rule that the program applies to the screen, and a vote is a count and a threshold.

### The example: three patterns in one file

The example below implements a router, a sectioned guard and a vote around a scripted stand-in for the Messages API. The router asks a cheap model to classify a question, maps the label to a model and a prompt, and answers; a label that is not a route takes the default. The guard runs the answer and the screen together and keeps the answer only when the screen says `ok`. The vote asks the same question three times, in parallel, and flags the snippet when the votes for `VULNERABLE` reach a threshold.

<!-- example: m34-routing-and-voting tabs: python,typescript,java,kotlin -->
```python
"""Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.

The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
"""
import asyncio

from harness import scripted_async_client
from harness.scripted import message, text

CHEAP, STRONG = "claude-haiku-4-5", "claude-sonnet-5-5"
ROUTES = {"billing": (STRONG, "You are a billing specialist. Be exact about amounts."), "technical": (STRONG, "You are a support engineer. Ask for logs."),
          "general": (CHEAP, "You are a friendly front desk. Answer briefly.")}


def user_text(body):
    return body["messages"][-1]["content"]


def stand_in(rules):
    """A reply function: the first rule whose key appears in the system prompt or the question decides the answer."""
    def reply(body):
        haystack = f"{body.get('system', '')}\n{user_text(body)}"
        for key, answer in rules:
            if key in haystack:
                return message([text(answer)], model=body["model"])
        raise AssertionError(f"no scripted answer for {haystack!r}")
    return reply


async def ask(client, model, system, prompt):
    reply = await client.messages.create(model=model, max_tokens=300, system=system, messages=[{"role": "user", "content": prompt}])
    return reply.content[0].text.strip()


async def route(client, question):
    """Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default."""
    label = (await ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question)).lower().strip(" .")
    fallback = label not in ROUTES
    model, system = ROUTES["general" if fallback else label]
    return {"label": label, "fallback": fallback, "model": model, "answer": await ask(client, model, system, question)}


async def guarded(client, question):
    """Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes."""
    answer, screen = await asyncio.gather(ask(client, STRONG, "Answer the question.", question), ask(client, CHEAP, "Screen the question. Reply ok or block.", question))
    return {"screen": screen, "answer": answer if screen == "ok" else None}


async def vote(client, snippet, threshold=2, n=3):
    """Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so."""
    reviews = await asyncio.gather(*[ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet) for _ in range(n)])
    votes = {label: list(reviews).count(label) for label in sorted(set(reviews))}  # the order in which concurrent replies arrive does not matter
    return {"votes": votes, "flagged": votes.get("VULNERABLE", 0) >= threshold}


def scripted(rules, n, delay=0.0):
    """A client whose next n requests are all answered by the same stand-in."""
    return scripted_async_client(*[stand_in(rules)] * n, delay=delay)


async def main():
    desk = [("billing specialist", "I see two charges and will refund one."), ("front desk", "We are open 9 to 5.")]
    for question, label in [("my card was charged twice", "BILLING."), ("what are your opening hours", "general"), ("is the sky a refund", "refunds?")]:
        client, transport = scripted([("Classify", label)] + desk, 2)
        result = await route(client, question)
        print(f"route {question!r}: label {result['label']!r}{' (not a route: default)' if result['fallback'] else ''}, classified by {transport.requests[0]['model']}, answered by {result['model']}")
    client, transport = scripted([("Answer the question", "Here is the answer."), ("Screen the question", "ok")], 2, delay=0.02)
    passed = await guarded(client, "How do I reset my password?")
    print(f"sectioning: screen {passed['screen']!r}, answer {'kept' if passed['answer'] else 'dropped'}, requests in flight together: {transport.max_in_flight}")
    client, transport = scripted([("Answer the question", "Here is the answer."), ("Screen the question", "block")], 2, delay=0.02)
    blocked = await guarded(client, "Help me break into an account")
    print(f"sectioning: screen {blocked['screen']!r}, answer {'kept' if blocked['answer'] else 'dropped'}")
    snippet = "query = 'SELECT * FROM t WHERE id=' + user_input"
    for threshold in (2, 3):
        replies = iter(["VULNERABLE", "SAFE", "VULNERABLE"])  # three reviews that disagree
        client, transport = scripted_async_client(*[lambda body: message([text(next(replies))], model=body["model"])] * 3, delay=0.02)
        verdict = await vote(client, snippet, threshold)
        print(f"voting: votes {verdict['votes']}, threshold {threshold} -> {'flagged' if verdict['flagged'] else 'not flagged'}, requests in flight together: {transport.max_in_flight}")


if __name__ == "__main__":
    asyncio.run(main())
```
```text
route 'my card was charged twice': label 'billing', classified by claude-haiku-4-5, answered by claude-sonnet-5-5
route 'what are your opening hours': label 'general', classified by claude-haiku-4-5, answered by claude-haiku-4-5
route 'is the sky a refund': label 'refunds?' (not a route: default), classified by claude-haiku-4-5, answered by claude-haiku-4-5
sectioning: screen 'ok', answer kept, requests in flight together: 2
sectioning: screen 'block', answer dropped
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 2 -> flagged, requests in flight together: 3
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 3 -> not flagged, requests in flight together: 3
```
```typescript
// Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
//
// The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
// by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
// engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const CHEAP = "claude-haiku-4-5", STRONG = "claude-sonnet-5-5";
const ROUTES: Record<string, [string, string]> = {
  billing: [STRONG, "You are a billing specialist. Be exact about amounts."], technical: [STRONG, "You are a support engineer. Ask for logs."],
  general: [CHEAP, "You are a friendly front desk. Answer briefly."],
};

/** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
export function standIn(rules: Array<[string, string]>) {
  return (body: any) => {
    const haystack = `${body.system ?? ""}\n${body.messages.at(-1).content}`;
    for (const [key, answer] of rules) if (haystack.includes(key)) return { body: message([text(answer)], "end_turn", undefined, body.model) };
    throw new Error(`no scripted answer for ${JSON.stringify(haystack)}`);
  };
}

export function scripted(rules: Array<[string, string]>, n: number, delayMs = 0) {
  const fake = scriptedFetch(Array.from({ length: n }, () => standIn(rules)), { delayMs });
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function ask(client: Anthropic, model: string, system: string, prompt: string): Promise<string> {
  const reply = await client.messages.create({ model, max_tokens: 300, system, messages: [{ role: "user", content: prompt }] });
  return (reply.content[0] as any).text.trim();
}

/** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
export async function route(client: Anthropic, question: string) {
  const label = (await ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question)).toLowerCase().replace(/^[ .]+|[ .]+$/g, "");
  const fallback = !(label in ROUTES);
  const [model, system] = ROUTES[fallback ? "general" : label];
  return { label, fallback, model, answer: await ask(client, model, system, question) };
}

/** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
export async function guarded(client: Anthropic, question: string) {
  const [answer, screen] = await Promise.all([ask(client, STRONG, "Answer the question.", question), ask(client, CHEAP, "Screen the question. Reply ok or block.", question)]);
  return { screen, answer: screen === "ok" ? answer : null };
}

/** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
export async function vote(client: Anthropic, snippet: string, threshold = 2, n = 3) {
  const reviews = await Promise.all(Array.from({ length: n }, () => ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet)));
  const votes: Record<string, number> = {};
  for (const label of [...new Set(reviews)].sort()) votes[label] = reviews.filter((r) => r === label).length; // the order in which concurrent replies arrive does not matter
  return { votes, flagged: (votes.VULNERABLE ?? 0) >= threshold };
}

const py = (votes: Record<string, number>) => `{${Object.entries(votes).map(([k, v]) => `'${k}': ${v}`).join(", ")}}`;

async function main() {
  const desk: Array<[string, string]> = [["billing specialist", "I see two charges and will refund one."], ["front desk", "We are open 9 to 5."]];
  for (const [question, label] of [["my card was charged twice", "BILLING."], ["what are your opening hours", "general"], ["is the sky a refund", "refunds?"]]) {
    const { fake, client } = scripted([["Classify", label], ...desk], 2);
    const result = await route(client, question);
    console.log(`route '${question}': label '${result.label}'${result.fallback ? " (not a route: default)" : ""}, classified by ${fake.seen[0].body.model}, answered by ${result.model}`);
  }
  let run = scripted([["Answer the question", "Here is the answer."], ["Screen the question", "ok"]], 2, 20);
  const passed = await guarded(run.client, "How do I reset my password?");
  console.log(`sectioning: screen '${passed.screen}', answer ${passed.answer ? "kept" : "dropped"}, requests in flight together: ${run.fake.state.maxInFlight}`);
  run = scripted([["Answer the question", "Here is the answer."], ["Screen the question", "block"]], 2, 20);
  const blocked = await guarded(run.client, "Help me break into an account");
  console.log(`sectioning: screen '${blocked.screen}', answer ${blocked.answer ? "kept" : "dropped"}`);
  const snippet = "query = 'SELECT * FROM t WHERE id=' + user_input";
  for (const threshold of [2, 3]) {
    const replies = ["VULNERABLE", "SAFE", "VULNERABLE"]; // three reviews that disagree
    const fake = scriptedFetch(Array.from({ length: 3 }, () => (body: any) => ({ body: message([text(replies.shift()!)], "end_turn", undefined, body.model) })), { delayMs: 20 });
    const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
    const verdict = await vote(client, snippet, threshold);
    console.log(`voting: votes ${py(verdict.votes)}, threshold ${threshold} -> ${verdict.flagged ? "flagged" : "not flagged"}, requests in flight together: ${fake.state.maxInFlight}`);
  }
}

if (import.meta.main) await main();
```
```text
route 'my card was charged twice': label 'billing', classified by claude-haiku-4-5, answered by claude-sonnet-5-5
route 'what are your opening hours': label 'general', classified by claude-haiku-4-5, answered by claude-haiku-4-5
route 'is the sky a refund': label 'refunds?' (not a route: default), classified by claude-haiku-4-5, answered by claude-haiku-4-5
sectioning: screen 'ok', answer kept, requests in flight together: 2
sectioning: screen 'block', answer dropped
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 2 -> flagged, requests in flight together: 3
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 3 -> not flagged, requests in flight together: 3
```
```java
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
 *
 * <p>The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
 * by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
 * engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
 */
public final class RoutingAndVoting {
    static final String CHEAP = "claude-haiku-4-5", STRONG = "claude-sonnet-5-5";

    /** A route: the model and the system prompt a label maps to. */
    record Route(String model, String system) {}

    static final Map<String, Route> ROUTES = Map.of(
        "billing", new Route(STRONG, "You are a billing specialist. Be exact about amounts."),
        "technical", new Route(STRONG, "You are a support engineer. Ask for logs."),
        "general", new Route(CHEAP, "You are a friendly front desk. Answer briefly."));

    /** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
    static Function<JsonNode, Object> standIn(List<String[]> rules) {
        return body -> {
            String haystack = body.path("system").asText("") + "\n" + body.at("/messages").get(body.get("messages").size() - 1).get("content").asText();
            for (String[] rule : rules) {
                if (haystack.contains(rule[0])) return message(List.of(text(rule[1])), "end_turn", body.get("model").asText(), Scripted.map("input_tokens", 1, "output_tokens", 1), null);
            }
            throw new AssertionError("no scripted answer for '" + haystack + "'");
        };
    }

    static CompletableFuture<String> ask(AnthropicClientAsync client, String model, String system, String prompt) {
        return client.messages().create(MessageCreateParams.builder().model(Model.of(model)).maxTokens(300).system(system).addUserMessage(prompt).build())
            .thenApply(reply -> reply.content().get(0).asText().text().strip());
    }

    private static String stripSpacesAndDots(String s) {
        int a = 0, b = s.length();
        while (a < b && (s.charAt(a) == ' ' || s.charAt(a) == '.')) a++;
        while (b > a && (s.charAt(b - 1) == ' ' || s.charAt(b - 1) == '.')) b--;
        return s.substring(a, b);
    }

    /** What routing decided and answered. */
    record Routed(String label, boolean fallback, String model, String answer) {}

    /** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
    static CompletableFuture<Routed> route(AnthropicClientAsync client, String question) {
        return ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question).thenCompose(raw -> {
            String label = stripSpacesAndDots(raw.toLowerCase());
            boolean fallback = !ROUTES.containsKey(label);
            Route route = ROUTES.get(fallback ? "general" : label);
            return ask(client, route.model(), route.system(), question).thenApply(answer -> new Routed(label, fallback, route.model(), answer));
        });
    }

    /** What sectioning produced: the screen's word and the answer when it was kept. */
    record Guarded(String screen, String answer) {}

    /** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
    static CompletableFuture<Guarded> guarded(AnthropicClientAsync client, String question) {
        CompletableFuture<String> answer = ask(client, STRONG, "Answer the question.", question);
        CompletableFuture<String> screen = ask(client, CHEAP, "Screen the question. Reply ok or block.", question);
        return answer.thenCombine(screen, (a, s) -> new Guarded(s, s.equals("ok") ? a : null));
    }

    /** What voting counted. */
    record Verdict(Map<String, Long> votes, boolean flagged) {}

    /** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
    static CompletableFuture<Verdict> vote(AnthropicClientAsync client, String snippet, int threshold, int n) {
        List<CompletableFuture<String>> reviews = new ArrayList<>();
        for (int i = 0; i < n; i++) reviews.add(ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet));
        return CompletableFuture.allOf(reviews.toArray(new CompletableFuture[0])).thenApply(done -> {
            Map<String, Long> votes = new TreeMap<>(); // the order in which concurrent replies arrive does not matter
            reviews.forEach(r -> votes.merge(r.join(), 1L, Long::sum));
            return new Verdict(votes, votes.getOrDefault("VULNERABLE", 0L) >= threshold);
        });
    }

    /** A client whose next n requests are all answered by the same stand-in. */
    static Scripted.AsyncRig scripted(List<String[]> rules, int n, Duration delay) {
        Object[] script = new Object[n];
        java.util.Arrays.fill(script, standIn(rules));
        return Scripted.asyncClient(delay, 0, script);
    }

    static List<String[]> rules(String... kv) {
        List<String[]> rules = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) rules.add(new String[] {kv[i], kv[i + 1]});
        return rules;
    }

    /** Three reviews that disagree, handed out in whatever order the requests arrive. */
    static Scripted.AsyncRig disagreeing(Duration delay) {
        ConcurrentLinkedQueue<String> replies = new ConcurrentLinkedQueue<>(List.of("VULNERABLE", "SAFE", "VULNERABLE"));
        Function<JsonNode, Object> reply = body -> message(List.of(text(replies.poll())), "end_turn", body.get("model").asText(), Scripted.map("input_tokens", 1, "output_tokens", 1), null);
        return Scripted.asyncClient(delay, 0, reply, reply, reply);
    }

    public static void main(String[] args) {
        List<String[]> desk = rules("billing specialist", "I see two charges and will refund one.", "front desk", "We are open 9 to 5.");
        String[][] questions = {{"my card was charged twice", "BILLING."}, {"what are your opening hours", "general"}, {"is the sky a refund", "refunds?"}};
        for (String[] q : questions) {
            List<String[]> all = new ArrayList<>(java.util.Arrays.<String[]>asList(new String[] {"Classify", q[1]}));
            all.addAll(desk);
            Scripted.AsyncRig rig = scripted(all, 2, Duration.ZERO);
            Routed result = route(rig.client(), q[0]).join();
            System.out.println("route " + py(q[0]) + ": label " + py(result.label()) + (result.fallback() ? " (not a route: default)" : "") + ", classified by "
                + rig.http().requests.get(0).get("model").asText() + ", answered by " + result.model());
        }
        Scripted.AsyncRig passRig = scripted(rules("Answer the question", "Here is the answer.", "Screen the question", "ok"), 2, Duration.ofMillis(20));
        Guarded passed = guarded(passRig.client(), "How do I reset my password?").join();
        System.out.println("sectioning: screen " + py(passed.screen()) + ", answer " + (passed.answer() != null ? "kept" : "dropped") + ", requests in flight together: " + passRig.http().maxInFlight());
        Scripted.AsyncRig blockRig = scripted(rules("Answer the question", "Here is the answer.", "Screen the question", "block"), 2, Duration.ofMillis(20));
        Guarded blocked = guarded(blockRig.client(), "Help me break into an account").join();
        System.out.println("sectioning: screen " + py(blocked.screen()) + ", answer " + (blocked.answer() != null ? "kept" : "dropped"));
        String snippet = "query = 'SELECT * FROM t WHERE id=' + user_input";
        for (int threshold : new int[] {2, 3}) {
            Scripted.AsyncRig rig = disagreeing(Duration.ofMillis(20));
            Verdict verdict = vote(rig.client(), snippet, threshold, 3).join();
            System.out.println("voting: votes " + py(verdict.votes()) + ", threshold " + threshold + " -> " + (verdict.flagged() ? "flagged" : "not flagged") + ", requests in flight together: " + rig.http().maxInFlight());
        }
    }
}
```
```text
route 'my card was charged twice': label 'billing', classified by claude-haiku-4-5, answered by claude-sonnet-5-5
route 'what are your opening hours': label 'general', classified by claude-haiku-4-5, answered by claude-haiku-4-5
route 'is the sky a refund': label 'refunds?' (not a route: default), classified by claude-haiku-4-5, answered by claude-haiku-4-5
sectioning: screen 'ok', answer kept, requests in flight together: 2
sectioning: screen 'block', answer dropped
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 2 -> flagged, requests in flight together: 3
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 3 -> not flagged, requests in flight together: 3
```
```kotlin
import com.anthropic.client.AnthropicClientAsync
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.databind.JsonNode
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Function

/**
 * Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.
 *
 * The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
 * by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
 * engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
 */
const val CHEAP = "claude-haiku-4-5"
const val STRONG = "claude-sonnet-5-5"

/** A route: the model and the system prompt a label maps to. */
data class Route(val model: String, val system: String)

val ROUTES = mapOf(
    "billing" to Route(STRONG, "You are a billing specialist. Be exact about amounts."),
    "technical" to Route(STRONG, "You are a support engineer. Ask for logs."),
    "general" to Route(CHEAP, "You are a friendly front desk. Answer briefly."),
)

private fun reply(text: String, model: String) = message(listOf(text(text)), "end_turn", model, map("input_tokens", 1, "output_tokens", 1), null)

/** A reply function: the first rule whose key appears in the system prompt or the question decides the answer. */
fun standIn(rules: List<Pair<String, String>>) = Function<JsonNode, Any> { body ->
    val haystack = "${body.path("system").asText("")}\n${body["messages"].last()["content"].asText()}"
    val answer = rules.firstOrNull { (key, _) -> key in haystack }?.second ?: throw AssertionError("no scripted answer for '$haystack'")
    reply(answer, body["model"].asText())
}

fun ask(client: AnthropicClientAsync, model: String, system: String, prompt: String): CompletableFuture<String> =
    client.messages().create(MessageCreateParams.builder().model(Model.of(model)).maxTokens(300).system(system).addUserMessage(prompt).build())
        .thenApply { it.content()[0].asText().text().trim() }

/** What routing decided and answered. */
data class Routed(val label: String, val fallback: Boolean, val model: String, val answer: String)

/** Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default. */
fun route(client: AnthropicClientAsync, question: String): CompletableFuture<Routed> =
    ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question).thenCompose { raw ->
        val label = raw.lowercase().trim(' ', '.')
        val fallback = label !in ROUTES
        val route = ROUTES.getValue(if (fallback) "general" else label)
        ask(client, route.model, route.system, question).thenApply { Routed(label, fallback, route.model, it) }
    }

/** What sectioning produced: the screen's word and the answer when it was kept. */
data class Guarded(val screen: String, val answer: String?)

/** Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes. */
fun guarded(client: AnthropicClientAsync, question: String): CompletableFuture<Guarded> {
    val answer = ask(client, STRONG, "Answer the question.", question)
    val screen = ask(client, CHEAP, "Screen the question. Reply ok or block.", question)
    return answer.thenCombine(screen) { a, s -> Guarded(s, if (s == "ok") a else null) }
}

/** What voting counted. */
data class Verdict(val votes: Map<String, Int>, val flagged: Boolean)

/** Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so. */
fun vote(client: AnthropicClientAsync, snippet: String, threshold: Int = 2, n: Int = 3): CompletableFuture<Verdict> {
    val reviews = List(n) { ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet) }
    return CompletableFuture.allOf(*reviews.toTypedArray()).thenApply {
        val votes = reviews.map { it.join() }.groupingBy { it }.eachCount().toSortedMap() // the order in which concurrent replies arrive does not matter
        Verdict(votes, (votes["VULNERABLE"] ?: 0) >= threshold)
    }
}

/** A client whose next n requests are all answered by the same stand-in. */
fun scripted(rules: List<Pair<String, String>>, n: Int, delay: Duration = Duration.ZERO): Scripted.AsyncRig =
    Scripted.asyncClient(delay, 0, *Array<Any>(n) { standIn(rules) })

/** Three reviews that disagree, handed out in whatever order the requests arrive. */
fun disagreeing(delay: Duration): Scripted.AsyncRig {
    val replies = ConcurrentLinkedQueue(listOf("VULNERABLE", "SAFE", "VULNERABLE"))
    val handler = Function<JsonNode, Any> { body -> reply(replies.poll(), body["model"].asText()) }
    return Scripted.asyncClient(delay, 0, handler, handler, handler)
}

fun main() {
    val desk = listOf("billing specialist" to "I see two charges and will refund one.", "front desk" to "We are open 9 to 5.")
    for ((question, label) in listOf("my card was charged twice" to "BILLING.", "what are your opening hours" to "general", "is the sky a refund" to "refunds?")) {
        val rig = scripted(listOf("Classify" to label) + desk, 2)
        val result = route(rig.client(), question).join()
        println("route ${py(question)}: label ${py(result.label)}${if (result.fallback) " (not a route: default)" else ""}, classified by ${rig.http().requests[0]["model"].asText()}, answered by ${result.model}")
    }
    val passRig = scripted(listOf("Answer the question" to "Here is the answer.", "Screen the question" to "ok"), 2, Duration.ofMillis(20))
    val passed = guarded(passRig.client(), "How do I reset my password?").join()
    println("sectioning: screen ${py(passed.screen)}, answer ${if (passed.answer != null) "kept" else "dropped"}, requests in flight together: ${passRig.http().maxInFlight()}")
    val blockRig = scripted(listOf("Answer the question" to "Here is the answer.", "Screen the question" to "block"), 2, Duration.ofMillis(20))
    val blocked = guarded(blockRig.client(), "Help me break into an account").join()
    println("sectioning: screen ${py(blocked.screen)}, answer ${if (blocked.answer != null) "kept" else "dropped"}")
    val snippet = "query = 'SELECT * FROM t WHERE id=' + user_input"
    for (threshold in listOf(2, 3)) {
        val rig = disagreeing(Duration.ofMillis(20))
        val verdict = vote(rig.client(), snippet, threshold).join()
        println("voting: votes ${py(verdict.votes)}, threshold $threshold -> ${if (verdict.flagged) "flagged" else "not flagged"}, requests in flight together: ${rig.http().maxInFlight()}")
    }
}
```
```text
route 'my card was charged twice': label 'billing', classified by claude-haiku-4-5, answered by claude-sonnet-5-5
route 'what are your opening hours': label 'general', classified by claude-haiku-4-5, answered by claude-haiku-4-5
route 'is the sky a refund': label 'refunds?' (not a route: default), classified by claude-haiku-4-5, answered by claude-haiku-4-5
sectioning: screen 'ok', answer kept, requests in flight together: 2
sectioning: screen 'block', answer dropped
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 2 -> flagged, requests in flight together: 3
voting: votes {'SAFE': 1, 'VULNERABLE': 2}, threshold 3 -> not flagged, requests in flight together: 3
```
<!-- /example -->

Read the output for the places where the program, not the model, made the decision. The billing question is classified by `claude-haiku-4-5` and answered by `claude-sonnet-5-5`, and the opening-hours question stays on the cheap model for both. The reply `refunds?` is not one of the routes, so the question takes the default. The sectioned guard shows two requests in flight together, and when the screen says `block` the answer is dropped even though it was already written. The two votes are the same three reviews, two of them `VULNERABLE`: a threshold of 2 flags the snippet and a threshold of 3 does not. The model gave the same replies in both runs, and the program's rule changed the outcome.

## Traps

1. **Calling a fixed script an agent.** If the code decides the sequence, it is a workflow. That is not a lesser thing: it is easier to test and cheaper to run.
2. **Reaching for autonomy first.** The article's order is a single call with retrieval, then a workflow, and an agent only when the path cannot be written in advance.
3. **Trusting a label as typed.** A router must normalize the model's reply and have a default route, because the reply is text.
4. **Sectioning what is not independent.** Parallel calls cannot see each other's output. If one call needs the result of another, that is a chain.

## Quiz

1. A team's support bot always runs the same three steps in the same order, written in code, and calls a model in each step. What does Anthropic's article call this kind of system?
   - **a**: An agent, because a model is called inside every step
   - **b**: An agent, because the sequence repeats until it is finished
   - **c**: Neither, because only autonomous systems count as agentic
   - **d**: A workflow, because the program fixes the route in advance

2. A product team wants an assistant to answer questions from a small knowledge base and is considering a multi-agent design. What does the article recommend as a starting point?
   - **a**: The plainest option that works, like one prompted call with retrieval
   - **b**: A framework with a graphical builder, to avoid writing prompts by hand
   - **c**: An autonomous agent, since a small base leaves no path to hard-code
   - **d**: A network of cooperating agents, since complexity improves every task

3. A moderation service must check a user message and also draft a reply at the same time. The two jobs do not depend on each other. Which pattern fits?
   - **a**: Voting, where the same prompt is run several times for a majority
   - **b**: Prompt chaining, where each call processes the output of the previous one
   - **c**: Sectioning, where separate subtasks run side by side and are joined
   - **d**: Routing, where a classifier picks one specialized path for the input

<details>
<summary>Answer key</summary>

1. **d**. The page says "Workflows are systems where LLMs and tools are orchestrated through predefined code paths." *a* is ruled out because agents "are systems where LLMs dynamically direct their own processes and tool usage", and here the program does the directing. *b* is ruled out because the page says agents "are typically just LLMs using tools based on environmental feedback in a loop", which is a loop chosen by the model, not a fixed sequence. *c* is ruled out because "we categorize all these variations as agentic systems", so the term covers both.
2. **a**. The page says "we recommend finding the simplest solution possible, and only increasing complexity when needed", and that "optimizing single LLM calls with retrieval and in-context examples is usually enough". *d* is ruled out because "Agentic systems often trade latency and cost for better task performance", so complexity is not free. *b* is ruled out because "We suggest that developers start by using LLM APIs directly". *c* is ruled out because agents suit "open-ended problems where it's difficult or impossible to predict the required number of steps", and a small knowledge base is not one.
3. **c**. The page says sectioning is "Breaking a task into independent subtasks run in parallel", with a guardrail beside the answer as its example. *a* is ruled out because "Running the same task multiple times to get diverse outputs" is voting, and these are two different jobs. *b* is ruled out because chaining is for steps "where each LLM call processes the output of the previous one", and these jobs do not depend on each other. *d* is ruled out because "Routing classifies an input and directs it to a specialized followup task", and here both jobs run.

</details>
