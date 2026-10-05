import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */
final class DataPolicy {
    private static final System.Logger LOG = System.getLogger(DataPolicy.class.getName());
    private DataPolicy() {}

    static final List<String> SAFE_INPUT = List.of("tokenise", "redact"); // pii_handling values that keep identifiers out of the prompt
    static final Map<String, String> WANT = Map.of("us", "us", "eu", "eu", "other", "global"); // the residency of the deployment that may serve a user region

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean safeInput(Object value) {
        return value instanceof String s && SAFE_INPUT.contains(s);
    }

    private static List<String> report(Set<String> findings) {
        // TODO 1 of 6 (unlocks m1): the answer of checkDeployment.
        // Receives the set of finding ids. Returns them as a list sorted by text, or null when there is nothing to report yet.
        // Example: report(Set.of("zdr-missing", "phi-no-baa")) -> ["phi-no-baa", "zdr-missing"]
        return null;
    }

    private static Set<String> residencyFindings(Map<String, Object> req, Map<String, Object> config) {
        Object platform = config.get("platform");
        String region = config.get("region") == null ? "" : (String) config.get("region");
        boolean cloud = "bedrock".equals(platform) || "vertex".equals(platform);
        if ("us".equals(req.get("residency"))) {
            if (!cloud) {
                if (!"us".equals(config.get("inference_geo"))) return Set.of("residency-not-pinned");
            } else if (!region.startsWith("us-")) {
                return Set.of("residency-region");
            }
        } else if ("eu".equals(req.get("residency"))) {
            if (!cloud) {
                return Set.of("residency-unavailable");
            } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region.equals("eu"))) {
                return Set.of("residency-region");
            }
        }
        return Set.of();
    }

    private static Set<String> zdrFindings(Map<String, Object> req, Map<String, Object> config) {
        // TODO 2 of 6 (unlocks e2): the findings about zero data retention.
        // Receives the requirements and the configuration. Returns a set of finding ids. When `zdr_required` is set: `zdr-not-anthropics` on the
        // platforms `bedrock` and `vertex` (and nothing else); on the others `zdr-missing` when `zdr` is not set, and `model-needs-retention` when the
        // model name starts with `claude-fable`. Nothing is required, nothing is found.
        // Example: platform "api", zdr false, zdr_required true -> Set.of("zdr-missing")
        return Set.of();
    }

    private static Set<String> phiFindings(Map<String, Object> req, Map<String, Object> config) {
        // TODO 3 of 6 (unlocks e3): the findings about protected health information.
        // Receives the requirements and the configuration. Returns a set of finding ids. When `phi` is set: `phi-no-baa` on the platform `api` without
        // `hipaa_baa`; `phi-platform-unsupported` on `aws-platform`; `phi-not-deidentified` when `pii_handling` is not in SAFE_INPUT (use `safeInput`).
        // Example: platform "api", hipaa_baa false, pii_handling "tokenise", phi true -> Set.of("phi-no-baa")
        return Set.of();
    }

    private static Set<String> tenantFindings(Map<String, Object> req, Map<String, Object> config) {
        return truthy(req.get("multi_tenant")) && !"workspace-per-tenant".equals(config.get("tenancy")) ? Set.of("tenant-isolation") : Set.of();
    }

    private static Set<String> retentionFindings(Map<String, Object> req, Map<String, Object> config) {
        Set<String> findings = new TreeSet<>();
        Integer days = (Integer) asMap(config.get("audit")).get("retain_days");
        if (days != null) {
            if (req.get("audit_max_days") != null && days > (Integer) req.get("audit_max_days")) findings.add("retention-too-long");
            if (req.get("audit_min_days") != null && days < (Integer) req.get("audit_min_days")) findings.add("retention-too-short");
        }
        return findings;
    }

    private static Set<String> auditFindings(Map<String, Object> req, Map<String, Object> config) {
        // TODO 4 of 6 (unlocks e5): the finding about stored prompts.
        // Receives the requirements and the configuration; the audit settings are `asMap(config.get("audit"))` (it may be missing). Returns
        // Set.of("audit-stores-sensitive") when `store_prompts` is set and either `phi` is required or `pii_handling` is not in SAFE_INPUT; an empty set otherwise.
        // Example: store_prompts true, pii_handling "none" -> Set.of("audit-stores-sensitive")
        return Set.of();
    }

    static List<String> checkDeployment(Map<String, Object> config, Map<String, Object> req) {
        LOG.log(System.Logger.Level.DEBUG, "checkDeployment input: {0}", config);
        Set<String> findings = new TreeSet<>();
        findings.addAll(residencyFindings(req, config));
        findings.addAll(zdrFindings(req, config));
        findings.addAll(phiFindings(req, config));
        findings.addAll(tenantFindings(req, config));
        findings.addAll(auditFindings(req, config));
        findings.addAll(retentionFindings(req, config));
        return report(findings);
    }

    private static long ageDays(String day, String today) {
        return ChronoUnit.DAYS.between(LocalDate.parse(day), LocalDate.parse(today));
    }

    private static List<Map<String, Object>> byId(List<Map<String, Object>> entries) {
        List<Map<String, Object>> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> (String) e.get("id")));
        return sorted;
    }

    private static Map<String, Object> result(List<String> purge, List<String> keep) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("purge", purge);
        out.put("keep", keep);
        return out;
    }

    static Map<String, Object> retentionActions(List<Map<String, Object>> entries, int maxDays, String today) {
        // TODO 5 of 6 (unlocks e7): what a retention rule purges.
        // Receives the entries (each has `id` and an ISO `date`, and may have `hold`), the limit in days and today's ISO date. Returns
        // `result(purge, keep)`, the lists of ids to purge and to keep, in id order (`byId` sorts the entries, `ageDays` gives an entry's age). An entry is purged when it is older than the limit (strictly) and is not on hold.
        // Example: an entry dated 365 days before today with maxDays 365 is kept; one dated 366 days before is purged.
        return null;
    }

    static String pickDeployment(String userRegion, List<Map<String, Object>> deployments) {
        // TODO 6 of 6 (unlocks e8): the deployment that may serve a user.
        // Receives the user's region ("us", "eu" or "other") and the deployments (each has `name` and `residency`). Returns the first name, in
        // alphabetical order, of the deployments whose residency is WANT.get(userRegion), or null when there is none (a global deployment does not serve "us" or "eu").
        // Example: "eu" with eu-main and eu-backup -> "eu-backup"
        return null;
    }
}
