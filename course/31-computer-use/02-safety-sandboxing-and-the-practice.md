# Computer use: safety, sandboxing, long loops and the practice

**Level:** Developer · **Module 31:** Computer use · **Page 2 of 2**
**Exams:** DV5, DV6 (and X: this module goes beyond what the exam guides name)

**After this page you can** name the documented precautions for computer use and apply them to a loop, place a person's confirmation where a batch cannot skip it, keep a long loop inside the image limits, and say what the practice builds and what it leaves out.

Checked against the Claude API documentation (Computer use tool) on 2026-10-03. The example of the previous page and the practice at the end of this one ran offline in the course container. No desktop, no model and no network were involved, and nothing on this page was run against the live API.

## Why it matters

A model that reads a screen reads everything on it, including text that someone planted for it. The documentation says so plainly: "Computer use has unique risks distinct from standard API features. These risks are heightened when interacting with the internet." Most of the work of an engineer on this tool is the part around the model: where it runs, what it can see, which action needs a person, and how a long session stays inside the limits. The exam asks for those choices, and for the reason behind each.

## The idea

### The documented precautions

The documentation lists four, and each answers a specific failure:

1. "Using a dedicated virtual machine or container with minimal privileges to prevent direct system attacks or accidents." The model's mistakes then land in a disposable place.
2. "Avoiding giving the model access to sensitive data, such as account login information, to prevent information theft." What the model cannot see it cannot leak.
3. "Limiting internet access to an allowlist of domains to reduce exposure to malicious content." Planted instructions need a page to live on.
4. "Asking a human to confirm decisions that might result in meaningful real-world consequences and any tasks requiring affirmative consent, such as accepting cookies, completing financial transactions, or agreeing to terms of service."

The reason is prompt injection through the screen. "In some circumstances, Claude will follow commands found in content even when they conflict with your instructions." The commands may be "instructions embedded in webpages or images". Anthropic trained the model to resist, and classifiers scan what the tools return, such as screenshots, and steer the model to check that an instruction came from you before it acts. That layer is not a replacement: "The precautions above remain important even with these classifiers in place." It can be turned off through support for cases such as a run with no human in the loop, which is exactly where the other precautions matter most.

Logging in raises the stakes. The documentation says that "Using computer use within applications that require login increases the risk of bad outcomes as a result of prompt injection", and it suggests reading the guidance on jailbreaks and prompt injections before you hand the model credentials. Tell end users the risks: "Inform end users of relevant risks and obtain their consent prior to enabling computer use in your own products."

### Where the person goes

The fourth precaution only works if the check sits in the right place. Because a batch can complete a multistep action within one turn, the check belongs before each block runs, not once per reply and not at the start of the task. In the practice that is the `confirm` function: a click on an element marked as risky asks a person first, and with no answer or a no, the result is an error that names the element and the reason, and nothing on the screen changes. Claude sees that error, so it can explain to the user that it stopped, as the example's last line shows.

### Limits to plan for

The documentation lists limits of the tool. Latency "might be too slow compared to regular human-directed computer actions", so it points to work where speed is not critical, such as background information gathering and automated software testing, in trusted environments. Claude "might make mistakes or hallucinate when outputting specific coordinates", and its tool selection can fail too, more so in niche applications or several at once. Its advice for a loop that matters: "Always carefully review and verify Claude's computer use actions and logs." The `zoom` member exists for the first of these: it shows a region at full resolution, and the documentation suggests asking about a specific region or element rather than the screen as a whole when Claude does not zoom by itself.

Prompting helps in four documented ways. Put the instruction text before the screenshot, which improves click accuracy. Ask Claude to take a screenshot after each step and say whether the step succeeded. Suggest keyboard shortcuts for dropdowns and scrollbars. Add that every group of actions should end with a screenshot, so the model sees the result without an extra round trip.

### Long loops: screenshots fill the context

Long agent loops "accumulate screenshots quickly", roughly 1,000 to 1,800 input tokens each, and the toolset itself adds about 4,500 input tokens to a request, of which about 410 belong to `zoom`. There is a second limit that surprises people. "Once a single request carries more than 20 images, every image in it is held to a stricter per-side limit." A loop that keeps its screenshot history reaches that count within a few dozen turns. The documented choices are to resize each screenshot so that neither side exceeds 2000 px, or to prune older screenshots so that 20 or fewer remain.

