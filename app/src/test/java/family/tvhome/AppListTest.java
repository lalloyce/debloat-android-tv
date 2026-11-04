package family.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AppListTest {
    private static AppEntry e(String label, String pkg) { return new AppEntry(label, pkg, pkg + ".Main"); }

    @Test public void sortsByLabelIgnoringCase() {
        List<AppEntry> r = AppList.build(Arrays.asList(e("zebra", "z"), e("Apple", "a"), e("banana", "b")), "own");
        assertEquals("Apple", r.get(0).label);
        assertEquals("banana", r.get(1).label);
        assertEquals("zebra", r.get(2).label);
    }

    @Test public void dropsOwnPackage() {
        List<AppEntry> r = AppList.build(Arrays.asList(e("Home", "own"), e("Other", "o")), "own");
        assertEquals(1, r.size());
        assertEquals("o", r.get(0).packageName);
    }

    @Test public void keepsOneEntryPerPackage() {
        List<AppEntry> r = AppList.build(Arrays.asList(e("Play Store", "p"), e("Play Store", "p")), "own");
        assertEquals(1, r.size());
    }
}
