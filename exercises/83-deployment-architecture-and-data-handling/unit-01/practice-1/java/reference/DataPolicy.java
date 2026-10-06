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
        return new ArrayList<>(new TreeSet<>(findings));
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
        Set<String> findings = new TreeSet<>();
        if (truthy(req.get("zdr_required"))) {
            if ("bedrock".equals(config.get("platform")) || "vertex".equals(config.get("platform"))) {
                findings.add("zdr-not-anthropics");
            } else {
                if (!truthy(config.get("zdr"))) findings.add("zdr-missing");
                if (String.valueOf(config.getOrDefault("model", "")).startsWith("claude-fable")) findings.add("model-needs-retention");
            }
        }
        return findings;
    }

    private static Set<String> phiFindings(Map<String, Object> req, Map<String, Object> config) {
        Set<String> findings = new TreeSet<>();
        if (truthy(req.get("phi"))) {
            if ("api".equals(config.get("platform")) && !truthy(config.get("hipaa_baa"))) findings.add("phi-no-baa");
            if ("aws-platform".equals(config.get("platform"))) findings.add("phi-platform-unsupported");
            if (!safeInput(config.get("pii_handling"))) {
                findings.add("phi-not-deidentified");
            }
        }
        return findings;
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
        Map<String, Object> audit = asMap(config.get("audit"));
        return truthy(audit.get("store_prompts")) && (truthy(req.get("phi")) || !safeInput(config.get("pii_handling"))) ? Set.of("audit-stores-sensitive") : Set.of();
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
        List<String> purge = new ArrayList<>();
        List<String> keep = new ArrayList<>();
        for (Map<String, Object> e : byId(entries)) {
            (ageDays((String) e.get("date"), today) > maxDays && !truthy(e.get("hold")) ? purge : keep).add((String) e.get("id"));
        }
        return result(purge, keep);
    }

    static String pickDeployment(String userRegion, List<Map<String, Object>> deployments) {
        return deployments.stream().filter(d -> d.get("residency").equals(WANT.get(userRegion))).map(d -> (String) d.get("name")).sorted().findFirst().orElse(null);
    }
}
