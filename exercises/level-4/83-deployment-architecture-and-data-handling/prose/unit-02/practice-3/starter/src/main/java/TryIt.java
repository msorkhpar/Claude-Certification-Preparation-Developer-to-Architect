import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // The deployment the team runs, and what the requirements ask of it (the tests' compliant pair).
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("store_prompts", false);
        audit.put("retain_days", 365);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("platform", "api");
        config.put("zdr", true);
        config.put("hipaa_baa", false);
        config.put("model", "claude-sonnet-5-5");
        config.put("inference_geo", "us");
        config.put("region", null);
        config.put("tenancy", "workspace-per-tenant");
        config.put("pii_handling", "tokenise");
        config.put("audit", audit);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("residency", "us");
        req.put("phi", false);
        req.put("zdr_required", true);
        req.put("multi_tenant", true);
        req.put("audit_min_days", 180);
        req.put("audit_max_days", 400);
        System.out.println("compliant: " + DataPolicy.checkDeployment(config, req));

        // The same deployment checked against stricter requirements: EU residency, and patient data.
        Map<String, Object> strict = new LinkedHashMap<>(req);
        strict.put("residency", "eu");
        strict.put("phi", true);
        System.out.println("strict: " + DataPolicy.checkDeployment(config, strict));

        // Which deployment may serve a user in the EU.
        List<Map<String, Object>> deployments = List.of(Map.of("name", "us-api", "residency", "us"), Map.of("name", "eu-api", "residency", "eu"),
            Map.of("name", "global-api", "residency", "global"));
        System.out.println("deployment for an EU user: " + DataPolicy.pickDeployment("eu", deployments));
    }
}
