# Least privilege, blocking hooks, sandboxes and secrets

**Level:** Developer · **Module 41:** Security and safety · **Page 2 of 3**
**Exams:** DV6; A2.3

**After this page you can** apply least privilege to an agent's tools, data and credentials, write a hook that blocks a destructive action in every spelling, say what the Claude Code sandbox covers and what runs outside it, keep secrets out of the model's reach in development and in production, and reduce what personal data an agent sees and logs.

Checked on 2026-10-03 against the Claude API documentation page on mitigating jailbreaks and prompt injections, the Claude Code security, permissions, hooks, sandboxing and settings pages, and the Managed Agents pages on vaults and environments. Nothing here was run against a live service. The practice on page 3 tests a gate that applies these ideas, offline.

## Why it matters

Page 1 reduces the chance that an injection works. This page limits what it can do when it does. The documentation states the principle in one sentence: "Apply the principle of least privilege so that a successful injection can do minimal damage: don't give Claude access to secrets it doesn't need, run tools in sandboxed environments, and scope permissions as narrowly as possible." The exam asks which control sits at which layer.

## The idea

### Least privilege

For an agent, privilege has three dimensions.

| Dimension | The question | A narrow answer |
|---|---|---|
| Tools | Which tools can the agent call? | A reviewer gets `Read`, `Grep` and `Glob` and no shell |
| Data | Which files, records and secrets can a call reach? | The project folder and not the home directory; no `.env` |
| Actions | What can a call change or send? | Edits ask first; a network call goes only to listed hosts; mail goes only to listed domains |

The rule that follows from a successful injection is simple. Whatever the injected text asks for, the agent can do only what its grants allow. An agent that can read a file, send mail to anyone and has the production key can leak. The same agent with no mail tool and a read-only grant cannot, whatever it was told. So the question to ask of each grant is not "will it be used well" but "what does a hostile instruction get if it uses this".

Claude Code applies the idea by default. In Manual mode it "starts with read-only permissions", and "When it needs to edit files, run tests, or execute commands, it asks you first." Commands that fetch from the web, such as `curl` and `wget`, "are not auto-approved by default", and "unmatched commands require approval by default", which is fail-closed matching. You tighten from there with deny rules, which are checked before everything else, and with managed settings, where `permissions.disableBypassPermissionsMode` and `permissions.disableAutoMode` can be set to `"disable"` so that a developer cannot switch them on.

### A session that has read untrusted text

Least privilege can change during a session. Once an agent has read a web page, a mail or a file from an unknown source, the session is tainted: its next action may be steered by that text. A practical design gives a tainted session less freedom. Writes ask for approval. A network call that carries data in its URL, a query string or a fragment asks, because the URL is a channel out. Sending mail is refused until a person does it. Reads stay allowed, since they change nothing. This taint gate is the course's own design, built from the documented advice, and it is not a product feature. The practice implements it.

### Hooks that block destructive actions

A memory line asks, and a hook enforces. The hooks guide describes the use: "Prevent Claude from modifying sensitive files like `.env`, `package-lock.json`, or anything in `.git/`. Claude receives feedback explaining why the edit was blocked, so it can adjust its approach." Module 39 gave the mechanics. Exit 2 blocks and writes the reason to standard error, and exit 0 with a JSON `permissionDecision` of `deny` also blocks and carries the reason.

For security the details of the match decide the outcome. A deny rule `Bash(git push *)` matches the text as written, so `git -C . push`, a push behind `&&` or inside `sh -c` is a different string. A hook can parse the command into its parts, drop environment assignments, take the program's base name and look at the real subcommand. It should also fail closed: a command that cannot be parsed, or an event the hook cannot read, is blocked and not waved through. And it should answer the safe case with no opinion and not with an allow, so that it never overrides a stricter rule elsewhere. Remember who runs it: "the most restrictive answer applies", so adding a guard hook only ever adds restriction.

### The sandbox

Permission rules decide whether an action may run. A sandbox decides what a running action can reach. "The Bash sandbox is a boundary that the operating system enforces around the shell commands Claude runs on your machine." You set which files and network domains those commands can reach, and the limits cover the processes they start. Network access goes "through a proxy on your machine that checks each host against your allowed domains, which start empty."

Know what it does not cover. "The sandbox covers shell commands only. Claude's file tools, MCP servers, and hooks run outside it." The built-in file and web tools "follow permission rules instead", so a `denyRead` entry in the sandbox "doesn't stop the Read tool", and `allowedDomains` "doesn't limit WebFetch". So the sandbox and the permission rules cover different surfaces, and a design needs both. Commands you type yourself at the shell prompt, excluded commands and unsandboxed retries run outside it as well. On native Windows the sandbox is not available and Claude Code runs commands unsandboxed, so use WSL2. In the auto-allow sandbox mode a command that runs inside the sandbox is approved automatically, which is the convenience that pays for the boundary. The regular-permissions mode keeps the prompts.

