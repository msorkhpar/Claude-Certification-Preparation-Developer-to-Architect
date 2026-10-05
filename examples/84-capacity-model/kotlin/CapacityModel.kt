private val log = System.getLogger("capacity_model")

/**
 * A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.
 *
 * The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
 * your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
 * Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
 * Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
 * limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
 */
/** Tokens per request and requests per minute. */
data class Workload(val rpm: Long, val input: Long, val cacheWrite: Long, val cacheRead: Long, val output: Long)

data class Need(val rpm: Long, val itpm: Long, val otpm: Long)

data class Tier(val name: String, val rpm: Long, val itpm: Long, val otpm: Long)

val TIERS = listOf(Tier("Start", 1000, 2_000_000, 400_000), Tier("Build", 5000, 5_000_000, 1_000_000), Tier("Scale", 10_000, 10_000_000, 2_000_000))

val CACHED = Workload(800, 1500, 200, 6000, 400)
val UNCACHED = Workload(800, 7700, 0, 0, 400) // the same prompts with no caching
const val REQUESTS_PER_MONTH = 2_000_000L

private fun up(x: Long, headroomPercent: Long) = (x * (100 + headroomPercent) + 99) / 100

/** RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom. */
fun requiredCapacity(w: Workload, headroomPercent: Long) =
    Need(up(w.rpm, headroomPercent), up(w.rpm * (w.input + w.cacheWrite), headroomPercent), up(w.rpm * w.output, headroomPercent))

fun smallestTier(need: Need, tiers: List<Tier>): String =
    tiers.firstOrNull { need.rpm <= it.rpm && need.itpm <= it.itpm && need.otpm <= it.otpm }?.name ?: "Custom"

/** Cents per month. The share of requests sent through the Batch API is billed at half price in every category. */
fun monthlyCents(w: Workload, requests: Long, batchPercent: Long): Long {
    log.log(System.Logger.Level.DEBUG, "monthlyCents input: {0}", w)
    val perRequest = w.input * 200 + w.cacheWrite * 250 + w.cacheRead * 20 + w.output * 1000 // cents times tokens, per million
    return requests * perRequest * (200 - batchPercent) / (200 * 1_000_000L)
}

fun dollars(cents: Long): String = "$" + "%,d".format(java.util.Locale.ROOT, cents / 100) + "." + "%02d".format(cents % 100)

fun main() {
    for ((label, workload) in listOf("with caching" to CACHED, "without caching" to UNCACHED)) {
        val need = requiredCapacity(workload, 30)
        println("$label: need ${need.rpm} rpm, ${need.itpm} itpm, ${need.otpm} otpm -> tier ${smallestTier(need, TIERS)}")
    }
    println("monthly bill with caching: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 0)))
    println("monthly bill without caching: " + dollars(monthlyCents(UNCACHED, REQUESTS_PER_MONTH, 0)))
    println("monthly bill with caching and 30 percent batch: " + dollars(monthlyCents(CACHED, REQUESTS_PER_MONTH, 30)))
}
