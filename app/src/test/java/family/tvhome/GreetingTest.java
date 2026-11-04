package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GreetingTest {
    @Test public void fourAmIsNight() { assertEquals("Good night", Greeting.forHour(4)); }
    @Test public void fiveAmIsMorning() { assertEquals("Good morning", Greeting.forHour(5)); }
    @Test public void elevenIsMorning() { assertEquals("Good morning", Greeting.forHour(11)); }
    @Test public void noonIsAfternoon() { assertEquals("Good afternoon", Greeting.forHour(12)); }
    @Test public void sixteenIsAfternoon() { assertEquals("Good afternoon", Greeting.forHour(16)); }
    @Test public void seventeenIsEvening() { assertEquals("Good evening", Greeting.forHour(17)); }
    @Test public void twentyIsEvening() { assertEquals("Good evening", Greeting.forHour(20)); }
    @Test public void twentyOneIsNight() { assertEquals("Good night", Greeting.forHour(21)); }
    @Test public void midnightIsNight() { assertEquals("Good night", Greeting.forHour(0)); }
    @Test(expected = IllegalArgumentException.class) public void twentyFourRejected() { Greeting.forHour(24); }
    @Test(expected = IllegalArgumentException.class) public void negativeRejected() { Greeting.forHour(-1); }
}
