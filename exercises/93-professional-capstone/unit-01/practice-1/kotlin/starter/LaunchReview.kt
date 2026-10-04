/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */

/** TODO: the findings "<severity> <domain> <rule>", high first, then by domain, then by rule id. */
fun launchReview(f: Set<String>, n: Map<String, Int>): List<String> {
    TODO("write launchReview")
}

/** TODO: reject for a high finding, revise for a medium one, otherwise approve. */
fun verdict(findings: List<String>): String {
    TODO("write verdict")
}

/** TODO: the number of findings in each domain, P1 to P7. */
fun scorecard(findings: List<String>): List<Int> {
    TODO("write scorecard")
}

/** TODO: 100 minus the review cost as a percent of the error cost, rounded up, never below 0; 0 when an error costs nothing. */
fun neededAccuracy(errorCost: Int, reviewCost: Int): Int {
    TODO("write neededAccuracy")
}
