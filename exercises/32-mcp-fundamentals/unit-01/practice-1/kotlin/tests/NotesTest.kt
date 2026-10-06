import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListPromptsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListResourceTemplatesRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListResourcesRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// The solution is compiled as the main source set (-Psolution=...); each test starts it as a separate process over stdio.
class NotesTest {
    /** The value of a call, or the error it threw. */
    class Att<T>(val value: T?, val error: Throwable?) {
        val ok get() = error == null
    }

    private suspend fun <T> attempt(call: suspend () -> T): Att<T> = try { Att(call(), null) } catch (e: Throwable) { Att(null, e) }

    /** Start the solution as a server over stdio, connect the SDK client to it and run steps. */
    private fun <T> session(steps: suspend (Client) -> T): T {
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process = ProcessBuilder(java, "-cp", System.getProperty("server.classpath"), "NotesServerKt").redirectError(ProcessBuilder.Redirect.DISCARD).start()
        val client = Client(Implementation(name = "course-tests", version = "1.0.0"))
        try {
            return runBlocking {
                withTimeout(60_000) {
                    client.connect(StdioClientTransport(process.inputStream.asSource().buffered(), process.outputStream.asSink().buffered()))
                    steps(client)
                }
            }
        } finally {
            process.destroyForcibly()
        }
    }

    private fun text(r: CallToolResult) = r.content.filterIsInstance<TextContent>().joinToString("") { it.text }

    private fun text(r: ReadResourceResult) = (r.contents.first() as TextResourceContents).text

    private fun isError(r: CallToolResult) = r.isError == true

    private suspend fun call(c: Client, tool: String, args: Map<String, Any?>) = attempt { c.callTool(tool, args) }

    private suspend fun add(c: Client, title: String, body: String = "body") = call(c, "add_note", mapOf("title" to title, "text" to body))

    private suspend fun search(c: Client, args: Map<String, Any?>) = call(c, "search_notes", args)

    private suspend fun read(c: Client, uri: String) = attempt { c.readResource(ReadResourceRequest(ReadResourceRequestParams(uri = uri))) }

    private suspend fun prompt(c: Client, args: Map<String, String>?) = attempt { c.getPrompt(GetPromptRequest(GetPromptRequestParams(name = "review_notes", arguments = args))) }

    private fun saved(a: Att<CallToolResult>) = if (a.ok) text(a.value!!) else a.error.toString()

    private fun readText(a: Att<ReadResourceResult>) = if (a.ok) text(a.value!!) else a.error.toString()

    private fun promptText(a: Att<GetPromptResult>) = (a.value!!.messages.first().content as TextContent).text

    @Test
    fun m1_aClientCanSaveANoteFindItAndReadItBack() {
        class Out(val info: Implementation?, val saved: Att<CallToolResult>, val found: Att<CallToolResult>, val got: Att<ReadResourceResult>)
        val o = session { c ->
            val saved = add(c, "Plan", "ship it")
            Out(c.serverVersion, saved, search(c, mapOf("query" to "ship")), read(c, "notes://note/1"))
        }
        assertEquals("notes", o.info?.name)
        assertEquals("1.0.0", o.info?.version)
        assertEquals("Saved note 1: Plan", saved(o.saved))
        assertFalse(o.saved.ok && isError(o.saved.value!!))
        assertEquals("1. Plan", saved(o.found))
        assertEquals("Plan\n\nship it", readText(o.got))
    }

    @Test
    fun e1_theServerDeclaresToolsResourcesAndPromptsAndNamesItsTools() {
        val (caps, listed) = session { c -> Pair(c.serverCapabilities, attempt { c.listTools(ListToolsRequest()) }) }
        assertTrue(caps?.tools != null && caps.resources != null && caps.prompts != null)
        assertTrue(listed.ok, listed.error.toString())
        val tools = listed.value!!.tools.associateBy { it.name }
        assertEquals(listOf("add_note", "search_notes"), tools.keys.sorted())
        assertTrue(tools.values.all { !it.description.isNullOrEmpty() })
        assertEquals(listOf("title", "text"), tools["add_note"]!!.inputSchema.required)
        assertEquals(listOf("query"), tools["search_notes"]!!.inputSchema.required)
        val limit = tools["search_notes"]!!.inputSchema.properties!!["limit"]!!.jsonObject
        assertEquals("integer", limit["type"]!!.jsonPrimitive.content)
        assertEquals(5, limit["default"]!!.jsonPrimitive.int)
    }

    @Test
    fun e2_badInputComesBackAsAToolErrorTheModelCanRead() {
        val r = session { c ->
            listOf(add(c, "  ", "x"), add(c, "T", "   "), add(c, "T", "x".repeat(501)), add(c, "T", "x".repeat(500)),
                search(c, mapOf("query" to " ")), search(c, mapOf("query" to "x", "limit" to 0)), search(c, mapOf("query" to "x", "limit" to 21)))
        }
        val messages = listOf("title is required", "text is required", "text is too long (max 500)", null, "query is required", "limit must be between 1 and 20", "limit must be between 1 and 20")
        messages.forEachIndexed { i, message ->
            assertTrue(r[i].ok, r[i].error.toString())
            if (message == null) {
                assertFalse(isError(r[i].value!!))
                assertEquals("Saved note 1: T", text(r[i].value!!))
            } else {
                assertTrue(isError(r[i].value!!), message)
                assertTrue(text(r[i].value!!).contains(message), text(r[i].value!!))
            }
        }
    }

