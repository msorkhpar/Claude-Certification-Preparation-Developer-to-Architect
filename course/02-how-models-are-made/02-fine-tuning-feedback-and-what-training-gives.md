# Fine-tuning, feedback training and what training gives

**Level:** Foundations · **Module 2:** How models are made · **Page 2 of 2**
**Exams:** X (beyond the exam blueprints; the ideas behind AS3, AS6 and DV4 questions)

**After this page you can** explain how a pretrained model becomes an assistant, name the transformer and
attention at the level of an idea, and list what training does and does not give a model.

Checked against the Anthropic glossary, the Constitutional AI research page and the constitution pages on 2026-10-02. Concepts only: no training practice, and no claim
about the internals of a specific Claude model.

## Why it matters

When a model refuses a request, apologises too much, or follows an instruction it was never given, the
explanation is usually in how it was trained to behave, not in the prompt. Knowing the stages lets you tell
a behaviour you can change with a prompt from one you cannot, and lets you answer exam questions that ask
what a model "knows", "remembers" or "was trained to do" without guessing.

## The idea

### The stages, in order

1. **Pretraining** (page 1): learn to continue text on a very large corpus. The result is capable and
   unruly.
2. **Fine-tuning:** "further training a pretrained language model using additional data. This causes the
   model to start representing and mimicking the patterns and characteristics of the fine-tuning dataset."
   The glossary adds: "Claude is not a bare language model; it has already been fine-tuned to be a helpful
   assistant."
3. **Feedback training.** Two ideas are worth knowing.

Source: Glossary, Claude API documentation.

### Learning from human preferences (RLHF)

Reinforcement learning from human feedback is described in the glossary as a technique "used to train a
pretrained language model to behave in ways that are consistent with human preferences". Humans rank two or
more example answers; "the reinforcement learning process encourages the model to prefer outputs that are
similar to the higher-ranked ones." This is how helpfulness and instruction following get layered on top
of raw text continuation.

### Learning from written principles (Constitutional AI)

Preference data from humans is expensive, hard to scale and hard to inspect. Anthropic's research on
**Constitutional AI** replaces much of it with a short written list of principles, a "constitution", and
trains in two phases:

1. **A supervised phase, critique and revision.** "We sample from an initial model, then generate
   self-critiques and revisions, and then finetune the original model on revised responses." The model
   critiques its own answers against the principles and rewrites them; the revised answers become
   training data.
2. **A reinforcement learning phase, from AI feedback (RLAIF).** The refined model produces pairs of
   answers, "a model" judges which of the two is better under the principles, and a **preference model**
   is trained from this dataset of AI preferences. That preference model then supplies the reward signal
   for reinforcement learning, the role human rankings play in RLHF.

The research page states the consequence for harmlessness: no human labels are needed for it, and "the
only human oversight is provided through a list of rules or principles."

Source: Constitutional AI: Harmlessness from AI Feedback, Anthropic research.

The principles are public and have been rewritten. Anthropic published a new version of Claude's
constitution on 2026-01-21, a detailed description of its intentions for Claude's values and behaviour.
It prefers judgment to rigid rules: "We generally favor cultivating good values and judgment over strict
rules and decision procedures", and "we try to explain any rules we do want Claude to follow", so that
Claude can understand the reasoning behind a rule rather than only obey it.

Sources: Claude's constitution, and the 2026-01-21 update note on Anthropic's constitution news page.

The idea to keep for the exams is the effect: behaviour is shaped by stated principles that people can
read, not only by thousands of unexplained rankings. The glossary describes the framework behind this as HHH, **helpful, honest,
harmless**: a helpful AI tries to do the task well; an honest one gives accurate information and
acknowledges "its limitations and uncertainties"; a harmless one declines to assist with dangerous or
unethical acts and says why. The glossary calls HHH "a research framework that informs how Claude is trained",
a goal and a direction, not a certificate on any single reply.

### The transformer and attention, as an idea

Almost every modern language model is built on the **transformer** architecture. The one idea worth carrying
is **attention**: when the model decides what token comes next, every position in the context can look at
every earlier position and weigh how relevant each one is. That is how a pronoun finds its noun and how a
question finds the sentence that answers it.

Two practical consequences, both used later in the course:

- Attention is a **limited resource spread across the context**. As the context grows, relevant material
  can compete with irrelevant material, which the context windows page names *context rot* ("accuracy and
  recall degrade" as token count grows). This is why where you place information and how much you include
  both matter (modules 6, 29 and 64).
- Everything the model uses is **in the window or in the weights**. There is no third place such as a
  notebook it consults. If it needs a fact you have, put the fact in the window.

### What training gives, and what it does not

| Training gives | Training does not give |
|---|---|
| Fluent language, many languages, code | A guarantee that any statement is true |
| Broad general knowledge to a cut-off date | Knowledge of anything after the cut-off |
| Following instructions and a helpful manner | Access to your files, systems or today's date |
| Tendencies toward honest, harmless replies | A rule that can never be broken |
| Reasoning patterns it can apply to new problems | Perfect arithmetic, counting or spelling |
| A fixed set of weights | Memory of your previous conversations |

Read the right-hand column as the reason for the rest of the course: knowledge you need, tools you need and
guarantees you need all come from what you build around the model.

## Traps

1. **Treating "trained to be honest" as "never wrong".** The glossary frames HHH as how Claude is trained,
   not a promise about each answer. Verification is still the user's job.
2. **Thinking fine-tuning is the default way to add knowledge.** The glossary says the Claude API does not
   currently offer fine-tuning (a statement about the Claude API, not about every platform); the standard route is to put the knowledge in the context (module 28 covers
   retrieval).
3. **Assuming a refusal or a style is a prompt bug.** Some behaviours come from training. A prompt can
   steer within them, but an instruction that fights the training may not win.

## Quiz

1. A stakeholder asks why a deployed assistant answers a question with an answer, when the raw pretrained
   model it started from tends to continue the question with more questions. Which part of how models are
   made does most to explain the gap?
   - **a**: A larger tokenizer vocabulary that cuts text into longer pieces
   - **b**: A wider context window that lets it see more of the exchange
   - **c**: Attention that lets each position weigh every earlier position
   - **d**: Follow-up training on curated examples and human-ranked outputs

2. A company wants Claude to answer from its product manual, which is revised every month, through the
   Claude API. An engineer proposes fine-tuning on each new version. What is the best response?
   - **a**: Supply the current edition in the prompt instead
   - **b**: Proceed, then rely on chat corrections to patch what the tuning missed
   - **c**: Ask Anthropic to retrain monthly, since only weights store facts reliably
   - **d**: Place the manual in the prompt and fine-tune as well, to be safe

<details>
<summary>Answer key</summary>

1. **d**. Fine-tuning and feedback training turn a text continuer into an assistant (the stages section). *a* is ruled out because a tokenizer only cuts text; it does not teach instruction following. *b* is ruled out because window size is a capacity, and the stage list calls the pretrained result capable and unruly however much it can see. *c* is ruled out because attention is the architecture shared by the raw model and the assistant, so it cannot be what separates them.
2. **a**. The glossary says the Claude API does not currently offer fine-tuning, and knowledge that changes monthly belongs in the context (the second trap and the table). *b* is ruled out because the table lists memory of previous conversations as something training does not give, so chat corrections patch nothing. *c* is ruled out because the table shows knowledge can live in the window, and the second trap names the context as the standard route. *d* is ruled out because fine-tuning adds cost and delay when the prompt already carries the facts.

</details>

## Module quiz

This quiz covers both pages of the module.

1. An analyst taught Claude a set of internal abbreviations in a chat last week. In a new chat today it
   ignores them, and the analyst concludes something broke. Which explanation fits?
   - **a**: The tokenizer discarded the unfamiliar abbreviations after a week
   - **b**: Weights stay fixed in use, so the earlier session left nothing behind
   - **c**: Attention drops any term that is older than a few days
   - **d**: The fine-tuning the chat triggered was rolled back overnight

2. A localisation lead notices that one paragraph needs noticeably more tokens in one language than in
   another. Which explanation fits best?
   - **a**: The model gives each language its own context window size
   - **b**: Preference training sets how many pieces each language costs
   - **c**: The learned sub-word pieces cover one script more compactly
   - **d**: Fine-tuning replaced part of the vocabulary for the rarer language

3. A reviewer waves through a number in a report because the assistant is built to be honest. Which reply
   is the soundest?
   - **a**: Honesty goals cover harmful topics only, so numbers are exempt
   - **b**: A system prompt can switch the honesty framework on for a task
   - **c**: Honesty work verifies each figure against its sources before release
   - **d**: That describes a goal, not a guarantee, so key claims still need checking

4. A team is automating four chores. Which one should be done by a deterministic program instead of the
   model alone?
   - **a**: Verifying every product code is exactly eight characters long
   - **b**: Rewriting release notes for a non-technical audience of customers
   - **c**: Proposing likely causes for an intermittent failing test
   - **d**: Sorting incoming emails by urgency from the message text

<details>
<summary>Answer key</summary>

1. **b**. Weights do not change while you chat, so a new conversation has no trace of the old one (page 1, weights section). *a* is ruled out because tokenizers cut text and store nothing about a user. *c* is ruled out because attention weighs positions within the current context only, as the transformer section describes, with no clock. *d* is ruled out because the weights section says a conversation does not train the model, and fine-tuning is a separate act.
2. **c**. Learned sub-word pieces were built from a corpus, so some scripts split into more pieces (page 1, "what the tokenizer does to your work"). *a* is ruled out because the window is a model property, not set per language. *b* is ruled out because the table on this page lists behaviour as what feedback training shapes, not token counts. *d* is ruled out because page 1 says the tokenizer is built before pretraining and fixed afterwards.
3. **d**. The glossary frames HHH as how Claude is trained and the first trap says verification is still the user's job. *a* is ruled out because the honest part of HHH covers giving accurate information and acknowledging limitations. *b* is ruled out because a system prompt steers within trained behaviour and does not switch a training framework on. *c* is ruled out because the table lists a guarantee that any statement is true among the things training does not give.
4. **a**. Exact character work is a tokenizer-level weakness, so code should do it (page 1, first trap). *b* is ruled out because fluent language and style are what pretraining and fine-tuning give, as the table shows. *c* is ruled out because the table lists reasoning patterns applied to new problems as something training gives. *d* is ruled out because following instructions is on the same list, and a model reads message text well.

</details>
