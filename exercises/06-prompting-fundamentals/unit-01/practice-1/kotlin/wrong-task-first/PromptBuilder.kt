// Build a structured prompt from a spec. Reference solution.
private val VAR = Regex("""\{\{(\w+)\}\}""")

private fun fill(text: String, variables: Map<String, String>): String =
    VAR.replace(text) { m ->
        val name = m.groupValues[1]
        require(name in variables) { "missing variable: $name" }
        variables.getValue(name)
    }

private fun escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun block(tag: String, body: String) = "<$tag>\n$body\n</$tag>"

private fun present(value: String?) = value != null && value.isNotBlank()

fun buildPrompt(spec: Spec, variables: Map<String, String> = emptyMap()): String {
    require(present(spec.task)) { "task is required" }
    val parts = mutableListOf(block("task", fill(spec.task!!, variables)))
    if (present(spec.role)) parts += block("role", fill(spec.role!!, variables))
    if (spec.documents.isNotEmpty()) {
        val rendered = spec.documents.mapIndexed { i, doc ->
            val name = escape(doc.name).replace("\"", "&quot;")
            "<document index=\"${i + 1}\" name=\"$name\">\n${escape(doc.text)}\n</document>"
        }
        parts += block("documents", rendered.joinToString("\n"))
    }
    if (present(spec.context)) parts += block("context", fill(spec.context!!, variables))
    if (spec.examples.isNotEmpty()) {
        val rendered = spec.examples.mapIndexed { i, ex ->
            "<example index=\"${i + 1}\">\n" +
                block("input", fill(ex.input, variables)) + "\n" +
                block("output", fill(ex.output, variables)) + "\n</example>"
        }
        parts += block("examples", rendered.joinToString("\n"))
    }
    if (spec.constraints.isNotEmpty()) {
        parts += block("constraints", spec.constraints.joinToString("\n") { "- " + fill(it, variables) })
    }
    if (present(spec.outputFormat)) parts += block("output_format", fill(spec.outputFormat!!, variables))
    return parts.joinToString("\n\n")
}