    @Test
    fun e3_searchIgnoresCaseKeepsIdOrderHonoursTheLimitAndSaysWhenNothingMatches() {
        val r = session { c ->
            add(c, "Alpha", "first"); add(c, "beta", "ALPHA again"); add(c, "Gamma", "third"); add(c, "alphabet", "soup")
            listOf(search(c, mapOf("query" to "ALPHA")), search(c, mapOf("query" to "alpha", "limit" to 2)), search(c, mapOf("query" to "  zeta ")), search(c, mapOf("query" to "third", "limit" to 20)))
        }
        assertEquals("1. Alpha\n2. beta\n4. alphabet", saved(r[0]))
        assertEquals("1. Alpha\n2. beta", saved(r[1]))
        assertEquals("No notes match \"zeta\"", saved(r[2]))
        assertFalse(r[2].ok && isError(r[2].value!!))
        assertEquals("3. Gamma", saved(r[3]))
    }

    @Test
    fun e4_toolAnnotationsTellAClientWhichToolOnlyReads() {
        val listed = session { c -> attempt { c.listTools(ListToolsRequest()) } }
        assertTrue(listed.ok, listed.error.toString())
        val annotations = listed.value!!.tools.associate { it.name to it.annotations }
        assertNotNull(annotations["search_notes"])
        assertEquals(true, annotations["search_notes"]!!.readOnlyHint)
        assertNotNull(annotations["add_note"])
        assertEquals(false, annotations["add_note"]!!.readOnlyHint)
        assertEquals(false, annotations["add_note"]!!.destructiveHint)
        assertEquals(false, annotations["add_note"]!!.idempotentHint)
    }

    @Test
    fun e5_resourcesGiveTheCountANoteByIdAndAnErrorForAMissingOne() {
        class Out(val resources: Att<*>, val uris: List<String>, val templates: Att<*>, val templateUris: List<String>, val counts: List<Att<ReadResourceResult>>, val second: Att<ReadResourceResult>, val bad: List<Att<ReadResourceResult>>)
        val o = session { c ->
            val resources = attempt { c.listResources(ListResourcesRequest()) }
            val templates = attempt { c.listResourceTemplates(ListResourceTemplatesRequest()) }
            val zero = read(c, "notes://count")
            add(c, "One")
            val one = read(c, "notes://count")
            add(c, "Two")
            val two = read(c, "notes://count")
            Out(resources, resources.value?.resources?.map { it.uri }.orEmpty(), templates, templates.value?.resourceTemplates?.map { it.uriTemplate }.orEmpty(), listOf(zero, one, two),
                read(c, "notes://note/2"), listOf(read(c, "notes://note/3"), read(c, "notes://note/0"), read(c, "notes://note/abc")))
        }
        assertTrue(o.resources.ok, o.resources.error.toString())
        assertEquals(listOf("notes://count"), o.uris)
        assertTrue(o.templates.ok, o.templates.error.toString())
        assertEquals(listOf("notes://note/{id}"), o.templateUris)
        assertEquals(listOf("0 notes", "1 note", "2 notes"), o.counts.map { readText(it) })
        assertEquals("Two\n\nbody", readText(o.second))
        o.bad.forEach {
            assertFalse(it.ok)
            assertTrue(it.error!!.message.orEmpty().contains("No note"), it.error.toString())
        }
    }

    @Test
    fun e6_thePromptListsTheNotesAndDefaultsTheTone() {
        class Out(val listed: Att<*>, val names: List<String>, val argName: String?, val argRequired: Boolean?, val empty: Att<GetPromptResult>, val byDefault: Att<GetPromptResult>, val formal: Att<GetPromptResult>)
        val o = session { c ->
            val listed = attempt { c.listPrompts(ListPromptsRequest()) }
            val empty = prompt(c, emptyMap())
            add(c, "Alpha"); add(c, "Beta")
            val arg = listed.value?.prompts?.firstOrNull()?.arguments?.firstOrNull()
            Out(listed, listed.value?.prompts?.map { it.name }.orEmpty(), arg?.name, arg?.required, empty, prompt(c, emptyMap()), prompt(c, mapOf("tone" to "formal")))
        }
        assertTrue(o.listed.ok, o.listed.error.toString())
        assertEquals(listOf("review_notes"), o.names)
        assertEquals("tone", o.argName)
        assertNotEquals(true, o.argRequired)
        assertTrue(o.empty.ok, o.empty.error.toString())
        assertEquals("There are no notes to review.", promptText(o.empty))
        assertTrue(o.byDefault.ok, o.byDefault.error.toString())
        assertEquals(1, o.byDefault.value!!.messages.size)
        assertEquals(Role.User, o.byDefault.value.messages.first().role)
        assertEquals("Review these notes in a brief tone:\n- Alpha\n- Beta", promptText(o.byDefault))
        assertTrue(o.formal.ok, o.formal.error.toString())
        assertTrue(promptText(o.formal).contains("in a formal tone"))
    }

    @Test
    fun e7_idsAreSequentialAndAFailedCallDoesNotUseOne() {
        class Out(val calls: List<Att<CallToolResult>>, val one: Att<ReadResourceResult>, val count: Att<ReadResourceResult>)
        val o = session { c ->
            Out(listOf(add(c, "  First  ", "a"), add(c, "", "b"), add(c, "First", "c"), add(c, "Third", "   "), add(c, "Fourth", "d")), read(c, "notes://note/1"), read(c, "notes://count"))
        }
        assertEquals("Saved note 1: First", saved(o.calls[0]))
        assertTrue(o.calls[1].ok && isError(o.calls[1].value!!))
        assertEquals("Saved note 2: First", saved(o.calls[2]))
        assertTrue(o.calls[3].ok && isError(o.calls[3].value!!))
        assertEquals("Saved note 3: Fourth", saved(o.calls[4]))
        assertEquals("First\n\na", readText(o.one))
        assertEquals("3 notes", readText(o.count))
    }
}
