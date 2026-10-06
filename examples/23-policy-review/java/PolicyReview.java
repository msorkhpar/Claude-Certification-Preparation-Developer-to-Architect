import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
 *
 * <p>The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
 * named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
 * predict permission (Google's IAM documentation, read 2026-10-02).
 */
public final class PolicyReview {
    private static final System.Logger LOG = System.getLogger(PolicyReview.class.getName());
    static final String BROAD = """
        {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "bedrock:*", "Resource": "*"}]}""";
    static final String NARROW = """
        {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["bedrock-mantle:CreateInference"],
          "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]}]}""";
    static final Map<String, String> ROLES = Map.of(
        "predefined", """
            {"id": "roles/aiplatform.user", "permissions": ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]}""",
        "custom", """
            {"id": "projects/example-project/roles/claudeInvoker", "permissions": ["aiplatform.endpoints.predict"]}""");

    private static final ObjectMapper JSON = new ObjectMapper();

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String json) {
        try {
            return JSON.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        if (value == null) return List.of();
        return value instanceof List<?> l ? (List<Object>) l : List.of(value);
    }

    @SuppressWarnings("unchecked")
    static List<String> reviewPolicy(Map<String, Object> policy) {
        List<String> findings = new ArrayList<>();
        int number = 0;
        for (Object item : asList(policy.get("Statement"))) {
            Map<String, Object> statement = (Map<String, Object>) item;
            number++;
            for (Object action : asList(statement.get("Action"))) {
                if (((String) action).contains("*")) findings.add("statement " + number + ": action " + action + " is a wildcard");
            }
            for (Object resource : asList(statement.get("Resource"))) {
                if (((String) resource).contains("*")) findings.add("statement " + number + ": resource " + resource + " names more than one model");
            }
            if (!"Allow".equals(statement.get("Effect"))) findings.add("statement " + number + ": effect is " + statement.get("Effect"));
        }
        return findings;
    }

    @SuppressWarnings("unchecked")
    static List<String> reviewRole(Map<String, Object> role) {
        List<String> findings = new ArrayList<>();
        String id = (String) role.get("id");
        if (id.startsWith("roles/")) findings.add(id + " is a predefined role, which carries more than the caller needs");
        List<String> extra = new ArrayList<>();
        for (Object p : (List<Object>) role.get("permissions")) if (!"aiplatform.endpoints.predict".equals(p)) extra.add((String) p);
        if (!extra.isEmpty()) findings.add("extra permissions: " + String.join(", ", extra));
        return findings;
    }

    private static void show(String name, List<String> found) {
        System.out.println(name + ": " + found.size() + " finding(s)");
        for (String item : found) System.out.println("  - " + item);
    }

    public static void main(String[] args) {
        show("broad policy", reviewPolicy(parse(BROAD)));
        show("narrow policy", reviewPolicy(parse(NARROW)));
        for (String name : List.of("predefined", "custom")) show(name + " role", reviewRole(parse(ROLES.get(name))));
    }
}
