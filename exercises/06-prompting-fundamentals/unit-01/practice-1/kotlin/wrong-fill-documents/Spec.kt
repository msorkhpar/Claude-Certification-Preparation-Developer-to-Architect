/** The given data model; solutions implement buildPrompt. Null or empty means "absent". */
data class Doc(val name: String, val text: String)
data class Example(val input: String, val output: String)
data class Spec(
    val task: String?,
    val role: String? = null,
    val context: String? = null,
    val documents: List<Doc> = emptyList(),
    val examples: List<Example> = emptyList(),
    val constraints: List<String> = emptyList(),
    val outputFormat: String? = null,
)
