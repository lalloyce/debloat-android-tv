package family.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class InputEntryTest {
    private final List<InputEntry> all = Arrays.asList(
            new InputEntry("hw1", "HDMI 1"), new InputEntry("hw2", "HDMI 2"), new InputEntry("hw3", "AV"));

    @Test public void hiddenInputsAreDropped() {
        List<InputEntry> r = InputEntry.passthrough(all, new HashSet<>(Collections.singletonList("hw2")));
        assertEquals(2, r.size());
        assertEquals("hw1", r.get(0).inputId);
        assertEquals("hw3", r.get(1).inputId);
    }

    @Test public void orderIsKept() {
        List<InputEntry> r = InputEntry.passthrough(all, Collections.emptySet());
        assertEquals("HDMI 1", r.get(0).label);
        assertEquals("HDMI 2", r.get(1).label);
        assertEquals("AV", r.get(2).label);
    }
}
