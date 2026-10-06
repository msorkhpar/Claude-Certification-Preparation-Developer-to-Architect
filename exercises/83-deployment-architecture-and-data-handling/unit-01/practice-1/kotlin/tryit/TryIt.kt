import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The deployment the team runs, and what the requirements ask of it (the tests' compliant pair).
    val config = mapOf("platform" to "api", "zdr" to true, "hipaa_baa" to false, "model" to "claude-sonnet-5-5", "inference_geo" to "us", "region" to null,
        "tenancy" to "workspace-per-tenant", "pii_handling" to "tokenise", "audit" to mapOf("store_prompts" to false, "retain_days" to 365))
    val req = mapOf("residency" to "us", "phi" to false, "zdr_required" to true, "multi_tenant" to true, "audit_min_days" to 180, "audit_max_days" to 400)
    println("compliant: ${checkDeployment(config, req)}")

    // The same deployment checked against stricter requirements: EU residency, and patient data.
    val strict = req + mapOf("residency" to "eu", "phi" to true)
    println("strict: ${checkDeployment(config, strict)}")

    // Which deployment may serve a user in the EU.
    val deployments = listOf(mapOf("name" to "us-api", "residency" to "us"), mapOf("name" to "eu-api", "residency" to "eu"), mapOf("name" to "global-api", "residency" to "global"))
    println("deployment for an EU user: ${pickDeployment("eu", deployments)}")
}
