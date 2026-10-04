package harness;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShowTest {
    @Test
    void stringsAreSingleQuotedUnlessTheyHoldASingleQuote() {
        assertEquals("'Paris.'", Show.py("Paris."));
        assertEquals("\"it's\"", Show.py("it's"));
        assertEquals("'a\\nb'", Show.py("a\nb"));
    }

    @Test
    void mapsListsBooleansAndNoneReadLikePython() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "adaptive");
        m.put("ok", true);
        m.put("n", 3);
        m.put("none", null);
        m.put("xs", List.of("a", 1.5));
        assertEquals("{'type': 'adaptive', 'ok': True, 'n': 3, 'none': None, 'xs': ['a', 1.5]}", Show.py(m));
    }

    @Test
    void aJsonTreeReadsLikeTheEquivalentPythonValue() {
        assertEquals("{'effort': 'high', 'flags': [True, None]}", Show.py(Scripted.tree("{\"effort\":\"high\",\"flags\":[true,null]}")));
    }
}
