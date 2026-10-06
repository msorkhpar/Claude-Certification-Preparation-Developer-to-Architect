import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataPolicyTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> cfg(Object... over) {
        Map<String, Object> c = map("platform", "api", "zdr", true, "hipaa_baa", false, "model", "claude-sonnet-5-5", "inference_geo", "us", "region", null, "tenancy", "workspace-per-tenant",
                "pii_handling", "tokenise", "audit", map("store_prompts", false, "retain_days", 365));
        for (int i = 0; i < over.length; i += 2) c.put((String) over[i], over[i + 1]);
        return c;
    }

    private static Map<String, Object> req(Object... over) {
        Map<String, Object> r = map("residency", "us", "phi", false, "zdr_required", true, "multi_tenant", true, "audit_min_days", 180, "audit_max_days", 400);
        for (int i = 0; i < over.length; i += 2) r.put((String) over[i], over[i + 1]);
        return r;
    }

    private static List<String> check(Map<String, Object> config, Map<String, Object> requirements) {
        List<String> result = DataPolicy.checkDeployment(config, requirements);
        assertNotNull(result, "checkDeployment returned nothing");
        return result;
    }

    @Test
    void m1_aCompliantDeploymentHasNoFindings() {
        assertEquals(List.of(), check(cfg(), req()));
    }

    @Test
    void e1_residencyIsPinnedByTheRequestOrByTheRegionThePlatformAllows() {
        Map<String, Object> us = req("zdr_required", false);
        assertEquals(List.of("residency-not-pinned"), check(cfg("inference_geo", "global"), us));
        assertEquals(List.of("residency-not-pinned"), check(cfg("inference_geo", null), us));
        assertEquals(List.of(), check(cfg("platform", "aws-platform"), us));
        assertEquals(List.of(), check(cfg("platform", "bedrock", "region", "us-east-1", "inference_geo", null), us));
        assertEquals(List.of("residency-region"), check(cfg("platform", "bedrock", "region", "eu-west-1"), us));
        assertEquals(List.of(), check(cfg("platform", "vertex", "region", "us-east5"), us));
        Map<String, Object> eu = req("zdr_required", false, "residency", "eu");
        assertEquals(List.of("residency-unavailable"), check(cfg(), eu));
        assertEquals(List.of(), check(cfg("platform", "bedrock", "region", "eu-west-1"), eu));
        assertEquals(List.of(), check(cfg("platform", "vertex", "region", "europe-west4"), eu));
        assertEquals(List.of(), check(cfg("platform", "vertex", "region", "eu"), eu));
        assertEquals(List.of("residency-region"), check(cfg("platform", "vertex", "region", "us-east5"), eu));
        assertEquals(List.of("residency-region"), check(cfg("platform", "bedrock", "region", null), eu));
    }

    @Test
    void e2_zeroDataRetentionIsAnArrangementOfTheProvidersOwnPlatformsAndNotOfEveryModel() {
        assertEquals(List.of("zdr-missing"), check(cfg("zdr", false), req()));
        assertEquals(List.of(), check(cfg("zdr", false), req("zdr_required", false)));
        assertEquals(List.of("zdr-not-anthropics"), check(cfg("platform", "bedrock", "region", "us-east-1", "zdr", true), req()));
        assertEquals(List.of("model-needs-retention"), check(cfg("model", "claude-fable-5-1"), req()));
        assertEquals(List.of("model-needs-retention", "zdr-missing"), check(cfg("model", "claude-fable-5-1", "zdr", false), req()));
        assertEquals(List.of(), check(cfg("model", "claude-fable-5-1"), req("zdr_required", false)));
    }

    @Test
    void e3_protectedHealthInformationNeedsACoveredPlatformAnAgreementAndDeidentifiedInput() {
        Map<String, Object> phi = req("zdr_required", false, "phi", true);
        assertEquals(List.of("phi-no-baa"), check(cfg(), phi));
        assertEquals(List.of(), check(cfg("hipaa_baa", true), phi));
        assertEquals(List.of("phi-platform-unsupported"), check(cfg("platform", "aws-platform", "hipaa_baa", true), phi));
        assertEquals(List.of(), check(cfg("platform", "bedrock", "region", "us-east-1"), phi));
        assertEquals(List.of("phi-not-deidentified"), check(cfg("hipaa_baa", true, "pii_handling", "none"), phi));
        assertEquals(List.of(), check(cfg("hipaa_baa", true, "pii_handling", "redact"), phi));
        Map<String, Object> withoutPii = cfg("hipaa_baa", true);
        withoutPii.remove("pii_handling");
        assertEquals(List.of("phi-not-deidentified"), check(withoutPii, phi));
    }

    @Test
    void e4_aMultiTenantServiceNeedsAWorkspaceForEachTenant() {
        assertEquals(List.of("tenant-isolation"), check(cfg("tenancy", "shared"), req()));
        assertEquals(List.of(), check(cfg("tenancy", "shared"), req("multi_tenant", false)));
    }

    @Test
    void e5_anAuditLogMustNotStorePromptsThatHoldSensitiveData() {
        Map<String, Object> stores = map("store_prompts", true, "retain_days", 365);
        assertEquals(List.of(), check(cfg("audit", stores), req()));
        assertEquals(List.of("audit-stores-sensitive"), check(cfg("audit", stores, "pii_handling", "none"), req()));
        assertEquals(List.of("audit-stores-sensitive"), check(cfg("audit", stores, "hipaa_baa", true), req("phi", true)));
        assertEquals(List.of(), check(cfg("audit", map("store_prompts", false, "retain_days", 365), "pii_handling", "none"), req()));
    }

    private static Map<String, Object> days(int n) {
        return cfg("audit", map("store_prompts", false, "retain_days", n));
    }

    @Test
    void e6_auditRetentionStaysBetweenTheMinimumAndTheMaximum() {
        assertEquals(List.of(), check(days(400), req()));
        assertEquals(List.of(), check(days(180), req()));
        assertEquals(List.of("retention-too-long"), check(days(401), req()));
        assertEquals(List.of("retention-too-short"), check(days(179), req()));
        assertEquals(List.of(), check(cfg("audit", map("store_prompts", false)), req()));
        assertEquals(List.of(), check(days(10_000), map("residency", null)));
    }

    @Test
    void e7_purgeOnlyWhatIsPastTheRetentionLimitAndNotOnHold() {
        List<Map<String, Object>> entries = List.of(map("id", "old", "date", "2025-01-01"), map("id", "edge", "date", "2025-01-31"), map("id", "over", "date", "2025-01-30"),
                map("id", "held", "date", "2020-01-01", "hold", true), map("id", "new", "date", "2026-01-30"));
        Map<String, Object> result = DataPolicy.retentionActions(entries, 365, "2026-01-31");
        assertNotNull(result, "retentionActions returned nothing");
        assertEquals(map("purge", List.of("old", "over"), "keep", List.of("edge", "held", "new")), result);
        assertEquals(map("purge", List.of(), "keep", List.of()), DataPolicy.retentionActions(List.of(), 30, "2026-01-31"));
    }

    private static final List<Map<String, Object>> DEPLOYMENTS = List.of(map("name", "us-main", "residency", "us"), map("name", "eu-main", "residency", "eu"),
            map("name", "global-a", "residency", "global"), map("name", "eu-backup", "residency", "eu"));

    @Test
    void e8_aRequestIsServedOnlyByADeploymentThatKeepsItsDataInTheRegion() {
        assertEquals("us-main", DataPolicy.pickDeployment("us", DEPLOYMENTS));
        assertEquals("eu-backup", DataPolicy.pickDeployment("eu", DEPLOYMENTS));
        assertEquals("global-a", DataPolicy.pickDeployment("other", DEPLOYMENTS));
        assertNull(DataPolicy.pickDeployment("eu", List.of(DEPLOYMENTS.get(0), DEPLOYMENTS.get(2))));
        assertNull(DataPolicy.pickDeployment("other", DEPLOYMENTS.subList(0, 2)));
        assertNull(DataPolicy.pickDeployment("us", List.of()));
    }
}
