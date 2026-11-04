package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThumbnailBudgetTest {
    private static final long MIB = 1024L * 1024L;

    @Test public void largeHeapIsCappedAtFourMib() {
        assertEquals(4 * MIB, ThumbnailBudget.bytes(100 * MIB));
    }

    @Test public void smallHeapGetsTwentiethOfIt() {
        assertEquals(2 * MIB, ThumbnailBudget.bytes(40 * MIB));
    }

    @Test public void zeroHeapGivesZero() {
        assertEquals(0L, ThumbnailBudget.bytes(0));
    }
}
