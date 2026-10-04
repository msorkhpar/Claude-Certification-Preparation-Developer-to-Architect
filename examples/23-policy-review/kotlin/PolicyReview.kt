import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
 *
 * The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
 * named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
 * predict permission (Google's IAM documentation, read 2026-10-02).
 */
val BROAD = """{"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "bedrock:*", "Resource": "*"}]}"""
val NARROW = """
    {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["bedrock-mantle:CreateInference"],
      "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]}]}"""
val ROLES = mapOf(
    "predefined" to """{"id": "roles/aiplatform.user", "permissions": ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]}""",
    "custom" to """{"id": "projects/example-project/roles/claudeInvoker", "permissions": ["aiplatform.endpoints.predict"]}""",
)

@Suppress("UNCHECKED_CAST")
fun parse(json: String): Map<String, Any?> = ObjectMapper().readValue(json, Map::class.java) as Map<String, Any?>

fun asList(value: Any?): List<Any?> = when (value) {
    null -> emptyList()
    is List<*> -> value
    else -> listOf(value)
}

fun reviewPolicy(policy: Map<String, Any?>): List<String> {
    val findings = mutableListOf<String>()
    for ((index, item) in asList(policy["Statement"]).withIndex()) {
        val number = index + 1
        @Suppress("UNCHECKED_CAST") val statement = item as Map<String, Any?>
        for (action in asList(statement["Action"])) if ("*" in action as String) findings += "statement $number: action $action is a wildcard"
        for (resource in asList(statement["Resource"])) if ("*" in resource as String) findings += "statement $number: resource $resource names more than one model"
        if (statement["Effect"] != "Allow") findings += "statement $number: effect is ${statement["Effect"]}"
    }
    return findings
}

fun reviewRole(role: Map<String, Any?>): List<String> {
    val findings = mutableListOf<String>()
    val id = role["id"] as String
    if (id.startsWith("roles/")) findings += "$id is a predefined role, which carries more than the caller needs"
    val extra = asList(role["permissions"]).filter { it != "aiplatform.endpoints.predict" }
    if (extra.isNotEmpty()) findings += "extra permissions: " + extra.joinToString(", ")
    return findings
}

private fun show(name: String, found: List<String>) {
    println("$name: ${found.size} finding(s)")
    for (item in found) println("  - $item")
}

fun main() {
    show("broad policy", reviewPolicy(parse(BROAD)))
    show("narrow policy", reviewPolicy(parse(NARROW)))
    for (name in listOf("predefined", "custom")) show("$name role", reviewRole(parse(ROLES.getValue(name))))
}
