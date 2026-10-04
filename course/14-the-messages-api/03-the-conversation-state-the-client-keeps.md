# The conversation state the client keeps, and a conversation client

**Level:** Developer · **Module 14:** The Messages API · **Page 3 of 3**
**Exams:** DV1; A1.1

**After this page you can** write a client that keeps the history, the usage totals and the stop reasons of a
conversation, survives a failed call without corrupting its history, and show what each request carries.

Checked against the Claude API documentation (Using the Messages API, Handling stop reasons, API errors) on
2026-10-02, and by running the example and the practice offline in the course container with `anthropic` 1.11.0,
`@anthropic-ai/sdk` 0.131.0, Java 25 and Kotlin 2.4. The model id is `claude-sonnet-5-5`; replies are hand-scripted
and labelled illustrative.

## Why it matters

Because the API is stateless, the conversation is a data structure in your program, with a life cycle you have to
design: it grows, it can be damaged by a failed call, it costs more with every turn, it can hold a customer's private
text, and a bug in how it is stored shows up as strange model behaviour. This is the part of a Claude integration
that is pure software engineering and the part the exam can test most precisely, since the right answers are
deterministic. It is also the loop of the agent you will build in module 34: an agent is this client with tools added.

## The idea

### What the client has to keep

| State | Why |
|---|---|
| **The history**: every user turn and every assistant turn, assistant content **as received** | The whole list goes in every request |
| **The settings that do not change per turn**: model id, `max_tokens`, `system`, stop sequences | Sent with every request; `system` is a top-level field |
| **Usage totals** from each reply's `usage` | Cost, and an early warning for the context window |
| **The last `stop_reason`**, and whether the reply was cut | The next action depends on it (page 2) |