Pruning has a cost for the cache: "Dropping a screenshot every turn changes the prefix every turn and invalidates the cache." The documentation suggests pruning in batches, for example keeping the last three screenshots and pruning every 25 turns. For Claude Fable 5.1, Claude Opus 5.5 and Claude Sonnet 5.5 it goes further and says to avoid pruning on the client, because removing an earlier screenshot invalidates every later thinking block in every request that still carries those turns. It prefers resizing to 2000 px or less and server-side tool result clearing, which drops old results from the context. The practice's `prune_screenshots` is the plain client-side version, and it shows the mechanism and its edge cases (a count of 0, a count above the number of screenshots, an input that must not change). It is not the recommended choice for these models, and the lesson says so because a function that passes its tests is not thereby the documented advice.

### Data and cost

Computer use is a client-side tool. "All screenshots, mouse actions, keyboard inputs, and any files involved in a session are captured and stored in your environment, not by Anthropic." Anthropic processes the images and action requests in real time as part of the API call, and the tool is eligible for zero data retention. Pricing follows the standard tool use pricing, plus the overhead of the toolset definition above.

### The practice: the loop around the tool

The practice is in `exercises/31-computer-use/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. Against a toy screen in memory and a scripted model, you write `scale_for` and `scaled_size`, `to_screen` (divide, round half to even, clamp), `perform` for the members with a reason for each refusal, `prune_screenshots`, and `run_computer_loop` with the batch rule, the halt text, the confirmation, the turn limit and the stop reasons. The tests judge what the loop sends and appends. The starter fails all of them; the reference passes them; every planted wrong solution fails on an assertion of the case it breaks, for instance a click that is not scaled back, a risky click that goes through, or a batch that keeps running after a failure. The scale rule is the documentation's example for 1568 px and about 1.15 megapixels, safe for every model. A high-resolution model accepts more, and a production loop would size to the visual-token limit instead.

## Traps

1. **Trusting the classifiers.** They are a second layer. The container, the missing secrets, the allowlist and the confirmation are the first.
2. **Confirming once per task.** A batch finishes several steps in a turn, so check each risky action before its block runs.
3. **Pruning screenshots on the client by reflex.** On the 5.5 models it invalidates thinking blocks, and every turn it breaks the cache. Resize first, clear on the server, and if you prune, prune in batches.
4. **Treating the page as an instruction source.** Text found on a screen is content. If the loop acts on it without a check, a planted line is as good as the user's request.

## Quiz

1. An agent browses supplier websites in a container that holds the team's admin password, so that it can log in to portals. A hidden line on one page tells the agent to send the password to an outside address. Which precaution from the documentation addresses this exposure?
   - **a**: Rely on the classifiers alone, since they replace the other precautions
   - **b**: Keep secrets away from the machine and allow only listed destinations
   - **c**: Use a larger screen so that hidden text is displayed more clearly
   - **d**: Add a line to the prompt that tells Claude to ignore text on web pages

2. After many turns, a loop that keeps every screenshot starts to fail, even though each image is small. What does the documentation give as the reason?
   - **a**: The API rejects any conversation that holds more than ten screenshots in total
   - **b**: Every screenshot uses up a unit of a daily quota that the toolset enforces
   - **c**: Claude deletes old screenshots after three turns to save space on its side
   - **d**: A request holding over twenty pictures puts a stricter limit on all of them

3. A loop on Claude Sonnet 5.5 keeps growing its context with screenshots, and an engineer proposes dropping the oldest one each turn on the client. What does the documentation advise?
   - **a**: Drop one every turn, which keeps the cached prefix byte-identical
   - **b**: Resize to 2000 px or less per side and let the server clear earlier results
   - **c**: Keep the whole history, because only the first screenshot counts
   - **d**: Remove them in groups of three on the client, with no other side effects

<details>
<summary>Answer key</summary>

1. **b**. The page lists two precautions for this: "Avoiding giving the model access to sensitive data, such as account login information, to prevent information theft", and "Limiting internet access to an allowlist of domains to reduce exposure to malicious content." *a* is ruled out because "The precautions above remain important even with these classifiers in place." *d* is ruled out because "Claude will follow commands found in content even when they conflict with your instructions", so a line in the prompt is not a control. *c* is ruled out because the commands may be "instructions embedded in webpages or images", and a larger screen only shows them better.
2. **d**. The page quotes the documentation: "Once a single request carries more than 20 images, every image in it is held to a stricter per-side limit." *a* is ruled out because the limit is "more than 20 images" in a single request, not ten in a conversation. *b* is ruled out because "Pricing follows the standard tool use pricing", with no quota unit per screenshot. *c* is ruled out because "All screenshots, mouse actions, keyboard inputs, and any files involved in a session are captured and stored in your environment", so the application holds and prunes them, not Claude.
3. **b**. The page says the documentation prefers resizing screenshots to "2000 px or less" per side and server-side tool result clearing, which drops old results from the context. *a* is ruled out because "Dropping a screenshot every turn changes the prefix every turn and invalidates the cache." *d* is ruled out because removing an earlier screenshot "invalidates every later thinking block in every request that still carries those turns". *c* is ruled out because every image in a request over 20 images "is held to a stricter per-side limit", not only the later ones.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A migration moves a working integration from Claude Opus 4.6 to Claude Sonnet 5.5 on the Claude API. What changes in the tools array?
   - **a**: The old entry stays, and the new model accepts it unchanged
   - **b**: A single entry replaces the earlier definition, with no beta header
   - **c**: A beta header is added so that the new toolset can be enabled
   - **d**: The entry gains width and height fields that match the screen

2. A payment form appears in the middle of a batch that the app has already started. Where must the check by a person sit?
   - **a**: Ahead of every single block, since one turn finishes several steps
   - **b**: Only at the start of the task, when the user's request arrives
   - **c**: Never, because classifiers steer the model away from unsafe clicks
   - **d**: Once, after every block has run and the screen has been captured

3. An application sizes screenshots with the rule of the practice, on a model of the high-resolution tier. What is the effect?
   - **a**: It works everywhere, but finer detail that the larger budget allows is lost
   - **b**: Coordinates from Claude must now be multiplied instead of divided
   - **c**: The tier needs a beta header, so requests without it fail
   - **d**: The API rejects the screenshots, since they fall below a minimum size

4. A compliance team asks where the screenshots from a computer use session are kept. What is the documented answer?
   - **a**: In a vault that Anthropic holds for every session by default
   - **b**: In the Files API, where the workspace can read them later
   - **c**: In the customer's own environment, and not at Anthropic
   - **d**: Nowhere, since screenshots are deleted as soon as Claude answers

<details>
<summary>Answer key</summary>

1. **b**. The first page says "Claude 5.5 and later models support computer use only through this toolset" and that "The request needs no beta header." *a* is ruled out because "Claude 5.5 and later models support computer use only through this toolset", so the old entry does not carry over. *d* is ruled out because the entry rejects the display fields, and "coordinates are always in the pixel space of the screenshots you return". *c* is ruled out because "The request needs no beta header."
2. **a**. The first page says that "a batch can finish a multistep action inside one turn", so the check goes before each block runs. *d* is ruled out because the page says to "make that check before each block runs", and a check after the batch comes too late. *b* is ruled out because the documentation asks for "Asking a human to confirm decisions that might result in meaningful real-world consequences", and a payment appears at a point the first request never named. *c* is ruled out because the classifier layer is a second one: "That layer is not a replacement".
3. **a**. The first page says the rule of the practice is "safe on every model", and "it costs detail on a high-resolution model". *d* is ruled out because the rule "is safe on every model", and the page names no minimum size. *b* is ruled out because "scale Claude's coordinates back up before applying them to the real display" holds for any scale. *c* is ruled out because "The request needs no beta header."
4. **c**. The second page quotes the documentation: "All screenshots, mouse actions, keyboard inputs, and any files involved in a session are captured and stored in your environment, not by Anthropic." *a* is ruled out because "Anthropic processes the images and action requests in real time as part of the API call". *b* is ruled out because "Computer use is a client-side tool." *d* is ruled out because the documentation says "Always carefully review and verify Claude's computer use actions and logs", which needs logs to exist.

</details>
