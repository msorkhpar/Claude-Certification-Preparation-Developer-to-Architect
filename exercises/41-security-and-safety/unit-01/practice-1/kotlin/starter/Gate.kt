/** An injection-resistant tool gate. See ../../statement.md. */
class Gate(root: String, allowedHosts: List<String>, allowedEmailDomains: List<String>) {
    private val root = resolve("/", root)
    private val allowedHosts = allowedHosts.map { it.lowercase() }
    private val allowedEmailDomains = allowedEmailDomains.map { it.lowercase() }
    private var tainted = false
    private val records = mutableListOf<Map<String, Any?>>()

    companion object {
        private val tools = setOf("read_file", "write_file", "bash", "fetch", "send_email")
        private val flags = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        private val signals = linkedMapOf(
            "override" to Regex("\\b(ignore|disregard|forget)\\b.{0,40}\\b(previous|prior|above|earlier|system)\\b.{0,20}\\b(instructions?|prompts?|rules)\\b", flags),
            "role-tag" to Regex("<\\s*/?\\s*(system|assistant|tool_result|instructions?)\\s*>", flags),
            "exfiltrate" to Regex("\\b(send|email|forward|post|upload)\\b.{0,60}\\b(to|at)\\b.{0,40}[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+", flags),
            "reveal" to Regex("\\b(reveal|print|show|repeat)\\b.{0,40}\\b(system prompt|password|secret|api key)\\b", flags),
        )
        private val shellTricks = listOf(";", "&", "|", ">", "<", "`", "$(", "\n")
        private val url = Regex("([A-Za-z][A-Za-z0-9+.-]*)://([^/?#]*)([^?#]*)(\\?[^#]*)?(#.*)?", RegexOption.DOT_MATCHES_ALL)

        /** The names of the injection signals found in the text, in the fixed order override, role-tag, exfiltrate, reveal. */
        fun screen(text: String): List<String> = signals.filter { it.value.containsMatchIn(text) }.keys.toList()

        /** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
        fun wrapUntrusted(toolUseId: String, source: String, content: String): Map<String, Any?> {
            // TODO 1 of 6 (unlocks m1 and e1): wrap untrusted text as a tool_result block.
            // Receives the tool use id, where the text came from and the text. Returns a tool_result map. Clean text becomes one line of
            // compact JSON (Json.stringify) with the keys source, trust ("untrusted") and content, in that order. When `screen` finds a
            // signal, return the error block from the statement instead and leave the text out of it.
            // Example: wrapUntrusted("t1", "web page", "hi")
            //   -> {type=tool_result, tool_use_id=t1, content={"source":"web page","trust":"untrusted","content":"hi"}}
            return linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to content)
        }

        private fun luhn(digits: String): Boolean {
            var total = 0
            for ((i, ch) in digits.reversed().withIndex()) {
                var d = ch - '0'
                if (i % 2 == 1) d = if (d * 2 > 9) d * 2 - 9 else d * 2
                total += d
            }
            return total % 10 == 0
        }

        /** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
        fun redact(input: String): String {
            var text = input.replace(Regex("sk-ant-[A-Za-z0-9_-]{8,}"), "[SECRET]")
            text = text.replace(Regex("\\bAKIA[0-9A-Z]{16}\\b"), "[SECRET]")
            text = text.replace(Regex("Bearer [A-Za-z0-9._-]{16,}"), "Bearer [SECRET]")
            text = text.replace(Regex("[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+"), "[EMAIL]")
            return text.replace(Regex("\\b(?:[0-9][ -]?){12,18}[0-9]\\b")) { m ->
                val digits = m.value.replace(Regex("[ -]"), "")
                if (isCard(digits)) "[CARD]" else m.value
            }
        }

        private fun resolve(root: String, path: String): String {
            val p = path.replace('\\', '/')
            val stack = mutableListOf<String>()
            for (part in (if (p.startsWith("/")) p else "$root/$p").split("/")) {
                if (part.isEmpty() || part == ".") continue
                if (part == "..") {
                    if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                } else stack.add(part)
            }
            return "/" + stack.joinToString("/")
        }

        private fun isSecret(path: String): Boolean {
            // TODO 2 of 6 (unlocks e2): does this path name a secret file?
            // Receives a path and returns true for a file named `.env`, a file starting `.env.` unless it is `.env.example`, any file inside
            // a folder called `secrets`, and a name ending in `.pem` or `.key`; false otherwise. Backslashes count as `/`.
            // Example: isSecret("app/.env.local") -> true, isSecret(".env.example") -> false
            return false
        }

        private fun commandAllowed(words: List<String>): Boolean {
            // TODO 3 of 6 (finish this to pass e3): is the first word of a bash command on the allow-list?
            // Receives the command split into words. Returns true for `ls`, `cat`, `pytest`, and `git` followed by `status`, `diff` or
            // `log`; false for anything else, including an empty list. (The checks for sudo, rm, chaining and secret arguments are written.)
            // Example: commandAllowed(listOf("git", "log", "--oneline")) -> true, commandAllowed(listOf("git", "push")) -> false
            return false
        }

        private fun isCard(digits: String): Boolean {
            // TODO 4 of 6 (finish this to pass e6): is this run of digits a card number?
            // Receives the digits with spaces and hyphens already removed. Returns true when there are 13 to 19 of them and `luhn` accepts
            // them (a long number that fails the check is not a card). Example: isCard("4111111111111111") -> true, isCard("4111111111111112") -> false
            return false
        }

        private fun result(decision: String, reason: String = "ok"): Map<String, Any?> = linkedMapOf("decision" to decision, "reason" to reason)

        /** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
        fun hookResponse(decision: Map<String, Any?>): Map<String, Any?> {
            if (decision["decision"] == "allow") return linkedMapOf("exit_code" to 0, "stdout" to "", "stderr" to "")
            val out = linkedMapOf("hookSpecificOutput" to linkedMapOf("hookEventName" to "PreToolUse", "permissionDecision" to decision["decision"], "permissionDecisionReason" to decision["reason"]))
            return linkedMapOf("exit_code" to 0, "stdout" to Json.stringify(out), "stderr" to "")
        }
    }

    val isTainted: Boolean get() = tainted

    fun audit(): List<Map<String, Any?>> = records.toList()

    fun markUntrusted(source: String) {
        tainted = true
    }

    private fun inside(path: String) = path == root || path.startsWith("$root/")

    fun decide(actor: String, tool: String, args: Map<String, Any?>): Map<String, Any?> {
        val r = choose(tool, args)
        val clean = args.mapValues { (_, v) -> if (v is String) redact(v) else v }
        records.add(linkedMapOf("actor" to actor, "tool" to tool, "decision" to r["decision"], "reason" to r["reason"], "args" to clean))
        return r
    }

    private fun choose(tool: String, args: Map<String, Any?>): Map<String, Any?> {
        if (tool !in tools) return result("deny", "unknown tool")
        if (tool == "read_file" || tool == "write_file") {
            val path = resolve(root, (args["path"] ?: "").toString())
            if (!inside(path)) return result("deny", "outside the project")
            if (isSecret(path)) return result("deny", "secret file")
            if (tool == "write_file") {
                val segments = path.split("/")
                if (".git" in segments || ".claude" in segments) return result("deny", "protected path")
                if (tainted) return result("ask", "untrusted content in this session")
            }
            return result("allow")
        }
        if (tool == "bash") return bash((args["command"] ?: "").toString())
        if (tool == "fetch") return fetch((args["url"] ?: "").toString())
        return email(args)
    }

    private fun bash(command: String): Map<String, Any?> {
        val words = command.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.any { it.substringAfterLast('/') == "sudo" || it.substringAfterLast('/') == "rm" }) return result("deny", "dangerous command")
        if (shellTricks.any { it in command }) return result("deny", "chaining or redirection")
        if (!commandAllowed(words)) return result("deny", "command not allowed")
        if (words.drop(1).any { !it.startsWith("-") && isSecret(it) }) return result("deny", "secret file")
        if (words[0] == "pytest" && tainted) return result("ask", "untrusted content in this session")
        return result("allow")
    }

    private fun fetch(address: String): Map<String, Any?> {
        val m = url.matchEntire(address)
        if (m == null || m.groupValues[1].lowercase() != "https") return result("deny", "https only")
        if ("@" in m.groupValues[2]) return result("deny", "credentials in the URL")
        val host = m.groupValues[2].lowercase().replace(Regex(":[0-9]+$"), "")
        if (allowedHosts.none { host == it || host.endsWith(".$it") }) return result("deny", "host not allowed")
        if (tainted && (m.groups[4] != null || m.groups[5] != null)) return result("ask", "data could leave in the URL")
        return result("allow")
    }

    private fun email(args: Map<String, Any?>): Map<String, Any?> {
        val to = (args["to"] ?: "").toString()
        val domain = if ("@" in to) to.substringAfterLast('@').lowercase() else ""
        val text = "${args["subject"] ?: ""}\n${args["body"] ?: ""}"
        return emailDecision(domain, text)
    }

    private fun emailDecision(domain: String, text: String): Map<String, Any?> {
        // TODO 5 of 6 (finish this to pass e4 and e5): decide a send_email call.
        // Receives the recipient's lower-case domain and the subject and body joined by a newline. Returns result(decision, reason):
        // deny "recipient not allowed" when the domain is not in allowedEmailDomains; deny "sensitive data in the body" when redact(text)
        // differs from the text; deny "a person must send it" when the session is tainted (the `tainted` field); else allow.
        // Example: a clean mail to "example.com" in a tainted session -> {decision=deny, reason=a person must send it}
        return result("allow")
    }

    fun alerts(): List<Map<String, Any?>> {
        // TODO 6 of 6 (finish this to pass e7): actors with three or more denials.
        // Reads `records` (maps with "actor" and "decision"). Returns one linkedMapOf("actor" to ..., "denials" to ...) per actor with three or
        // more "deny" records (an "ask" is not a denial), with that actor's current count, ordered by when each reached three.
        // Example: alice denied 3 times, then bob 3 times, then alice once more -> [{actor=alice, denials=4}, {actor=bob, denials=3}]
        return emptyList()
    }
}
