import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */
final class DataPolicy {
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

    static List<String> checkDeployment(Map<String, Object> config, Map<String, Object> req) {
        Object platform = config.get("platform");
        String region = config.get("region") == null ? "" : (String) config.get("region");
        Map<String, Object> audit = asMap(config.get("audit"));
        boolean cloud = "bedrock".equals(platform) || "vertex".equals(platform);
        java.util.Set<String> findings = new TreeSet<>();
        if ("us".equals(req.get("residency"))) {
            if (!cloud) {
                if (!"us".equals(config.get("inference_geo"))) findings.add("residency-not-pinned");
            } else if (!region.startsWith("us-")) {
                findings.add("residency-region");
            }
        } else if ("eu".equals(req.get("residency"))) {
            if (!cloud) {
                findings.add("residency-unavailable");
            } else if (!(region.startsWith("eu-") || region.startsWith("europe-") || region.equals("eu"))) {
                findings.add("residency-region");
            }
        }
        if (truthy(req.get("zdr_required"))) {
            if (cloud) {
                findings.add("zdr-not-anthropics");
            } else {
                if (!truthy(config.get("zdr"))) findings.add("zdr-missing");
                if (String.valueOf(config.getOrDefault("model", "")).startsWith("claude-fable")) findings.add("model-needs-retention");
            }
        }
        if (truthy(req.get("phi"))) {
            if ("api".equals(platform) && !truthy(config.get("hipaa_baa"))) findings.add("phi-no-baa");
            if ("aws-platform".equals(platform)) findings.add("phi-platform-unsupported");
            if (!safeInput(config.get("pii_handling"))) {
                findings.add("phi-not-deidentified");
            }
        }
        if (truthy(req.get("multi_tenant")) && !"workspace-per-tenant".equals(config.get("tenancy"))) findings.add("tenant-isolation");
        if (truthy(audit.get("store_prompts")) && (truthy(req.get("phi")) || !safeInput(config.get("pii_handling")))) findings.add("audit-stores-sensitive");
        Integer days = (Integer) audit.get("retain_days");
        if (days != null) {
            if (req.get("audit_max_days") != null && days > (Integer) req.get("audit_max_days")) findings.add("retention-too-long");
            if (req.get("audit_min_days") != null && days < (Integer) req.get("audit_min_days")) findings.add("retention-too-short");
        }
        return new ArrayList<>(findings);
    }

    static Map<String, Object> retentionActions(List<Map<String, Object>> entries, int maxDays, String today) {
        LocalDate now = LocalDate.parse(today);
        List<Map<String, Object>> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> (String) e.get("id")));
        List<String> purge = new ArrayList<>();
        List<String> keep = new ArrayList<>();
        for (Map<String, Object> e : sorted) {
            long age = ChronoUnit.DAYS.between(LocalDate.parse((String) e.get("date")), now);
            (age > maxDays && !truthy(e.get("hold")) ? purge : keep).add((String) e.get("id"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("purge", purge);
        out.put("keep", keep);
        return out;
    }

    static String pickDeployment(String userRegion, List<Map<String, Object>> deployments) {
        List<String> matching = new ArrayList<>();
        for (Map<String, Object> d : deployments) if (d.get("residency").equals(WANT.get(userRegion))) matching.add((String) d.get("name"));
        matching.sort(Comparator.naturalOrder());
        return matching.isEmpty() ? null : matching.get(0);
    }
}