For hosted agents the same ideas appear as environment settings. Module 36 covered `limited` networking with explicit hosts and the point that an omitted setting gets `unrestricted`. The documentation adds the advice to prefer a narrow host list, and to put `bash` on `always_ask` or `auto` when the agent reads untrusted input.

### Secrets in development and in production

The rule is that a secret never enters anything the model can read or a file anyone can commit.

- **In development**, keys live in the environment, loaded from an untracked file such as `.env`, and the file is on the project's deny list for reads and edits. Claude Code stores its own credentials in "the macOS Keychain when available", and on Linux "in a file with mode `0600`". Test fixtures use a placeholder, never a real key.
- **In CI**, keys live in the platform's secret store and are referenced and not written: "Never commit API keys or OAuth tokens directly to your repository." Where possible avoid a long-lived key by exchanging a short-lived identity token, as workload identity federation does.
- **In production agents**, keep the value out of the sandbox. Managed Agents vaults store a credential as "an opaque placeholder" in the sandbox and substitute the real value when the agent makes an outbound request, so "The agent never sees the secret value." Self-hosted setups own this duty themselves: the environment key goes in a secrets manager and never in an image.
- **In logs and results**, redact before writing. A key that appears in a tool result or an audit line has left your control.

### Personal data

Personal data (PII) is a leak path of its own. Three habits cover most of it. Collect less: if the task does not need the field, do not put it in the prompt. Redact on the way out: before a result is logged or an email is sent, replace keys, addresses and card numbers with fixed markers, and verify card numbers with the Luhn check so that an ordinary long number is not hidden. And do not let a tainted session send data to an address the person did not choose. The practice's `redact` function does the first two for logs and mail bodies, and its mail rule does the third. These are the course's design choices. No documentation page here prescribes them.

## Traps

1. **Trusting a prefix rule to stop a destructive command.** `Bash(git push *)` does not match `git -C . push`. Parse the command in a hook.
2. **Assuming the sandbox covers every tool.** It covers shell commands. File tools and web tools follow permission rules.
3. **Letting a hook fail open.** A guard that exits 0 on a parse error is a guard an attacker can walk through.
4. **Giving a read-only job the production key.** Whatever the injected text asks for, the key lets the job do it.

## Quiz

1. An agent reads web pages and can send mail to any address. Which change best reduces what an injected instruction can do?
   - **a**: Add a sentence to the system prompt that forbids sending any mail
   - **b**: Limit the tool to a short list of approved recipient domains
   - **c**: Screen each page with a pattern list before the agent sees the text
   - **d**: Ask the model to explain why it wants to send a message at all

2. A deny entry blocks `Bash(git push *)`, and the agent runs `git -C . push origin main`. What happens?
   - **a**: The call is approved, because rules never apply to a command that has options
   - **b**: The rule matches, because git ignores the options before the subcommand
   - **c**: The sandbox blocks it, because the network is closed to shell commands
   - **d**: The rule misses it, since the typed text differs from the pattern

3. A team turns on the Bash isolation boundary with an empty domain list and assumes the WebFetch tool is blocked too. Why is that wrong?
   - **a**: It governs network calls only, while shell commands run outside it
   - **b**: It governs shell commands only, while built-in web access follows permission rules
   - **c**: It governs every tool alike, so the fetch is blocked by the empty list
   - **d**: It governs hooks only, while shell commands follow permission rules

<details>
<summary>Answer key</summary>

1. **b**. The table gives the narrow answer for actions: mail goes "only to listed domains", so a hostile instruction gets at most those recipients. *a* is ruled out because "Whatever the injected text asks for, the agent can do only what its grants allow", and a sentence in a prompt is not a grant. *c* is ruled out because the grants stay the same after a screen, and the page says to "run tools in sandboxed environments, and scope permissions as narrowly as possible". *d* is ruled out because an explanation from the model changes no grant, and the page says "don't give Claude access to secrets it doesn't need".
2. **d**. The page says a deny rule "matches the text as written", so `git -C . push` is a different string and the rule misses it, and "A hook can parse the command into its parts". *b* is ruled out because the match is on text as written, so "A hook can parse the command into its parts" is the page's remedy and git's own option handling is not what the rule sees. *c* is ruled out because "Permission rules decide whether an action may run. A sandbox decides what a running action can reach", and the sandbox does not replace the rule. *a* is ruled out because the page says "a push behind `&&` or inside `sh -c` is a different string", so a rule written for one spelling does not cover the other, and a rule is not exempt for a command with options.
3. **b**. The page says "The sandbox covers shell commands only", and that the built-in file and web tools "follow permission rules instead", so `allowedDomains` "doesn't limit WebFetch". *a* is ruled out because the sandbox is "a boundary that the operating system enforces around the shell commands", so shell commands are restricted and not free. *c* is ruled out because "The sandbox covers shell commands only", and the web tools are not among them. *d* is ruled out because "Claude's file tools, MCP servers, and hooks run outside it".

</details>
