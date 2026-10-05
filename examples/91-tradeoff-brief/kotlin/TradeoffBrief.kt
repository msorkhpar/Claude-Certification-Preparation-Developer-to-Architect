/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */

private val log = System.getLogger("tradeoff_brief")

data class Sla(val name: String, val limit: Int, val direction: String, val unit: String)

data class Segment(val name: String, val right: Int, val total: Int, val errorCost: Int)

fun group(n: Long): String = String.format(java.util.Locale.US, "%,d", n)

/** Whole percent, halves rounded up, and 0 for no cases. */
fun pct(right: Int, total: Int): Int = if (total == 0) 0 else (200 * right + total) / (2 * total)

/** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
fun breakEven(errorCost: Int, reviewCost: Int): Int = 100 - (100 * reviewCost + errorCost - 1) / errorCost

/** A service level is met at its limit exactly, and a miss says by how much. */
fun slaLine(sla: Sla, measured: Int): String {
    log.log(System.Logger.Level.DEBUG, "slaLine input: {0}", sla)
    val met = if (sla.direction == "max") measured <= sla.limit else measured >= sla.limit
    val verdict = if (met) "met" else "missed by ${Math.abs(measured - sla.limit)} ${sla.unit}"
    val word = if (sla.direction == "max") "limit" else "floor"
    return "${sla.name}: $measured ${sla.unit} against a $word of ${sla.limit} ${sla.unit}: $verdict"
}

/** The costliest segment first, with its accuracy and whether a person checks it. */
fun segmentReport(segments: List<Segment>, reviewCost: Int): List<String> =
    segments.sortedWith(compareBy<Segment>({ -it.errorCost }, { it.name })).map { s ->
        val floor = breakEven(s.errorCost, reviewCost)
        val handling = if (pct(s.right, s.total) >= floor) "auto" else "reviewed"
        "${s.name}: ${pct(s.right, s.total)} percent right, error cost ${s.errorCost}, $handling (break-even $floor)"
    }

/** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
fun brief(audience: String, design: String, cost: Int, baseline: Int, weakest: Segment, ask: String): String {
    if (audience == "sponsor") {
        return "$design costs ${group(cost.toLong())} a month against ${group(baseline.toLong())} for people alone, a saving of ${group((baseline - cost).toLong())}. " +
            "The weakest answers are ${weakest.name}: ${pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs ${weakest.errorCost}, " +
            "so a person decides them. Decision asked: $ask."
    }
    return "design=$design; cost=$cost; baseline=$baseline; saving=${baseline - cost}; weakest=${weakest.name} ${pct(weakest.right, weakest.total)}% at ${weakest.errorCost} an error"
}

fun main() {
    val latency = Sla("p95 latency", 2000, "max", "ms")
    val availability = Sla("availability", 995, "min", "per mille")
    for ((sla, measured) in listOf(latency to 1800, latency to 2000, latency to 2150, availability to 997, availability to 990)) println(slaLine(sla, measured))
    val segments = listOf(Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250), Segment("complaint", 91, 100, 60))
    for (line in segmentReport(segments, 5)) println(line)
    val weakest = segments.minByOrNull { pct(it.right, it.total) }!!
    for (audience in listOf("sponsor", "engineer")) println("$audience: ${brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot")}")
}
