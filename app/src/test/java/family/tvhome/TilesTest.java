package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public class TilesTest {
    @Test public void fourTilesInOrder() {
        List<Tile> all = Tiles.ALL;
        assertEquals(4, all.size());
        assertEquals("youtube", all.get(0).id);
        assertEquals("netflix", all.get(1).id);
        assertEquals("tclmovies", all.get(2).id);
        assertEquals("tv", all.get(3).id);
    }

    @Test public void labelsFollowTheSpec() {
        assertEquals("YouTube", Tiles.ALL.get(0).label);
        assertEquals("Netflix", Tiles.ALL.get(1).label);
        assertEquals("TCL Movies", Tiles.ALL.get(2).label);
        assertEquals("TV", Tiles.ALL.get(3).label);
    }

    @Test public void targetsAreTheVerifiedPackages() {
        assertEquals("com.google.android.youtube.tv", Tiles.ALL.get(0).plans.get(0).packageName);
        assertEquals("com.netflix.ninja", Tiles.ALL.get(1).plans.get(0).packageName);
        LaunchPlan tcl = Tiles.ALL.get(2).plans.get(0);
        assertEquals("com.tcl.waterfall.overseas", tcl.packageName);
        assertEquals("com.tcl.waterfall.overseas.main.MainActivity", tcl.className);
        LaunchPlan tv = Tiles.ALL.get(3).plans.get(0);
        assertEquals("com.tcl.tv", tv.packageName);
        assertEquals("com.tcl.tv.TVActivity", tv.className);
    }

    @Test public void listIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> Tiles.ALL.add(Tiles.ALL.get(0)));
    }
}
