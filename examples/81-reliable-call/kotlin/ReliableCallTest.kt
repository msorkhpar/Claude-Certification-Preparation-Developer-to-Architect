import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReliableCallTest {
    @Test
    fun aRetryWithoutAKeyPaysTwice() {
        val ledger = Ledger()
        retry(2) { n -> refund(ledger, null, "o1", 10, n == 1) }
        assertEquals(2, ledger.paid.size)
    }

    @Test
    fun aRetryWithTheSameKeyPaysOnceAndReturnsTheFirstReceipt() {
        val ledger = Ledger()
        val (receipt, calls) = retry(2) { n -> refund(ledger, "k", "o1", 10, n == 1) }
        assertEquals(Triple("refund-1", 2, 1), Triple(receipt, calls, ledger.paid.size))
    }

    @Test
    fun aNewKeyIsANewRefund() {
        val ledger = Ledger()
        refund(ledger, "a", "o1", 10, false)
        refund(ledger, "b", "o1", 10, false)
        assertEquals(2, ledger.paid.size)
    }

    @Test
    fun retryGivesUpAfterTheLastTry() {
        assertThrows(Transient::class.java) { retry<String>(3) { throw Transient("down") } }
    }

    @Test
    fun theBreakerOpensAfterTheThresholdAndProbesAfterTheCooldown() {
        val b = Breaker(3, 30)
        for (t in listOf(0, 1, 2)) b.record(false, t)
        assertEquals("open", b.state(3))
        assertFalse(b.allow(3))
        assertEquals("half-open", b.state(32))
        assertTrue(b.allow(32))
        b.record(false, 32)
        assertEquals("open", b.state(33))
        b.record(true, 70)
        assertEquals("closed", b.state(71))
    }
}
