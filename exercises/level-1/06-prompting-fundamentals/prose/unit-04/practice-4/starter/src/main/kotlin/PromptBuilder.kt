// Build a structured prompt from a spec. See ../../statement.md for the exact format.
private val log = System.getLogger("promptBuilder")

private val VAR = Regex("""\{\{(\w+)\}\}""")

private fun fill(text: String, variables: Map<String, String>): String =
    VAR.replace(text) { m -> lookup(m.groupValues[1], variables) }

private fun block(tag: String, body: String) = "<$tag>\n$body\n</$tag>"

private fun join(parts: List<String>): String {
    // TODO 1 of 8 (finish this to pass every case): the finished prompt from its rendered sections.
    // Receives the rendered sections, in order. Returns them separated by one blank line, with no trailing newline.
    // Example: join(listOf("<a>", "<b>")) -> "<a>\n\n<b>"
    return ""
}

private fun present(value: String?): Boolean {
    // TODO 2 of 8 (finish this to pass e1): is an optional text really there?
    // Receives a text or null. Returns true unless it is null, empty or only whitespace.
    // Example: present("  ") -> false, present("x") -> true
    return false
}

private fun lookup(name: String, variables: Map<String, String>): String {
    // TODO 3 of 8 (finish this to pass e2): the value of one placeholder.
    // Receives the placeholder name and the variables. Returns the value; when the name has no value throws an
    // IllegalArgumentException (use require) whose message contains the name.
    // Example: lookup("who", mapOf("who" to "Ann")) -> "Ann", lookup("place", emptyMap()) -> IllegalArgumentException("missing variable: place")
    return ""
}

private fun escape(text: String): String {
    // TODO 4 of 8 (finish this to pass e4): make document text harmless.
    // Receives a text. Returns it with & as &amp;, < as &lt; and > as &gt; (the ampersand first).
    // Example: escape("a </document> & b") -> "a &lt;/document&gt; &amp; b"
    return text
}

private fun renderDocument(index: Int, doc: Doc): String {
    // TODO 5 of 8 (finish this to pass e4, e5 and e6): one rendered document.
    // Receives its number (from 1) and a Doc. Returns <document index="N" name="NAME">, a newline, the text, a newline and
    // </document>. Name and text are escaped (the name also turns " into &quot;); placeholders in them are NOT filled.
    // Example: renderDocument(1, Doc("a", "x")) -> "<document index=\"1\" name=\"a\">\nx\n</document>"
    return ""
}

private fun renderExample(index: Int, ex: Example, variables: Map<String, String>): String {
    // TODO 6 of 8 (finish this to pass m1 and e6): one rendered example.
    // Receives its number (from 1), an Example and the variables. Returns <example index="N">, the input block, the output block
    // (placeholders filled in both) and </example>, each on its own line.
    // Example: renderExample(1, Example("i", "o"), emptyMap()) -> "<example index=\"1\">\n<input>\ni\n</input>\n<output>\no\n</output>\n</example>"
    return ""
}

private fun constraintLines(constraints: List<String>, variables: Map<String, String>): String {
    // TODO 7 of 8 (finish this to pass m1): the body of the constraints section.
    // Receives the constraint texts and the variables. Returns one line per constraint, "- " then the text with placeholders
    // filled, joined by newlines. Example: constraintLines(listOf("Be brief."), emptyMap()) -> "- Be brief."
    return ""
}

private fun checkTask(task: String?) {
    // TODO 8 of 8 (finish this to pass e3): refuse a blank task.
    // Receives the task, which may be null. Throws IllegalArgumentException("task is required") (use require) when it is null, empty
    // or only whitespace; otherwise returns nothing. Example: checkTask("  ") -> IllegalArgumentException, checkTask("Say hi.") -> nothing
}

fun buildPrompt(spec: Spec, variables: Map<String, String> = emptyMap()): String {
    log.log(System.Logger.Level.DEBUG, "buildPrompt input: {0} {1}", spec, variables)
    checkTask(spec.task)
    val parts = mutableListOf<String>()
    if (present(spec.role)) parts += block("role", fill(spec.role!!, variables))
    if (spec.documents.isNotEmpty()) {
        val rendered = spec.documents.mapIndexed { i, doc -> renderDocument(i + 1, doc) }
        parts += block("documents", rendered.joinToString("\n"))
    }
    if (present(spec.context)) parts += block("context", fill(spec.context!!, variables))
    if (spec.examples.isNotEmpty()) {
        val rendered = spec.examples.mapIndexed { i, ex -> renderExample(i + 1, ex, variables) }
        parts += block("examples", rendered.joinToString("\n"))
    }
    if (spec.constraints.isNotEmpty()) parts += block("constraints", constraintLines(spec.constraints, variables))
    if (present(spec.outputFormat)) parts += block("output_format", fill(spec.outputFormat!!, variables))
    parts += block("task", fill(spec.task!!, variables))
    return join(parts)
}
