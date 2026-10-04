import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class BpeTest {
    private static final String CORPUS = "low low low lower lower lowest newest newest widest widest";

    @Test
    void aFrequentWordBecomesOneTokenAndARareOneSplits() {
        List<Bpe.Rule> rules = Bpe.train(CORPUS, 6);
        assertEquals(List.of("low"), Bpe.encode("low", rules));
        assertTrue(Bpe.encode("lowish", rules).size() > 1);
    }

    @Test
    void anUnseenWordStillEncodesWithKnownPieces() {
        assertEquals("newer", String.join("", Bpe.encode("newer", Bpe.train(CORPUS, 6))));
    }

    @Test
    void noMergesMeansOneTokenPerCharacter() {
        assertEquals(List.of("l", "o", "w"), Bpe.encode("low", Bpe.train(CORPUS, 0)));
    }
}
