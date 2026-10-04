import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageResult
import io.modelcontextprotocol.kotlin.sdk.types.ElicitRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ElicitResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.StopReason
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MrtrHttpTest {
    private fun answer(action: ElicitResult.Action, confirm: Boolean): (ElicitRequestParams) -> ElicitResult = {
        if (action == ElicitResult.Action.Accept) ElicitResult(action, buildJsonObject { put("confirm", confirm) }) else ElicitResult(action)
    }

    private suspend fun deploy(client: io.modelcontextprotocol.kotlin.sdk.client.Client, env: String): CallToolResult =
        client.callTool("deploy", mapOf("service" to "api", "env" to env))

    @Test
    fun aStagingDeployAsksNobody() = runBlocking {
        serveOnLoopback().use { server ->
            val client = connect(server.url, WireLog(), null, null)
            assertEquals("Deployed api to staging", textOf(deploy(client, "staging")))
            client.close()
        }
    }

    @Test
    fun aProductionDeployFollowsThePersonsAnswer() = runBlocking {
        serveOnLoopback().use { server ->
            for ((action, confirm, expected) in listOf(
                Triple(ElicitResult.Action.Accept, true, "Deployed api to production"),
                Triple(ElicitResult.Action.Accept, false, "Deployment cancelled"),
                Triple(ElicitResult.Action.Decline, false, "Deployment cancelled"))) {
                val client = connect(server.url, WireLog(), answer(action, confirm), null)
                assertEquals(expected, textOf(deploy(client, "production")), "$action $confirm")
                client.close()
            }
        }
    }

    @Test
    fun theReleaseNotesComeFromTheClientsModel() = runBlocking {
        serveOnLoopback().use { server ->
            val client = connect(server.url, WireLog(), null) { CreateMessageResult(Role.Assistant, listOf(TextContent("Faster.")), "scripted", StopReason.EndTurn) }
            assertEquals("api: Faster.", textOf(client.callTool("release_notes", mapOf("service" to "api"))))
            client.close()
        }
    }

    @Test
    fun aClientThatCannotBeAskedGetsAToolErrorAndNotAQuestion() = runBlocking {
        serveOnLoopback().use { server ->
            val client = connect(server.url, WireLog(), null, null)
            val result = deploy(client, "production")
            assertTrue(result.isError == true && "cannot be asked" in textOf(result))
            client.close()
        }
    }

    @Test
    fun theWireLogShowsTheServerAskingInTheMiddleOfTheCall() = runBlocking {
        serveOnLoopback().use { server ->
            val log = WireLog()
            val client = connect(server.url, log, answer(ElicitResult.Action.Accept, true), null)
            deploy(client, "production")
            assertEquals(listOf("-> tools/call deploy", "<- elicitation/create (a request from the server)", "-> the answer to the server's request", "<- complete: Deployed api to production"), log.lines.toList())
            assertTrue(log.sawInitialize && log.sessionOnEvery)
            client.close()
        }
    }
}
