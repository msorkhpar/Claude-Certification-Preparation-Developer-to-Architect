// Build a structured prompt from a spec. See ../../statement.md for the exact format.
private val log = System.getLogger("promptBuilder")

private val VAR = Regex("""\{\{(\w+)\}\}""")

private fun fill(text: String, variables: Map<String, String>): String =
    VAR.replace(text) { m -> lookup(m.groupValues[1], variables) }

private fun block(tag: String, body: String) = "<$tag>\n$body\n</$tag>"

private fun join(parts: List<String>): String = parts.joinToString("\n\n")

private fun present(value: String?) = value != null && value.isNotBlank()

private fun lookup(name: String, variables: Map<String, String>): String {
    require(name in variables) { "missing variable: $name" }
    return variables.getValue(name)
}

private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun renderDocument(index: Int, doc: Doc): String {
    val name = escape(doc.name).replace("\"", "&quot;")
    return "<document index=\"$index\" name=\"$name\">\n${escape(doc.text)}\n</document>"
}

private fun renderExample(index: Int, ex: Example, variables: Map<String, String>): String =
    "<example index=\"$index\">\n" +
        block("input", fill(ex.input, variables)) + "\n" +
        block("output", fill(ex.output, variables)) + "\n</example>"

private fun constraintLines(constraints: List<String>, variables: Map<String, String>): String =
    constraints.joinToString("\n") { "- " + fill(it, variables) }

private fun checkTask(task: String?) {
    require(present(task)) { "task is required" }
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
