package family.tvhome;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ClockText {
    final String time;
    final String amPm;
    final String date;

    private ClockText(String time, String amPm, String date) {
        this.time = time;
        this.amPm = amPm;
        this.date = date;
    }

    static ClockText of(long epochMillis, ZoneId zone, boolean is24Hour, Locale locale) {
        ZonedDateTime at = Instant.ofEpochMilli(epochMillis).atZone(zone);
        String time = at.format(DateTimeFormatter.ofPattern(is24Hour ? "HH:mm" : "h:mm", locale));
        String amPm = is24Hour ? "" : at.format(DateTimeFormatter.ofPattern("a", locale));
        String date = at.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale));
        return new ClockText(time, amPm, date);
    }
}
