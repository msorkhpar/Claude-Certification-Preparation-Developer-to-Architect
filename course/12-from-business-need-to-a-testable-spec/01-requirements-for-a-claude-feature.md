# Requirements for a Claude feature

**Level:** Developer · **Module 12:** From business need to a testable spec · **Page 1 of 3**
**Exams:** DV1

**After this page you can** turn a business request into functional requirements that a test can pass or fail,
including the requirements that only exist because the feature calls a model.

Checked against the Developer exam guide (domain 1, version 1.0, July 2026) and the Claude API documentation
(overview, errors, rate limits, models) on 2026-10-02, for `claude-sonnet-5-5` and `claude-haiku-4-5-20251001`. No
live API call was made for this page.

## Why it matters

A product owner writes "add AI summaries to the support console". Three weeks later the team has a demo that
impresses everyone and cannot say whether it is done. Nobody wrote down how long a summary may take, what it may
cost, what it must never contain, or what happens when the model refuses or the API is down. The exam's first
domain, applications and integration, starts here: before any code, the feature has to be stated in a form that a
test can fail. A feature that cannot fail a test cannot be finished, and it cannot be operated either.

## The idea

### A requirement is a sentence a test can fail

A functional requirement says what the feature does for a user in terms you can observe. "Summaries are good" fails
that test; "a summary of a ticket is at most 80 words and names the customer's product" passes it, because a script
can count words and look for a product name. The habit to build is to write each requirement with three parts:

1. **The input it applies to**: which tickets, which language, how long.
2. **The observable result**: the shape, the length, the fields, the refusal, the error.
3. **The check**: a script, a rubric with a grader, or a person with a checklist.

A Claude feature adds a kind of requirement that ordinary software rarely needs, because the component in the
middle is a model: **what counts as a good enough answer, how often it may be wrong, and what the system does when it
is**. The model's output is not a fixed function of its input, so a requirement about answer quality is stated as a
rate over a set of examples, not as a single expected string.

### Five kinds of requirement

| Kind | Example for the support-console summary | Typical check |
|---|---|---|
| **Behaviour** | Summarise one ticket thread into at most 80 words; name the product | A script over a sample of tickets |
| **Quality** | At least 90 percent of summaries in the reviewed sample are rated "accurate" by two support leads | A fixed evaluation set and a grader (module 42) |
| **Safety and policy** | A summary never repeats a card number or an access token that appears in the thread | A script over planted test tickets |
| **Failure behaviour** | If the model call fails or is refused, the console shows the raw thread and a notice, within 2 seconds | A scripted failure in a test |
| **Operational** | Calls, tokens and errors are logged by request id, without the ticket text | A log inspection test |

The fourth row is the one teams forget. A feature that calls a remote service has failure modes the product owner
never mentioned: the call can time out, the service can be overloaded, a refusal can come back with a normal
status, and the reply can stop at the length limit in the middle of a sentence. Each of those needs a
sentence in the requirements, because each needs a branch in the code and a test for that branch.

### What the API itself promises

The documentation is explicit that the API reports problems in two different ways, and a requirement has to name
both. A request can fail with an HTTP error, which module 15 classifies. A request can also succeed and still not
give you what you wanted, and the response tells you why in its `stop_reason`. The documentation lists the values
`end_turn`, `max_tokens`, `stop_sequence`, `tool_use`, `pause_turn`, `refusal` and `model_context_window_exceeded`
and gives this advice:

> Distinguish stop_reason from errors: stop_reason indicates normal completion; HTTP errors indicate failures

Source: Handling stop reasons, Claude API documentation (summarised, 2026-10-02).

So the failure requirement for the summary feature has at least four rows: the call fails, the model refuses, the
reply is cut at `max_tokens`, and the reply is fine but wrong. Module 14 shows how to read `stop_reason`; this page
only insists that the requirements list them.

### Acceptance criteria and the evaluation set

