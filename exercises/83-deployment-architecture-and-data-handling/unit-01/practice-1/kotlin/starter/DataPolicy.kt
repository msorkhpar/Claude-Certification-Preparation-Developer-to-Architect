/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */

val SAFE_INPUT = listOf("tokenise", "redact") // pii_handling values that keep identifiers out of the prompt
val WANT = mapOf("us" to "us", "eu" to "eu", "other" to "global") // the residency of the deployment that may serve a user region

fun checkDeployment(config: Map<String, Any?>, req: Map<String, Any?>): List<String>? {
    // TODO: the sorted list of finding ids for a deployment configuration against the requirements.
    return null
}

fun retentionActions(entries: List<Map<String, Any?>>, maxDays: Int, today: String): Map<String, Any?>? {
    // TODO: a map with purge and keep, both lists sorted by id.
    return null
}

fun pickDeployment(userRegion: String, deployments: List<Map<String, Any?>>): String? {
    // TODO: the name of the deployment that may serve the user's region, or null.
    return null
}
