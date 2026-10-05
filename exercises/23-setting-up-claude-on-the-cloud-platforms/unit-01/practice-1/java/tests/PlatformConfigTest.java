import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlatformConfigTest {
    // config.dir is the folder that holds the three config files: starter, reference or a planted wrong solution.
    private static final Path DIR = Path.of(System.getProperty("config.dir", "starter"));

    private static final Set<String> INVOKE = Set.of("bedrock-mantle:CreateInference", "bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream");
    // the AWS regions listed for Claude in Amazon Bedrock on 2026-10-02
    private static final Set<String> REGIONS = Set.of("af-south-1", "ap-northeast-1", "ap-northeast-2", "ap-northeast-3", "ap-south-1", "ap-south-2", "ap-southeast-1", "ap-southeast-2",
            "ap-southeast-3", "ap-southeast-4", "ca-central-1", "ca-west-1", "eu-central-1", "eu-central-2", "eu-north-1", "eu-south-1",
            "eu-south-2", "eu-west-1", "eu-west-2", "eu-west-3", "il-central-1", "me-central-1", "sa-east-1", "us-east-1", "us-east-2",
            "us-west-1", "us-west-2");
    private static final Set<String> VERTEX_MODELS = Set.of("claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-4-5@20251001", "claude-sonnet-4-6");
    private static final String WANTED_ARN = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(String name) {
        try {
            return (Map<String, Object>) Json.parse(Files.readString(DIR.resolve(name)));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        if (value == null) return List.of();
        return value instanceof List<?> l ? (List<Object>) l : List.of(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> statements() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object s : asList(load("bedrock-policy.json").get("Statement"))) out.add((Map<String, Object>) s);
        return out;
    }

    private static List<String> collect(String key) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> s : statements()) for (Object v : asList(s.get(key))) out.add((String) v);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @Test
    void m1_thePolicyAllowsInvokingOneModelInOneRegionAndNothingElse() {
        List<Map<String, Object>> stmts = statements();
        assertEquals(1, stmts.size());
        assertEquals("Allow", stmts.get(0).get("Effect"));
        assertFalse(collect("Action").isEmpty());
        assertTrue(INVOKE.containsAll(collect("Action")), collect("Action").toString());
        assertEquals(List.of(WANTED_ARN), collect("Resource"));
    }

    @Test
    void e1_noActionIsAWildcardAndEveryActionIsAnInvokeAction() {
        assertFalse(collect("Action").isEmpty(), "the policy has no action");
        for (String action : collect("Action")) {
            assertFalse(action.contains("*"), action);
            assertTrue(INVOKE.contains(action), action);
        }
    }

    @Test
    void e2_everyResourceArnNamesOneDocumentedRegionAndOneModel() {
        assertFalse(collect("Resource").isEmpty(), "the policy has no resource");
        for (String arn : collect("Resource")) {
            assertFalse(arn.contains("*"), arn);
            String[] parts = arn.split(":");
            assertTrue(parts.length == 6 && parts[0].equals("arn") && parts[1].equals("aws") && parts[2].equals("bedrock"), arn);
            assertTrue(REGIONS.contains(parts[3]), arn);
            assertTrue(parts[5].startsWith("foundation-model/anthropic.claude-"), arn);
        }
    }

    @Test
    void e3_everyStatementAllowsAndThePolicyUsesTheCurrentVersion() {
        assertEquals("2012-10-17", load("bedrock-policy.json").get("Version"));
        assertFalse(statements().isEmpty());
        for (Map<String, Object> s : statements()) {
            assertEquals("Allow", s.get("Effect"));
            assertFalse(s.containsKey("NotAction") || s.containsKey("NotResource"));
        }
    }

    @Test
    void e4_theVertexRoleIsACustomRoleThatCanOnlyPredict() {
        Map<String, Object> role = map(load("vertex.json").get("role"));
        String first = String.valueOf(role.get("id")).split("/")[0];
        assertTrue(first.equals("projects") || first.equals("organizations"), String.valueOf(role.get("id")));
        assertEquals(List.of("aiplatform.endpoints.predict"), role.get("permissions"));
    }

    @Test
    void e5_theVertexEndpointKeepsTheDataWhereResidencySaysAndServesTheModel() {
        Map<String, Object> cfg = load("vertex.json");
        String endpoint = String.valueOf(cfg.get("endpoint"));
        String residency = String.valueOf(cfg.get("residency"));
        String model = String.valueOf(cfg.get("model"));
        assertTrue(VERTEX_MODELS.contains(model), model);
        if (residency.equals("eu")) assertTrue(endpoint.equals("eu") || endpoint.startsWith("europe-"), endpoint);
        if (residency.equals("us")) assertTrue(endpoint.equals("us") || endpoint.startsWith("us-"), endpoint);
        if (!Set.of("global", "us", "eu").contains(endpoint)) assertEquals("claude-sonnet-4-6", model, model + " is not served on a specific region");
    }

    @Test
    void e6_modelIdsUseEachPlatformsOwnForm() {
        for (String arn : collect("Resource")) {
            String last = arn.substring(arn.lastIndexOf('/') + 1);
            assertTrue(last.startsWith("anthropic.claude-"), arn);
        }
        String model = String.valueOf(load("vertex.json").get("model"));
        assertFalse(model.startsWith("anthropic."), model);
        if (model.startsWith("claude-haiku-4-5")) assertEquals("claude-haiku-4-5@20251001", model);
    }

    @Test
    void e7_theQuotaRequestStaysUnderTheSelfServiceCeiling() {
        Map<String, Object> quota = load("quotas.json");
        long input = ((Number) quota.get("input_tpm")).longValue();
        long output = ((Number) quota.get("output_tpm")).longValue();
        assertTrue(input > 0 && output > 0);
        if (!Boolean.TRUE.equals(quota.get("anthropic_approval"))) {
            assertTrue(input <= 5_000_000L, String.valueOf(input));
            assertTrue(output <= 500_000L, String.valueOf(output));
        }
    }
}
