import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReliableCallTest {
    @Test
    void aRetryWithoutAKeyPaysTwice() {
        ReliableCall.Ledger ledger = new ReliableCall.Ledger();
        ReliableCall.retry(n -> ReliableCall.refund(ledger, null, "o1", 10, n == 1), 2);
        assertEquals(2, ledger.paid.size());
    }

    @Test
    void aRetryWithTheSameKeyPaysOnceAndReturnsTheFirstReceipt() {
        ReliableCall.Ledger ledger = new ReliableCall.Ledger();
        ReliableCall.Attempt<String> done = ReliableCall.retry(n -> ReliableCall.refund(ledger, "k", "o1", 10, n == 1), 2);
        assertEquals("refund-1", done.result());
        assertEquals(2, done.calls());
        assertEquals(1, ledger.paid.size());
    }

    @Test
    void aNewKeyIsANewRefund() {
        ReliableCall.Ledger ledger = new ReliableCall.Ledger();
        ReliableCall.refund(ledger, "a", "o1", 10, false);
        ReliableCall.refund(ledger, "b", "o1", 10, false);
        assertEquals(2, ledger.paid.size());
    }

    @Test
    void retryGivesUpAfterTheLastTry() {
        assertThrows(ReliableCall.Transient.class, () -> ReliableCall.retry(n -> {
            throw new ReliableCall.Transient("down");
        }, 3));
    }

    @Test
    void theBreakerOpensAfterTheThresholdAndProbesAfterTheCooldown() {
        ReliableCall.Breaker b = new ReliableCall.Breaker(3, 30);
        for (int t : new int[] {0, 1, 2}) b.record(false, t);
        assertEquals("open", b.state(3));
        assertFalse(b.allow(3));
        assertEquals("half-open", b.state(32));
        assertTrue(b.allow(32));
        b.record(false, 32);
        assertEquals("open", b.state(33));
        b.record(true, 70);
        assertEquals("closed", b.state(71));
    }
}
