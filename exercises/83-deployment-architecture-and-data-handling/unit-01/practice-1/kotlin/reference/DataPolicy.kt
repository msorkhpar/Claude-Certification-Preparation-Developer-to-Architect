import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */

val SAFE_INPUT = listOf("tokenise", "redact") // pii_handling values that keep identifiers out of the prompt
val WANT = mapOf("us" to "us", "eu" to "eu", "other" to "global") // the residency of the deployment that may serve a user region

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

private fun truthy(value: Any?): Boolean = value == true

fun checkDeployment(config: Map<String, Any?>, req: Map<String, Any?>): List<String>? {
    val platform = config["platform"]
    val region = (config["region"] as String?) ?: ""
    val audit = asMap(config["audit"])
    val cloud = platform == "bedrock" || platform == "vertex"
    val findings = sortedSetOf<String>()
    if (req["residency"] == "us") {
        if (!cloud) {
            if (config["inference_geo"] != "us") findings += "residency-not-pinned"
        } else if (!region.startsWith("us-")) {
            findings += "residency-region"
        }
    } else if (req["residency"] == "eu") {
        if (!cloud) {
            findings += "residency-unavailable"
        } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region == "eu")) {
            findings += "residency-region"
        }
    }
    if (truthy(req["zdr_required"])) {
        if (cloud) {
            findings += "zdr-not-anthropics"
        } else {
            if (!truthy(config["zdr"])) findings += "zdr-missing"
            if ((config["model"] ?: "").toString().startsWith("claude-fable")) findings += "model-needs-retention"
        }
    }
    if (truthy(req["phi"])) {
        if (platform == "api" && !truthy(config["hipaa_baa"])) findings += "phi-no-baa"
        if (platform == "aws-platform") findings += "phi-platform-unsupported"
        if (config["pii_handling"] !in SAFE_INPUT) {
            findings += "phi-not-deidentified"
        }
    }
    if (truthy(req["multi_tenant"]) && config["tenancy"] != "workspace-per-tenant") findings += "tenant-isolation"
    if (truthy(audit["store_prompts"]) && (truthy(req["phi"]) || config["pii_handling"] !in SAFE_INPUT)) findings += "audit-stores-sensitive"
    val days = audit["retain_days"] as Int?
    if (days != null) {
        if (req["audit_max_days"] != null && days > (req["audit_max_days"] as Int)) findings += "retention-too-long"
        if (req["audit_min_days"] != null && days < (req["audit_min_days"] as Int)) findings += "retention-too-short"
    }
    return findings.toList()
}

fun retentionActions(entries: List<Map<String, Any?>>, maxDays: Int, today: String): Map<String, Any?>? {
    val now = LocalDate.parse(today)
    val purge = mutableListOf<String>()
    val keep = mutableListOf<String>()
    for (e in entries.sortedBy { it["id"] as String }) {
        val age = ChronoUnit.DAYS.between(LocalDate.parse(e["date"] as String), now)
        (if (age > maxDays && !truthy(e["hold"])) purge else keep).add(e["id"] as String)
    }
    return linkedMapOf("purge" to purge, "keep" to keep)
}

fun pickDeployment(userRegion: String, deployments: List<Map<String, Any?>>): String? =
    deployments.filter { it["residency"] == WANT[userRegion] }.map { it["name"] as String }.sorted().firstOrNull()
