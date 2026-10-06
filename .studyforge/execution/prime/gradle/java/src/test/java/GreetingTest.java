import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class GreetingTest {
    @Test
    void greets() { assertEquals("Hello, Ada", Greeting.of("Ada")); }
}
