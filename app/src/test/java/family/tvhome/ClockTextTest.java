package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.ZoneId;
import java.util.Locale;
import org.junit.Test;

public class ClockTextTest {
    private static final ZoneId UTC_PLUS_3 = ZoneId.of("Etc/GMT-3");

    @Test public void twelveHourClockHasNoLeadingZero() {
        ClockText c = ClockText.of(1793710468000L, UTC_PLUS_3, false, Locale.ENGLISH);
        assertEquals("3:54", c.time);
        assertEquals("PM", c.amPm);
        assertEquals("Tuesday, 3 November", c.date);
    }

    @Test public void twentyFourHourClockHasNoAmPm() {
        ClockText c = ClockText.of(1793710468000L, UTC_PLUS_3, true, Locale.ENGLISH);
        assertEquals("15:54", c.time);
        assertEquals("", c.amPm);
    }

    @Test public void staleYearStillProducesText() {
        ClockText c = ClockText.of(1638094267000L, UTC_PLUS_3, false, Locale.ENGLISH);
        assertEquals("Sunday, 28 November", c.date);
        assertEquals("1:11", c.time);
        assertEquals("PM", c.amPm);
    }

    @Test public void epochZeroDoesNotThrow() {
        ClockText c = ClockText.of(0L, UTC_PLUS_3, false, Locale.ENGLISH);
        assertFalse(c.time.isEmpty());
        assertFalse(c.date.isEmpty());
    }
}