Everything else (the user's name, the session id, the feature that started it) lives beside the conversation, not
in it.

### The example: three turns through the real SDK

The program below keeps a history list, sends all of it on every turn and adds up the usage. It uses the real SDK
against a scripted transport. The three replies end three different ways: normally, at `max_tokens`, and at a stop
sequence. Read the output for what each request contained.

<!-- example: m14-conversation tabs: python,typescript,java,kotlin -->
```python
"""A conversation the client keeps: the API is stateless, so every request carries the whole history.

Three turns through the real SDK against a scripted transport. The replies are illustrative,
hand-written Messages responses (claude-sonnet-5-5), not captures.
"""
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
SYSTEM = "You answer in one short sentence."

REPLIES = [
    message([text("Paris.")], usage={"input_tokens": 18, "output_tokens": 4}),
    message([text("It has been the capital since")], "max_tokens", usage={"input_tokens": 30, "output_tokens": 6}),
    message([text("Seine")], "stop_sequence", usage={"input_tokens": 41, "output_tokens": 2}, stop_sequence="END"),
]
QUESTIONS = ["Capital of France?", "Since when?", "Name its river. End with END."]


def run(client, questions):
    """Keep the history in a list and send all of it every time."""
    history, totals = [], {"input": 0, "output": 0}
    for question in questions:
        history.append({"role": "user", "content": question})
        reply = client.messages.create(model=MODEL, max_tokens=16, system=SYSTEM, messages=history,
                                       stop_sequences=["END"])
        history.append({"role": "assistant", "content": reply.content})
        totals["input"] += reply.usage.input_tokens
        totals["output"] += reply.usage.output_tokens
        yield reply, totals


def main():
    transport = ScriptedTransport(*REPLIES)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=transport))
    for number, (reply, totals) in enumerate(run(client, QUESTIONS), start=1):
        sent = transport.requests[-1]["messages"]
        roles = ", ".join(m["role"] for m in sent)
        print(f"turn {number}: sent {len(sent)} message(s) [{roles}] -> {reply.stop_reason}"
              f"{' ' + repr(reply.stop_sequence) if reply.stop_sequence else ''}, {reply.content[0].text!r}")
    print("totals:", totals)
    print("system is a top-level field:", all(r["system"] == SYSTEM for r in transport.requests),
          "| roles ever used in messages:", sorted({m["role"] for r in transport.requests for m in r["messages"]}))


if __name__ == "__main__":
    main()
```
```text
turn 1: sent 1 message(s) [user] -> end_turn, 'Paris.'
turn 2: sent 3 message(s) [user, assistant, user] -> max_tokens, 'It has been the capital since'
turn 3: sent 5 message(s) [user, assistant, user, assistant, user] -> stop_sequence 'END', 'Seine'
totals: {'input': 89, 'output': 12}
system is a top-level field: True | roles ever used in messages: ['assistant', 'user']
```
```typescript
// A conversation the client keeps: the API is stateless, so every request carries the whole history.
// Three turns through the real SDK against a scripted fetch. The replies are illustrative,
// hand-written Messages responses (claude-sonnet-5-5), not captures.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

const MODEL = "claude-sonnet-5-5";
const SYSTEM = "You answer in one short sentence.";

export const REPLIES = [
  { body: message([text("Paris.")], "end_turn", { input_tokens: 18, output_tokens: 4 }) },
  { body: message([text("It has been the capital since")], "max_tokens", { input_tokens: 30, output_tokens: 6 }) },
  { body: { ...message([text("Seine")], "stop_sequence", { input_tokens: 41, output_tokens: 2 }), stop_sequence: "END" } },
];
export const QUESTIONS = ["Capital of France?", "Since when?", "Name its river. End with END."];

// Keep the history in an array and send all of it every time.
export async function run(client: Anthropic, questions: string[]) {
  const history: Anthropic.MessageParam[] = [];
  const totals = { input: 0, output: 0 };
  const replies: Anthropic.Message[] = [];
  for (const question of questions) {
    history.push({ role: "user", content: question });
    const reply = await client.messages.create({
      model: MODEL,
      max_tokens: 16,
      system: SYSTEM,
      messages: history,
      stop_sequences: ["END"],
    });
    history.push({ role: "assistant", content: reply.content });
    totals.input += reply.usage.input_tokens;
    totals.output += reply.usage.output_tokens;
    replies.push(reply);
  }
  return { replies, totals };
}

async function main() {
  const fake = scriptedFetch(REPLIES);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const { replies, totals } = await run(client, QUESTIONS);
  replies.forEach((reply, i) => {
    const sent = fake.seen[i].body.messages;
    const roles = sent.map((m: any) => m.role).join(", ");
    const stop = reply.stop_sequence ? ` '${reply.stop_sequence}'` : "";
    console.log(`turn ${i + 1}: sent ${sent.length} message(s) [${roles}] -> ${reply.stop_reason}${stop}, '${(reply.content[0] as any).text}'`);
  });
  console.log("totals:", `{ input: ${totals.input}, output: ${totals.output} }`);
  const roles = [...new Set(fake.seen.flatMap((r) => r.body.messages.map((m: any) => m.role)))].sort();
  console.log("system is a top-level field:", fake.seen.every((r) => r.body.system === SYSTEM), "| roles ever used in messages:", roles);
}

if (import.meta.main) await main();
```
```text
turn 1: sent 1 message(s) [user] -> end_turn, 'Paris.'
turn 2: sent 3 message(s) [user, assistant, user] -> max_tokens, 'It has been the capital since'
turn 3: sent 5 message(s) [user, assistant, user, assistant, user] -> stop_sequence 'END', 'Seine'
totals: { input: 89, output: 12 }
system is a top-level field: true | roles ever used in messages: [ 'assistant', 'user' ]
```
```java
import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A conversation the client keeps: the API is stateless, so every request carries the whole history.
 *
 * <p>Three turns through the real SDK against a scripted transport. The replies are illustrative,
 * hand-written Messages responses (claude-sonnet-5-5), not captures.
 */
public final class Conversation {
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You answer in one short sentence.";

    static final List<Map<String, Object>> REPLIES = List.of(
        message(List.of(text("Paris.")), "end_turn", MODEL, map("input_tokens", 18, "output_tokens", 4), null),
        message(List.of(text("It has been the capital since")), "max_tokens", MODEL, map("input_tokens", 30, "output_tokens", 6), null),
        message(List.of(text("Seine")), "stop_sequence", MODEL, map("input_tokens", 41, "output_tokens", 2), "END"));
    static final List<String> QUESTIONS = List.of("Capital of France?", "Since when?", "Name its river. End with END.");

    /** One turn: the reply and the token totals so far. */
    record Turn(Message reply, long input, long output) {}

    /** Keep the history in the request builder and send all of it every time. */
    static List<Turn> run(AnthropicClient client, List<String> questions) {
        MessageCreateParams.Builder history = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).system(SYSTEM).stopSequences(List.of("END"));
        long input = 0, output = 0;
        List<Turn> turns = new ArrayList<>();
        for (String question : questions) {
            history.addUserMessage(question);
            Message reply = client.messages().create(history.build());
            history.addMessage(reply); // the assistant turn goes back exactly as it was received
            input += reply.usage().inputTokens();
            output += reply.usage().outputTokens();
            turns.add(new Turn(reply, input, output));
        }
        return turns;
    }

    public static void main(String[] args) {
        ScriptedHttp transport = Scripted.http(REPLIES.toArray());
        List<Turn> turns = run(Scripted.clientOn(transport, 0), QUESTIONS);
        for (int number = 1; number <= turns.size(); number++) {
            Message reply = turns.get(number - 1).reply();
            JsonNode sent = transport.requests.get(number - 1).get("messages");
            List<String> roles = new ArrayList<>();
            sent.forEach(m -> roles.add(m.get("role").asText()));
            String stop = reply.stopReason().get().asString();
            String sequence = reply.stopSequence().map(s -> " '" + s + "'").orElse("");
            System.out.println("turn " + number + ": sent " + sent.size() + " message(s) [" + String.join(", ", roles) + "] -> " + stop + sequence + ", '" + reply.content().get(0).asText().text() + "'");
        }
        Turn last = turns.get(turns.size() - 1);
        System.out.println("totals: {'input': " + last.input() + ", 'output': " + last.output() + "}");
        boolean systemTopLevel = transport.requests.stream().allMatch(r -> SYSTEM.equals(r.path("system").asText()));
        TreeSet<String> used = new TreeSet<>();
        transport.requests.forEach(r -> r.get("messages").forEach(m -> used.add(m.get("role").asText())));
        System.out.println("system is a top-level field: " + (systemTopLevel ? "True" : "False") + " | roles ever used in messages: "
            + used.stream().map(r -> "'" + r + "'").collect(Collectors.joining(", ", "[", "]")));
    }
}
```
```text
turn 1: sent 1 message(s) [user] -> end_turn, 'Paris.'
turn 2: sent 3 message(s) [user, assistant, user] -> max_tokens, 'It has been the capital since'
turn 3: sent 5 message(s) [user, assistant, user, assistant, user] -> stop_sequence 'END', 'Seine'
totals: {'input': 89, 'output': 12}
system is a top-level field: True | roles ever used in messages: ['assistant', 'user']
```
```kotlin
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text

/**
 * A conversation the client keeps: the API is stateless, so every request carries the whole history.
 *
 * Three turns through the real SDK against a scripted transport. The replies are illustrative,
 * hand-written Messages responses (claude-sonnet-5-5), not captures.
 */
const val MODEL = "claude-sonnet-5-5"
const val SYSTEM = "You answer in one short sentence."

val REPLIES = listOf(
    message(listOf(text("Paris.")), "end_turn", MODEL, map("input_tokens", 18, "output_tokens", 4), null),
    message(listOf(text("It has been the capital since")), "max_tokens", MODEL, map("input_tokens", 30, "output_tokens", 6), null),
    message(listOf(text("Seine")), "stop_sequence", MODEL, map("input_tokens", 41, "output_tokens", 2), "END"),
)
val QUESTIONS = listOf("Capital of France?", "Since when?", "Name its river. End with END.")

/** One turn: the reply and the token totals so far. */
data class Turn(val reply: Message, val input: Long, val output: Long)

/** Keep the history in the request builder and send all of it every time. */
fun run(client: AnthropicClient, questions: List<String>): List<Turn> {
    val history = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).system(SYSTEM).stopSequences(listOf("END"))
    var input = 0L
    var output = 0L
    return questions.map { question ->
        history.addUserMessage(question)
        val reply = client.messages().create(history.build())
        history.addMessage(reply) // the assistant turn goes back exactly as it was received
        input += reply.usage().inputTokens()
        output += reply.usage().outputTokens()
        Turn(reply, input, output)
    }
}

fun main() {
    val transport = Scripted.http(*REPLIES.toTypedArray())
    val turns = run(Scripted.clientOn(transport, 0), QUESTIONS)
    for ((index, turn) in turns.withIndex()) {
        val sent = transport.requests[index]["messages"]
        val roles = sent.map { it["role"].asText() }
        val sequence = turn.reply.stopSequence().map { " '$it'" }.orElse("")
        println("turn ${index + 1}: sent ${sent.size()} message(s) [${roles.joinToString(", ")}] -> ${turn.reply.stopReason().get().asString()}$sequence, '${turn.reply.content()[0].asText().text()}'")
    }
    println("totals: {'input': ${turns.last().input}, 'output': ${turns.last().output}}")
    val systemTopLevel = transport.requests.all { it.path("system").asText() == SYSTEM }
    val used = transport.requests.flatMap { r -> r["messages"].map { it["role"].asText() } }.toSortedSet()
    println("system is a top-level field: ${if (systemTopLevel) "True" else "False"} | roles ever used in messages: ${used.joinToString(", ", "[", "]") { "'$it'" }}")
}
```
```text
turn 1: sent 1 message(s) [user] -> end_turn, 'Paris.'
turn 2: sent 3 message(s) [user, assistant, user] -> max_tokens, 'It has been the capital since'
turn 3: sent 5 message(s) [user, assistant, user, assistant, user] -> stop_sequence 'END', 'Seine'
totals: {'input': 89, 'output': 12}
system is a top-level field: True | roles ever used in messages: ['assistant', 'user']
```
<!-- /example -->

What the output shows:

1. **The list grows by two entries per turn** (the user turn and the assistant turn): 1, 3, 5 messages. The second and
   third requests carry the earlier turns again, which is the statelessness of page 1 made visible.
2. **The assistant turn is stored as received** (`reply.content`, a list of blocks) and not rebuilt from text.
3. **`system` is the same top-level field on every request** and never a message role.
4. **Each reply ends differently**, and the client keeps the reason: `end_turn`, `max_tokens` (the second reply stops
   mid-sentence) and `stop_sequence` with the sequence that matched.
5. **Usage adds up**: 89 input tokens and 12 output tokens over the three turns in this scripted run.

The Java and Kotlin tabs build the same list with `addUserMessage` and `addMessage` (which takes the reply the SDK returned)
on `MessageCreateParams`, and print the same lines. The practice below runs in all four languages.

### The cost of a long conversation

Every turn resends everything before it. If each exchange adds about 200 tokens, turn *n* sends about 200 *n* input
tokens, and a conversation of *n* turns pays for about 200 *n*(*n* + 1) / 2 input tokens in total: **the input cost
of a conversation grows with the square of its length**. A 40-turn conversation costs roughly 15 times what the
first 10 turns did, not 4 times. Three controls exist, taught later: prompt caching makes the repeated prefix cheaper
(module 19) and does not count cached reads toward the input-token rate limit on most models, compaction summarises
old turns (module 25), and a hard cap on turns ends the conversation politely. Until you add one, the client should at
least count the tokens, so the growth is visible.

### Failure leaves no mark

A call can fail after you appended the user turn: a 529, a timeout, a 429. If the client leaves that turn in the
history, the next request contains two user turns in a row, and the retry logic of module 15 will send a different
conversation from the one the user saw. The rule is to **commit a turn only when the whole exchange succeeded**:
append the user turn, call, and on any failure remove it before the error leaves the function. Case `e2` of the
practice is exactly this.

Two more defects in the same family, each a practice case:

- **Aliasing.** If the request body holds the client's own history list, a later turn changes earlier requests
  (`e5`). That is invisible in single-threaded tests and a heisenbug under load, and it also corrupts anything that
  logs or retries a request later. Send a snapshot.
- **A silent blank.** An empty user turn is a wasted call at best; refuse it before anything is sent (`e6`).

### Cut replies stay in the history

When a reply stops at `max_tokens`, the client reports `truncated` and keeps the turn as received. The user can then
say "continue" in a normal next turn, and the model sees its own cut-off text. What the client must not do on current
models is try to continue by ending the list with the cut text as an assistant turn: that is a prefill, which page 1
shows returns a 400.

## The practice: a conversation client

You write `Conversation`, in Python, TypeScript, Java or Kotlin, against an injected `send` function that the tests
script. It keeps the history, builds each request, accumulates the usage, reports the stop reason, and survives
failure. The statement, with the exact contract, is in
`exercises/14-the-messages-api/unit-01/practice-1/statement.md`; each language folder has a `starter`, the `tests` and
a `run.sh` or build file. The starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Every request carries the whole history in order, with assistant content as received |
| `e1` | Usage adds up over the turns |
| `e2` | A failed call leaves no dangling user turn |
| `e3` | The stop reason is reported and `max_tokens` marks the reply truncated |
| `e4` | `system` is top-level (and left out when absent); stop sequences are passed on |
| `e5` | Each request is a snapshot, and the history you read back is a copy |
| `e6` | A blank turn is refused before anything is sent |

## Traps

1. **Keeping only the text of replies.** The history then loses `tool_use` and thinking blocks. Store the content list
   as received.
2. **Appending the user turn and then calling without a rollback.** One timeout leaves two user turns in a row, and
   every later request is a different conversation from the one the user sees.
3. **Letting the cost grow unseen.** A conversation's input cost grows with the square of its length. Count tokens
   per turn from the first day.

## Quiz

1. A chat client appends each user turn to its history before calling the API. After a night of occasional 529
   errors, some conversations answer oddly, and their stored history shows two user turns in a row. What is the best
   fix?
   - **a**: Retry every failed call forever so that no turn is ever left without its answer by morning
   - **b**: Take that entry out again whenever the call raises, so that only whole exchanges are kept
   - **c**: Merge any two consecutive user turns into one when the history is saved
   - **d**: Store only the assistant turns, which are the ones that the model produced

2. A product team notices that a support conversation of 40 turns costs far more than four conversations of 10
   turns, although the total number of questions is the same. What explains it?
   - **a**: The system prompt is repeated once for every question, so it dominates the bill
   - **b**: Long conversations are charged at a higher price for each output token that is produced
   - **c**: The model writes longer replies as a conversation grows, and the replies dominate the bill in the end
   - **d**: Each call repeats the earlier exchanges, so the input bill grows with the square of the length

<details>
<summary>Answer key</summary>

1. **b**. The rule is to "commit a turn only when the whole exchange succeeded", which means removing the user turn on any failure. *a* is ruled out because a retry is a separate decision, and the page says the retry "will send a different conversation from the one the user saw" if the history is damaged. *c* is ruled out because a merge hides the damage while "the next request contains two user turns in a row" in the very same way, and a question that never got an answer stays in the history. *d* is ruled out because "the whole list goes in every request", and a list without user turns is not a conversation.
2. **d**. The page says "the input cost of a conversation grows with the square of its length", because each turn resends everything before it. *b* is ruled out because nothing on the page prices long conversations differently, and the growth comes from input: "every turn resends everything before it". *c* is ruled out because "turn n sends about 200 n input tokens", which is growth in input and not in replies. *a* is ruled out because the system prompt is the same size on every turn, and the page says "every turn resends everything before it", so the history is what grows.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. After a team adds a tool, some conversations fail with a 400 or lose the model's reasoning when they are resumed
   from the database. The save code keeps only the text of each reply. What is the best fix?
   - **a**: Save the usage totals with each turn so that the conversation can be rebuilt from them
   - **b**: Store each reply's content list exactly as received and send it back unchanged
   - **c**: Save the model id with each turn so that the same tier answers after a resume
   - **d**: Follow every tool result with an explanatory text block when the turn is rebuilt

2. A client receives `stop_reason` of `max_tokens` on the fifth turn of a conversation. Which handling fits the
   module?
   - **a**: Mark the reply truncated, keep it in the history, and let the user ask to continue next
   - **b**: End the list with the cut text as an assistant turn so that the model resumes writing
   - **c**: Treat the turn as failed and remove it, because a cut reply is not a whole exchange
   - **d**: Raise the sampling temperature so that the model finishes inside the limit the next time it runs

3. A team wants an early warning before long chats approach the context window and become expensive. Which signal
   can the client compute from every reply?
   - **a**: The total of the three input-token fields in the usage object
   - **b**: The count of turns so far, since each turn adds the same number of tokens
   - **c**: The output-token count alone, because the reply is what fills the window
   - **d**: The appearance of the context-window stop reason, which says the limit is close

4. A developer logs each request body for debugging and finds that bodies logged earlier in a conversation change
   after later turns. Which defect fits best?
   - **a**: A failed call was not rolled back before the error finally left the function that made it
   - **b**: The assistant content was rebuilt from text and not stored as received
   - **c**: The payload holds the client's own history list instead of a snapshot of it
   - **d**: The system prompt is resent with every turn and grows each time

<details>
<summary>Answer key</summary>

1. **b**. Storing only text drops `tool_use` and thinking blocks, and the page says to "store the content list as received". *a* is ruled out because usage totals are "cost, and an early warning for the context window", not a way to rebuild a conversation. *c* is ruled out because the model id belongs to the "settings that do not change per turn", and it does not carry blocks. *d* is ruled out because adding text after tool results "can teach the model to expect user input after every tool call".
2. **a**. A truncated reply is kept: "cut replies stay in the history", and the user can ask to continue in a normal turn. *b* is ruled out because "a request that does returns a 400 invalid_request_error", and ending the list with an assistant turn is a prefill. *c* is ruled out because the rollback rule is to "commit a turn only when the whole exchange succeeded", and a cut reply is a completed exchange. *d* is ruled out because "a non-default value of any of them is rejected with a 400 error", and sampling does not control where the limit cuts.
3. **a**. The page says the total input is the sum of three numbers, and that counting tokens makes the growth visible. *b* is ruled out because "turn n sends about 200 n input tokens", so each turn adds more than the one before and a turn count is a poor proxy. *c* is ruled out because the page says "the input cost of a conversation grows with the square of its length", and the history is input. *d* is ruled out because that value reports that "the context window filled before max_tokens", which is already too late.
4. **c**. The page describes it: "if the request body holds the client's own history list, a later turn changes earlier requests". *b* is ruled out because the page says to "store the content list as received", which concerns what a turn contains, not whether old requests change. *a* is ruled out because the rollback rule is to "commit a turn only when the whole exchange succeeded", and it concerns failures. *d* is ruled out because "system is the same top-level field on every request", so it does not grow.

</details>
