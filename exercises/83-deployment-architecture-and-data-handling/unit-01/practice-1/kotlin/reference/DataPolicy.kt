import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = System.getLogger("data_policy")

/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */

val SAFE_INPUT = listOf("tokenise", "redact") // pii_handling values that keep identifiers out of the prompt
val WANT = mapOf("us" to "us", "eu" to "eu", "other" to "global") // the residency of the deployment that may serve a user region

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

private fun truthy(value: Any?): Boolean = value == true

private fun report(findings: Set<String>): List<String>? {
    return findings.sorted()
}

private fun residencyFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    val platform = config["platform"]
    val region = (config["region"] as String?) ?: ""
    val cloud = platform == "bedrock" || platform == "vertex"
    if (req["residency"] == "us") {
        if (!cloud) {
            if (config["inference_geo"] != "us") return setOf("residency-not-pinned")
        } else if (!region.startsWith("us-")) {
            return setOf("residency-region")
        }
    } else if (req["residency"] == "eu") {
        if (!cloud) {
            return setOf("residency-unavailable")
        } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region == "eu")) {
            return setOf("residency-region")
        }
    }
    return emptySet()
}

private fun zdrFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    val findings = sortedSetOf<String>()
    if (truthy(req["zdr_required"])) {
        if (config["platform"] == "bedrock" || config["platform"] == "vertex") {
            findings += "zdr-not-anthropics"
        } else {
            if (!truthy(config["zdr"])) findings += "zdr-missing"
            if ((config["model"] ?: "").toString().startsWith("claude-fable")) findings += "model-needs-retention"
        }
    }
    return findings
}

private fun phiFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    val findings = sortedSetOf<String>()
    if (truthy(req["phi"])) {
        if (config["platform"] == "api" && !truthy(config["hipaa_baa"])) findings += "phi-no-baa"
        if (config["platform"] == "aws-platform") findings += "phi-platform-unsupported"
        if (config["pii_handling"] !in SAFE_INPUT) {
            findings += "phi-not-deidentified"
        }
    }
    return findings
}

private fun tenantFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> =
    if (truthy(req["multi_tenant"]) && config["tenancy"] != "workspace-per-tenant") setOf("tenant-isolation") else emptySet()

private fun retentionFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    val findings = sortedSetOf<String>()
    val days = asMap(config["audit"])["retain_days"] as Int?
    if (days != null) {
        if (req["audit_max_days"] != null && days > (req["audit_max_days"] as Int)) findings += "retention-too-long"
        if (req["audit_min_days"] != null && days < (req["audit_min_days"] as Int)) findings += "retention-too-short"
    }
    return findings
}

private fun auditFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    val audit = asMap(config["audit"])
    return if (truthy(audit["store_prompts"]) && (truthy(req["phi"]) || config["pii_handling"] !in SAFE_INPUT)) setOf("audit-stores-sensitive") else emptySet()
}

fun checkDeployment(config: Map<String, Any?>, req: Map<String, Any?>): List<String>? {
    log.log(System.Logger.Level.DEBUG, "checkDeployment input: {0}", config)
    val findings = sortedSetOf<String>()
    for (check in listOf(::residencyFindings, ::zdrFindings, ::phiFindings, ::tenantFindings, ::auditFindings, ::retentionFindings)) findings += check(req, config)
    return report(findings)
}

private fun ageDays(day: String, today: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(day), LocalDate.parse(today))

fun retentionActions(entries: List<Map<String, Any?>>, maxDays: Int, today: String): Map<String, Any?>? {
    val purge = mutableListOf<String>()
    val keep = mutableListOf<String>()
    for (e in entries.sortedBy { it["id"] as String }) {
        (if (ageDays(e["date"] as String, today) > maxDays && !truthy(e["hold"])) purge else keep).add(e["id"] as String)
    }
    return linkedMapOf("purge" to purge, "keep" to keep)
}

fun pickDeployment(userRegion: String, deployments: List<Map<String, Any?>>): String? {
    return deployments.filter { it["residency"] == WANT[userRegion] }.map { it["name"] as String }.sorted().firstOrNull()
}
