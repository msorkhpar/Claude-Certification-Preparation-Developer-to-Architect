import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SamplerTest {
    private fun draws(seed: Long, probs: List<Double>): List<Int> {
        val rng = Lcg(seed)
        return List(10) { sample(probs, rng) }
    }

    @Test
    fun probabilitiesSumToOneAtAnyTemperature() {
        for (t in listOf(0.1, 1.0, 5.0)) assertEquals(1.0, softmax(LOGITS, t).sum(), 1e-9)
    }

    @Test
    fun lowTemperatureSharpensAndHighFlattens() {
        val (low, mid, high) = listOf(0.5, 1.0, 2.0).map { softmax(LOGITS, it)[0] }
        assertTrue(low > mid && mid > high)
    }

    @Test
    fun greedyIgnoresTemperature() {
        assertEquals(0, greedy(softmax(LOGITS, 0.5)))
        assertEquals(0, greedy(softmax(LOGITS, 2.0)))
    }

    @Test
    fun sameSeedSameDrawsDifferentSeedMayDiffer() {
        val probs = softmax(LOGITS, 2.0)
        assertEquals(draws(7, probs), draws(7, probs))
        assertNotEquals(draws(7, probs), draws(8, probs))
    }
}
