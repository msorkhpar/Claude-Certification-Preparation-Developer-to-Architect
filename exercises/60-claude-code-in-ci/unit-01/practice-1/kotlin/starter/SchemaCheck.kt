import com.fasterxml.jackson.databind.JsonNode

/** Provided: the errors of a value against the JSON Schema subset the structured outputs support. Do not edit. */
object SchemaCheck {
    private fun isType(value: JsonNode, kind: String): Boolean = when (kind) {
        "object" -> value.isObject
        "array" -> value.isArray
        "string" -> value.isTextual
        "boolean" -> value.isBoolean
        "null" -> value.isNull
        "integer" -> value.isIntegralNumber
        "number" -> value.isNumber
        else -> throw IllegalArgumentException(kind)
    }

    private fun shown(n: JsonNode) = if (n.isTextual) "'${n.asText()}'" else n.toString()

    /** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
    fun schemaCheck(value: JsonNode, schema: JsonNode, path: String = "$"): List<String> {
        val want = schema.get("type")
        val wants = when {
            want == null -> emptyList()
            want.isArray -> want.map { it.asText() }
            else -> listOf(want.asText())
        }
        if (wants.isNotEmpty() && wants.none { isType(value, it) }) return listOf("$path: expected ${wants.joinToString(" or ")}")
        val errors = mutableListOf<String>()
        val allowed = schema.get("enum")
        if (allowed != null && allowed.none { it == value }) errors += "$path: ${shown(value)} is not one of [${allowed.joinToString(", ") { shown(it) }}]"
        if (value.isObject) {
            schema.get("required")?.forEach { k -> if (!value.has(k.asText())) errors += "$path.${k.asText()}: is required" }
            val properties = schema.get("properties")
            val extra = schema.get("additionalProperties")
            if (extra != null && extra.isBoolean && !extra.asBoolean()) value.fieldNames().forEach { k -> if (properties == null || !properties.has(k)) errors += "$path.$k: is not allowed" }
            properties?.fields()?.forEach { (k, sub) -> if (value.has(k)) errors += schemaCheck(value.get(k), sub, "$path.$k") }
        }
        if (value.isArray && schema.has("items")) value.forEachIndexed { i, item -> errors += schemaCheck(item, schema.get("items"), "$path[$i]") }
        return errors
    }
}
