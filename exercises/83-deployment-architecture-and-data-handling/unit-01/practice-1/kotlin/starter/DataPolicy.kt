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
    // TODO 1 of 6 (unlocks m1): the answer of checkDeployment.
    // Receives the set of finding ids. Returns them as a list sorted by text, or null when there is nothing to report yet.
    // Example: report(setOf("zdr-missing", "phi-no-baa")) -> ["phi-no-baa", "zdr-missing"]
    return null
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
    // TODO 2 of 6 (unlocks e2): the findings about zero data retention.
    // Receives the requirements and the configuration. Returns a set of finding ids. When `zdr_required` is set: `zdr-not-anthropics` on the
    // platforms `bedrock` and `vertex` (and nothing else); on the others `zdr-missing` when `zdr` is not set, and `model-needs-retention` when the
    // model name starts with `claude-fable`. Nothing is required, nothing is found.
    // Example: platform "api", zdr false, zdr_required true -> setOf("zdr-missing")
    return emptySet()
}

private fun phiFindings(req: Map<String, Any?>, config: Map<String, Any?>): Set<String> {
    // TODO 3 of 6 (unlocks e3): the findings about protected health information.
    // Receives the requirements and the configuration. Returns a set of finding ids. When `phi` is set: `phi-no-baa` on the platform `api` without
    // `hipaa_baa`; `phi-platform-unsupported` on `aws-platform`; `phi-not-deidentified` when `pii_handling` is not in SAFE_INPUT.
    // Example: platform "api", hipaa_baa false, pii_handling "tokenise", phi true -> setOf("phi-no-baa")
    return emptySet()
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
    // TODO 4 of 6 (unlocks e5): the finding about stored prompts.
    // Receives the requirements and the configuration; the audit settings are `asMap(config["audit"])` (it may be missing). Returns
    // setOf("audit-stores-sensitive") when `store_prompts` is set and either `phi` is required or `pii_handling` is not in SAFE_INPUT; an empty set otherwise.
    // Example: store_prompts true, pii_handling "none" -> setOf("audit-stores-sensitive")
    return emptySet()
}

fun checkDeployment(config: Map<String, Any?>, req: Map<String, Any?>): List<String>? {
    log.log(System.Logger.Level.DEBUG, "checkDeployment input: {0}", config)
    val findings = sortedSetOf<String>()
    for (check in listOf(::residencyFindings, ::zdrFindings, ::phiFindings, ::tenantFindings, ::auditFindings, ::retentionFindings)) findings += check(req, config)
    return report(findings)
}

private fun ageDays(day: String, today: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(day), LocalDate.parse(today))

fun retentionActions(entries: List<Map<String, Any?>>, maxDays: Int, today: String): Map<String, Any?>? {
    // TODO 5 of 6 (unlocks e7): what a retention rule purges.
    // Receives the entries (each has `id` and an ISO `date`, and may have `hold`), the limit in days and today's ISO date. Returns a map with
    // `purge` and `keep`, both lists of ids sorted by id. An entry is purged when it is older than the limit (strictly) and is not on hold.
    // Example: an entry dated 365 days before today with maxDays 365 is kept; one dated 366 days before is purged.
    return null
}

fun pickDeployment(userRegion: String, deployments: List<Map<String, Any?>>): String? {
    // TODO 6 of 6 (unlocks e8): the deployment that may serve a user.
    // Receives the user's region ("us", "eu" or "other") and the deployments (each has `name` and `residency`). Returns the first name, in
    // alphabetical order, of the deployments whose residency is WANT[userRegion], or null when there is none (a global deployment does not serve "us" or "eu").
    // Example: "eu" with eu-main and eu-backup -> "eu-backup"
    return null
}
