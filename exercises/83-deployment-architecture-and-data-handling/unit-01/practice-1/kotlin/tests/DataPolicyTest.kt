import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DataPolicyTest {
    private fun cfg(vararg over: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>(
        "platform" to "api", "zdr" to true, "hipaa_baa" to false, "model" to "claude-sonnet-5-5", "inference_geo" to "us", "region" to null, "tenancy" to "workspace-per-tenant",
        "pii_handling" to "tokenise", "audit" to mapOf("store_prompts" to false, "retain_days" to 365),
    ).also { it.putAll(over) }

    private fun req(vararg over: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>(
        "residency" to "us", "phi" to false, "zdr_required" to true, "multi_tenant" to true, "audit_min_days" to 180, "audit_max_days" to 400,
    ).also { it.putAll(over) }

    private fun check(config: Map<String, Any?>, requirements: Map<String, Any?>): List<String> {
        val result = checkDeployment(config, requirements)
        assertNotNull(result, "checkDeployment returned nothing")
        return result!!
    }

    @Test
    fun m1_aCompliantDeploymentHasNoFindings() {
        assertEquals(emptyList<String>(), check(cfg(), req()))
    }

    @Test
    fun e1_residencyIsPinnedByTheRequestOrByTheRegionThePlatformAllows() {
        val us = req("zdr_required" to false)
        assertEquals(listOf("residency-not-pinned"), check(cfg("inference_geo" to "global"), us))
        assertEquals(listOf("residency-not-pinned"), check(cfg("inference_geo" to null), us))
        assertEquals(emptyList<String>(), check(cfg("platform" to "aws-platform"), us))
        assertEquals(emptyList<String>(), check(cfg("platform" to "bedrock", "region" to "us-east-1", "inference_geo" to null), us))
        assertEquals(listOf("residency-region"), check(cfg("platform" to "bedrock", "region" to "eu-west-1"), us))
        assertEquals(emptyList<String>(), check(cfg("platform" to "vertex", "region" to "us-east5"), us))
        val eu = req("zdr_required" to false, "residency" to "eu")
        assertEquals(listOf("residency-unavailable"), check(cfg(), eu))
        assertEquals(emptyList<String>(), check(cfg("platform" to "bedrock", "region" to "eu-west-1"), eu))
        assertEquals(emptyList<String>(), check(cfg("platform" to "vertex", "region" to "europe-west4"), eu))
        assertEquals(emptyList<String>(), check(cfg("platform" to "vertex", "region" to "eu"), eu))
        assertEquals(listOf("residency-region"), check(cfg("platform" to "vertex", "region" to "us-east5"), eu))
        assertEquals(listOf("residency-region"), check(cfg("platform" to "bedrock", "region" to null), eu))
    }

    @Test
    fun e2_zeroDataRetentionIsAnArrangementOfTheProvidersOwnPlatformsAndNotOfEveryModel() {
        assertEquals(listOf("zdr-missing"), check(cfg("zdr" to false), req()))
        assertEquals(emptyList<String>(), check(cfg("zdr" to false), req("zdr_required" to false)))
        assertEquals(listOf("zdr-not-anthropics"), check(cfg("platform" to "bedrock", "region" to "us-east-1", "zdr" to true), req()))
        assertEquals(listOf("model-needs-retention"), check(cfg("model" to "claude-fable-5-1"), req()))
        assertEquals(listOf("model-needs-retention", "zdr-missing"), check(cfg("model" to "claude-fable-5-1", "zdr" to false), req()))
        assertEquals(emptyList<String>(), check(cfg("model" to "claude-fable-5-1"), req("zdr_required" to false)))
    }

    @Test
    fun e3_protectedHealthInformationNeedsACoveredPlatformAnAgreementAndDeidentifiedInput() {
        val phi = req("zdr_required" to false, "phi" to true)
        assertEquals(listOf("phi-no-baa"), check(cfg(), phi))
        assertEquals(emptyList<String>(), check(cfg("hipaa_baa" to true), phi))
        assertEquals(listOf("phi-platform-unsupported"), check(cfg("platform" to "aws-platform", "hipaa_baa" to true), phi))
        assertEquals(emptyList<String>(), check(cfg("platform" to "bedrock", "region" to "us-east-1"), phi))
        assertEquals(listOf("phi-not-deidentified"), check(cfg("hipaa_baa" to true, "pii_handling" to "none"), phi))
        assertEquals(emptyList<String>(), check(cfg("hipaa_baa" to true, "pii_handling" to "redact"), phi))
        val withoutPii = LinkedHashMap(cfg("hipaa_baa" to true)).also { it.remove("pii_handling") }
        assertEquals(listOf("phi-not-deidentified"), check(withoutPii, phi))
    }

    @Test
    fun e4_aMultiTenantServiceNeedsAWorkspaceForEachTenant() {
        assertEquals(listOf("tenant-isolation"), check(cfg("tenancy" to "shared"), req()))
        assertEquals(emptyList<String>(), check(cfg("tenancy" to "shared"), req("multi_tenant" to false)))
    }

    @Test
    fun e5_anAuditLogMustNotStorePromptsThatHoldSensitiveData() {
        val stores = mapOf("store_prompts" to true, "retain_days" to 365)
        assertEquals(emptyList<String>(), check(cfg("audit" to stores), req()))
        assertEquals(listOf("audit-stores-sensitive"), check(cfg("audit" to stores, "pii_handling" to "none"), req()))
        assertEquals(listOf("audit-stores-sensitive"), check(cfg("audit" to stores, "hipaa_baa" to true), req("phi" to true)))
        assertEquals(emptyList<String>(), check(cfg("audit" to mapOf("store_prompts" to false, "retain_days" to 365), "pii_handling" to "none"), req()))
    }

    private fun days(n: Int) = cfg("audit" to mapOf("store_prompts" to false, "retain_days" to n))

    @Test
    fun e6_auditRetentionStaysBetweenTheMinimumAndTheMaximum() {
        assertEquals(emptyList<String>(), check(days(400), req()))
        assertEquals(emptyList<String>(), check(days(180), req()))
        assertEquals(listOf("retention-too-long"), check(days(401), req()))
        assertEquals(listOf("retention-too-short"), check(days(179), req()))
        assertEquals(emptyList<String>(), check(cfg("audit" to mapOf("store_prompts" to false)), req()))
        assertEquals(emptyList<String>(), check(days(10_000), mapOf("residency" to null)))
    }

    @Test
    fun e7_purgeOnlyWhatIsPastTheRetentionLimitAndNotOnHold() {
        val entries = listOf(mapOf("id" to "old", "date" to "2025-01-01"), mapOf("id" to "edge", "date" to "2025-01-31"), mapOf("id" to "over", "date" to "2025-01-30"),
            mapOf("id" to "held", "date" to "2020-01-01", "hold" to true), mapOf("id" to "new", "date" to "2026-01-30"))
        val result = retentionActions(entries, 365, "2026-01-31")
        assertNotNull(result, "retentionActions returned nothing")
        assertEquals(mapOf("purge" to listOf("old", "over"), "keep" to listOf("edge", "held", "new")), result)
        assertEquals(mapOf("purge" to emptyList<String>(), "keep" to emptyList<String>()), retentionActions(emptyList(), 30, "2026-01-31"))
    }

    private val deployments: List<Map<String, Any?>> = listOf(
        mapOf("name" to "us-main", "residency" to "us"), mapOf("name" to "eu-main", "residency" to "eu"),
        mapOf("name" to "global-a", "residency" to "global"), mapOf("name" to "eu-backup", "residency" to "eu"),
    )

    @Test
    fun e8_aRequestIsServedOnlyByADeploymentThatKeepsItsDataInTheRegion() {
        assertEquals("us-main", pickDeployment("us", deployments))
        assertEquals("eu-backup", pickDeployment("eu", deployments))
        assertEquals("global-a", pickDeployment("other", deployments))
        assertNull(pickDeployment("eu", listOf(deployments[0], deployments[2])))
        assertNull(pickDeployment("other", deployments.subList(0, 2)))
        assertNull(pickDeployment("us", emptyList()))
    }
}