A requirement about quality needs an **evaluation set**: a fixed list of inputs with the expected properties of a good
answer, kept under version control and rerun whenever the prompt, the model or the code changes. You do not need a
large one to begin. Twenty real tickets, chosen to include the awkward cases (very long threads, two languages, a
thread with a pasted log), tell you more than two hundred easy ones. Module 42 builds the machinery; here the point
is that **the acceptance criterion for quality is a number over that set**, written before the first prompt is tried,
so that a change can be judged and not just admired.

Three more things belong in the first version of a spec:

- **Out of scope.** "Summaries are for support agents, never shown to customers" removes a whole class of policy questions.
- **Who decides.** If the model's answer drives an action (a refund, a ticket closure), the requirement says whether a
  person approves it. A guarantee that must always hold belongs in code, not in a prompt; the spec says which rules
  those are.
- **The unit of work.** One ticket per call, or a whole queue? It decides the cost model on the next page.

<!-- illustrative -->
A requirement written the three-part way, with a check for each. The wording is hand-written for this page.

```text
R1  Input: any ticket thread up to 6,000 words, English or German.
    Result: a summary of at most 80 words that names the product the customer wrote about.
    Check: script over the 20-ticket evaluation set (word count, product name present).
R2  Input: a thread that contains a 16-digit card number.
    Result: the number does not appear in the summary.
    Check: script over 5 planted tickets.
R3  Input: any thread, when the model call fails with a 429, a 5xx or a timeout.
    Result: the console shows the raw thread and a notice within 2 seconds; one retry at most.
    Check: scripted failures in a unit test (the practice of module 15).
```
<!-- /illustrative -->

## Traps

1. **"The summary should be accurate."** It reads as a requirement and checks nothing. A requirement that only a
   person's impression can settle is a wish, not a requirement. Replace it with a rate over a named set and a named
   grader, or it will be argued about forever.
2. **Describing only the happy path.** The reply that stops at the length limit, the refusal and the timeout are all
   normal events of a model call. A spec without failure behaviour produces code without it.
3. **Putting a hard rule in a prompt.** A rule such as never output a card number is only a request to the model; a
   regular expression in your code is a guarantee. The spec should say which of its rules need the guarantee.

## Quiz

1. A product owner asks that a new feature for writing customer emails must "sound professional". The team wants a
   requirement that a test can fail. Which version is best?
   - **a**: Ninety percent of outputs in a fixed sample of forty tickets pass a tone rubric that a grader applies
   - **b**: The support lead reads a handful of outputs before release and judges that they sound professional
   - **c**: The prompt gives the instruction to write in a professional and courteous voice whenever it replies
   - **d**: Customers rate each email after sending, and the feature stays while the ratings stay positive

2. A draft spec for an invoice-extraction feature lists the fields to extract, the accepted file types and the
   expected accuracy. Which missing item should a reviewer ask the team to add first?
   - **a**: The exact system prompt that the developers plan to use for the extraction
   - **b**: One expected output string for each sample invoice, compared character by character
   - **c**: What the screen shows the user when a call to the model errors, is refused or stops early
   - **d**: A recorded demo of the extraction running cleanly on a handful of sample invoices

<details>
<summary>Answer key</summary>

1. **a**. It states a rate over a fixed sample and names a grader, which is what the page asks of a quality requirement: a number over that set. *b* is ruled out because "a requirement that only a person's impression can settle is a wish", and a read-through by one lead is only an impression. *c* is ruled out because a rule in a prompt "is only a request to the model", and a prompt line is not a test. *d* is ruled out because ratings after sending are an operational signal, while the acceptance criterion must be "written before the first prompt is tried".
2. **c**. Failure behaviour is the row the page says teams forget, because a spec without it produces code without it. *a* is ruled out because the quality number is "written before the first prompt is tried", so the prompt is an implementation choice that comes after the spec. *b* is ruled out because quality "is stated as a rate over a set of examples, not as a single expected string". *d* is ruled out because a demo can be one that "impresses everyone and cannot say whether it is done", so it adds no line a test can fail.

</details>
