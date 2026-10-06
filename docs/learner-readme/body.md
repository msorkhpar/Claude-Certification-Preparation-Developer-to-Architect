### The four levels and the exams they serve

One path, four levels, each building on the one before, so nothing you study for one exam has to be
learned again for the next. The exams are Anthropic's own certifications; the course maps its modules to
their published topic lists and writes everything in its own words.

| Level | Modules | Serves | What it teaches |
|---|---|---|---|
| 1 Foundations | 1 to 11 | Claude Certified Associate (CCAO-F), and the base of every other exam | How a language model works, Claude's models and apps, prompting, working responsibly, safety and policy |
| 2 Developer | 12 to 44 | Claude Certified Developer (CCDV-F) | The Messages API, errors and retries, streaming, caching, batches, cloud platforms, tool use, retrieval, context, vision, computer use, MCP, agents and the Agent SDK, Claude Code, security and evaluation |
| 3 Architect | 45 to 78 | Claude Certified Architect, Foundations (CCAR-F) | The agentic loop, coordinators and subagents, hooks, session state, tool and MCP design, Claude Code configuration, structured output, long-context handling, escalation, and eight worked scenarios |
| 4 Architect Professional | 79 to 94 | Claude Certified Architect, Professional (CCAR-P) | From business problem to solution, multi-agent reliability, deployment and data handling, cost, observability, evaluation, migration, governance, and a capstone |

Each level ends with a revision module that holds exam-style mock exams. The page
[`docs/EXAM-MAP.md`](docs/EXAM-MAP.md) says which module serves which exam topic and which facts were read on an official page; [`docs/VERSIONS.md`](docs/VERSIONS.md) lists the model ids and SDK versions the pages were checked on.

### What you can do with it

- **Read** every lesson in a browser, with the contents, an outline of the page and links to the previous and next page.
- **Pick a reading language**: examples and practices come in Python, TypeScript, Java and Kotlin, and a switch at the top of every page changes the language you read in. Where a topic needs the Agent SDK, only the SDK's own languages are offered.
- **Answer quizzes** in the page: a short quiz on each page, a quiz at the end of each module, and each answer is explained whether you were right or wrong.
- **Sit mock exams** in the exams' scenario style at the end of each level, with explanations after you finish.
- **Revise** with flashcards and a review bank of questions for each level.
- **Practise in code.** Each code module has a graded practice. **Submit** runs the practice's tests in a runner and reports whether the main task is done and how many edge cases pass. **Run** only runs a try-it file and shows what your code printed and logged; it grades nothing.
- **Edit in the browser.** The practice opens an editor beside its statement; nothing to install.
- **Search** the whole course from the top bar, and switch between light and dark.
- **Keep your place.** Reading marks and quiz results are kept in your browser; practice results are kept in a Docker volume on your machine.

### Everything graded runs offline, with no API key

You do not need an Anthropic account or an API key for anything the course grades, and no practice sends a request to Claude or to any other service.

- **Agent SDK practices** run the real Agent SDK client code, and hand it [`harness/fake_claude.py`](harness/fake_claude.py) in place of the Claude Code binary: `cli_path` in Python, `pathToClaudeCodeExecutable` in TypeScript. The stand-in speaks the same stream protocol and plays a script of replies and tool calls, so the SDK's own code runs for real while the model is replaced by the script.
- **Messages API practices** use the real `anthropic` SDK with a scripted transport ([`harness/scripted.py`](harness/scripted.py) and [`harness/replay.py`](harness/replay.py) in Python, with equivalents for TypeScript, Java and Kotlin under `harness/`). Your code calls `client.messages.create(...)` unchanged and the request is answered by the script, never by a socket. Some practices instead hand your function a scripted `send`, with the same effect.
- **The runner has no network.** The container that runs your code sits on a Docker network marked `internal`, so even a mistake could not reach the internet.
- **Live runs are not part of the course.** The site has no key field in this build and nothing in it reads `ANTHROPIC_API_KEY`. If you want to try an example against the real API, do it yourself, outside the course, with your own key in your own environment. That is optional, it costs you real tokens, and it is never graded.

Scripted answers are stand-ins, so a practice shows that your code handles the shapes of reply the course describes; it is not a test of how the live model behaves today.

### A look around

The pictures were taken from a fresh copy of the course, with no progress and no edits.

<!--SHOTS-->

### Two ways to use it

- **Run it locally with Docker** for everything above: quizzes, mock exams, Submit and Run, the editor, search and saved progress. See [Run it locally with Docker](#run-it-locally-with-docker), which gives the exact commands.
- **Read the online preview** on GitHub Pages: a read-only copy of the lessons, with the quizzes and your reading marks, and no runner or editor. Anything that needs the local course says so and points back here. See [The online preview](#the-online-preview) for how to switch it on for your own copy.
