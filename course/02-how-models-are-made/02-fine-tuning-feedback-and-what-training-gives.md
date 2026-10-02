# Fine-tuning, feedback training and what training gives

**Level:** Foundations · **Module 2:** How models are made · **Page 2 of 2**
**Exams:** X (beyond the exam blueprints; the ideas behind AS3, AS6 and DV4 questions)

**After this page you can** explain how a pretrained model becomes an assistant, name the transformer and
attention at the level of an idea, and list what training does and does not give a model.

Checked against the Anthropic glossary on 2026-10-02. Concepts only: no training practice, and no claim
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

### Learning from written principles (constitutional training)

Preference data from humans is expensive and hard to scale, and it is hard to inspect. Anthropic's
constitutional approach adds a written set of principles, a "constitution", that the model is trained to
apply: it critiques and revises its own answers against the principles, and feedback derived from that
process is used as a training signal in addition to human feedback. The idea to keep for the exams is
the effect: behaviour is shaped by stated principles that people can read, not only by thousands of
unexplained rankings. The glossary describes the framework behind this as HHH, **helpful, honest,
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
   currently offer fine-tuning; the standard route is to put the knowledge in the context (module 28 covers
   retrieval).
3. **Assuming a refusal or a style is a prompt bug.** Some behaviours come from training. A prompt can
   steer within them, but an instruction that fights the training may not win.

## Quiz

1. A stakeholder asks why the assistant answers questions helpfully, when a raw pretrained model would
   often just continue the text. Which stage does the most to explain the difference?
   - **a**: A larger vocabulary in the tokenizer
   - **b**: Further training on curated examples and ranked human preferences
   - **c**: A wider context window at inference time
   - **d**: Lowering the sampler's randomness setting

2. A company wants Claude to answer from its product manual, which changes monthly. A colleague proposes
   fine-tuning the model on each new manual. What is the best response?
   - **a**: Agree, because only weights can store product facts
   - **b**: Ask the model to memorise the manual in chat
   - **c**: Agree, but only after lowering the temperature
   - **d**: Place the latest version in the prompt instead

<details>
<summary>Answer key</summary>

1. **b**. Fine-tuning and feedback training turn a text continuer into an assistant (the stages section). *a* is ruled out because a tokenizer only cuts text; it does not teach instruction following. *c* is ruled out because window size is a capacity, and a raw model with a big window still continues text. *d* is ruled out because the sampler picks among the model's options and does not create the helpful behaviour.
2. **d**. Weights are changed by training, a separate act that the API does not currently offer, and knowledge that changes monthly belongs in the context (the trap on fine-tuning and the table). *a* is ruled out because the table shows knowledge can live in the window too. *c* is ruled out because temperature is irrelevant to where facts are stored. *b* is ruled out because a conversation does not change the weights, so nothing is memorised for later.

</details>

## Module quiz

This quiz covers both pages of the module.

1. An analyst complains that Claude "forgot" the glossary they taught it last week in a separate
   session. Which explanation fits how models are made?
   - **a**: Weights stay fixed at inference, so each conversation starts fresh
   - **b**: The tokenizer drops terms that were unfamiliar the first time
   - **c**: Attention discards any term older than seven days
   - **d**: Constitutional training deletes user vocabulary overnight

2. A localisation lead notices that the same paragraph needs noticeably more tokens in one language than
   in another. What is the most likely reason?
   - **a**: The two languages were assigned different context windows
   - **b**: The model refuses one language in order to save money
   - **c**: The tokenizer's learned pieces match one writing system better
   - **d**: RLHF raises the price of less common languages

3. A teammate says a model's answer must be right because it was trained to be honest. Which reply is
   accurate?
   - **a**: Honesty training makes every figure verified
   - **b**: That is a goal of training; check important claims anyway
   - **c**: Honesty training only applies to harmful topics
   - **d**: Honesty training is replaced by the system prompt

4. Which task is the poorest fit for a model working alone, given how tokenizers and training work?
   - **a**: Summarising a meeting transcript in three bullets
   - **b**: Explaining a stack trace in plain language
   - **c**: Suggesting five names for a new internal tool
   - **d**: Reporting the exact number of characters in a string

<details>
<summary>Answer key</summary>

1. **a**. Weights do not change while you chat, so a new conversation has no trace of the old one (page 1, weights section). *b* is ruled out because tokenizers cut text and store nothing about a user. *c* is ruled out because attention weighs positions within the current context only, with no clock. *d* is ruled out because constitutional training happens before release and holds no user vocabulary.
2. **c**. Learned sub-word pieces were built from a corpus, so some languages split into more pieces (page 1, "what the tokenizer does to your work"). *a* is ruled out because the window is a model property, not set per language. *b* is ruled out because nothing on the pages describes any such refusal. *d* is ruled out because feedback training shapes behaviour and does not set token counts.
3. **b**. The glossary frames HHH as a training framework and goal, and the traps say verification is still needed. *a* is ruled out because nothing in training verifies figures. *c* is ruled out because the framework covers accuracy as well as harm. *d* is ruled out because a system prompt steers behaviour at run time and does not replace training.
4. **d**. Exact character counts are a tokenizer-level weakness, so code should do them (page 1). *a*, *c* and *b* are language tasks that play to what pretraining and fine-tuning give, as the table shows.

</details>
