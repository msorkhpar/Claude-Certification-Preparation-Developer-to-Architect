# Goal

## The goal

Take an engineer who can program but has never built with Claude, and make them **ready for the
Claude Certified Developer exam, then the Claude Certified Architect exams**, in one path where
each level adds to the one before. Ready means they can answer a scenario question by reasoning
about the system, not by recalling a phrase, because they have built the thing the question is
about.

The questions the course must let a reader answer:

- The agent skipped the customer lookup in one case in ten. Is the fix a better prompt, an
  example, or a check in code, and why?
- Which model, which context strategy and which API (synchronous, batch, cached) does this
  workload need, and what does each choice cost?
- Should these three tools be one tool or three, what does each description say, and what does
  the tool return when it fails?
- Who owns this subagent's context, what does the coordinator pass it, and how does an error in
  it reach the user?
- Where does this rule belong: the user's `CLAUDE.md`, the project's, a path rule, a skill, a
  hook or a permission?
- How is this extraction validated, retried and sampled for human review, and how do we know
  its confidence is honest?

## Who it is for

1. **Developers** starting to build with Claude, who want the Developer certificate.
2. **Architects and senior engineers** who design agentic systems and want the Architect
   certificates.
3. **Teams** adopting Claude Code and the API who want a shared, tested standard of practice.

The course assumes the reader can program in one of its four languages (Python, TypeScript,
Java or Kotlin) and knows HTTP and JSON. It assumes
nothing about Claude.

## Non-goals (the scope guard)

- Not a copy of any exam, exam guide or official course. Exam blueprints are used only as a map
  of topics, written in the course's own words; questions are written fresh.
- No claim that the course is official, endorsed or sufficient for a pass.
- Not a machine-learning research course: how models are made (pre-training, fine-tuning,
  tokenizers, architecture) is taught as concepts, with no training practice.
- Not a cloud administration course: Bedrock and Vertex access, identity, quotas and regions are
  taught as configuration to read and check, in graded config practices, not as console tours.
- No live API call in a graded practice. Practices run offline; live runs are optional and use
  the reader's own key from the environment.

## How we know it worked

- A reader can run every example and practice with `docker compose up` and a browser, with no
  API key and no network.
- Every example's recorded exchange is a real exchange, captured once against the pinned model
  and replayed; the page names the model and SDK versions it was captured on.
- Practices are graded on the main ask and on edge cases by a real runner.
- Every topic of each exam's blueprint has a home in the outline (`EXAM-MAP.md`), and every
  page states which exam domains it serves.
- An engineer who has passed an exam, reviewing the course, finds no claim that is wrong for the
  pinned model and SDK versions.

## Constraints that shape everything

- **No network at run time** for practices. Dependencies are vendored and warmed.
- **The licence decides how a source is used.** Material whose licence permits copying is
  credited on the page where it is used; everything else is read for understanding and written
  fresh.
- **Claude moves fast.** Model names, limits and features are pinned per page, and a release
  pass re-checks them before each publish.
- **Nothing is published without the owner's review.** Pushing is the owner's act.
