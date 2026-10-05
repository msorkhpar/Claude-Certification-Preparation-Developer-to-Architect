import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoundedTest {
    private fun sleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /** A work function that records how many calls are in flight at once. */
    private inner class Probe : (Int) -> Int {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val started = AtomicInteger()

        override fun invoke(item: Int): Int {
            started.incrementAndGet()
            val now = active.incrementAndGet()
            peak.accumulateAndGet(now) { a, b -> maxOf(a, b) }
            try {
                sleep(30)
                return item * 2
            } finally {
                active.decrementAndGet()
            }
        }
    }

    private fun range(n: Int): Iterator<Int> = (0 until n).iterator()

    private fun values(outcomes: List<Outcome<Int>>): List<Int?> = outcomes.map { it.value }

    @Test
    fun m1_resultsComeBackForEveryItemAndNeverMoreThanLimitRunAtOnce() {
        val probe = Probe()
        val outcomes = mapBounded(range(10), probe, 3)
        assertEquals((0 until 10).map { it * 2 }, values(outcomes).sortedBy { it }) // the order is the point of e1 alone
        assertTrue(outcomes.all { it.ok })
        assertEquals(3, probe.peak.get())
        assertEquals(10, probe.started.get())
    }

    @Test
    fun e1_resultsKeepTheInputOrderEvenWhenLaterItemsFinishFirst() {
        val outcomes = mapBounded(range(5), { item: Int -> sleep(if (item == 0) 120 else 2); item }, 5)
        assertEquals(listOf(0, 1, 2, 3, 4), values(outcomes))
    }

    @Test
    fun e2_aFailingItemIsReportedAndTheOthersStillFinish() {
        val sometimes = { item: Int ->
            if (item == 2) throw IllegalStateException("boom")
            sleep(5)
            item
        }
        val outcomes = try {
            mapBounded(range(5), sometimes, 2)
        } catch (e: RuntimeException) {
            fail("the run threw $e")
        }
        val failed = outcomes.filter { !it.ok }
        assertEquals(5, outcomes.size)
        assertEquals(1, failed.size)
        assertTrue(failed[0].error is IllegalStateException, "got ${failed[0].error}")
        assertEquals("boom", failed[0].error!!.message)
        assertEquals(listOf(0, 1, 3, 4), outcomes.filter { it.ok }.map { it.value }.sortedBy { it })
    }

    @Test
    fun e3_aLimitAboveTheItemCountAndAnEmptyInputBothWork() {
        val probe = Probe()
        val outcomes = mapBounded(listOf(1, 2, 3).iterator(), probe, 50)
        assertEquals(listOf(2, 4, 6), values(outcomes).sortedBy { it })
        assertEquals(3, probe.peak.get())
        assertEquals(0, mapBounded(range(0), Probe(), 4).size)
    }

    @Test
    fun e4_aLimitBelowOneIsRefused() {
        for (bad in listOf(0, -1)) {
            assertThrows(IllegalArgumentException::class.java, { mapBounded(listOf(1).iterator(), Probe(), bad) }, "limit $bad")
        }
    }

    @Test
    fun e5_itemsArePulledLazilySoASlowConsumerHoldsTheProducerBack() {
        val pulled = AtomicInteger()
        val source = object : Iterator<Int> {
            var n = 0
            override fun hasNext() = n < 20
            override fun next(): Int {
                pulled.incrementAndGet()
                return n++
            }
        }
        val gate = CountDownLatch(1)
        val blocked = { item: Int -> gate.await(); item }
        val run = CompletableFuture.supplyAsync { mapBounded(source, blocked, 2) }
        sleep(300)
        val held = pulled.get() // every worker is blocked: only `limit` items may have been taken
        gate.countDown()
        val outcomes = run.get(10, TimeUnit.SECONDS)
        assertEquals(2, held, "$held items were pulled while 2 workers were blocked")
        assertEquals((0 until 20).toList(), values(outcomes).sortedBy { it })
    }
}
