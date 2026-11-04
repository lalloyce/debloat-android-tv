package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HomeSelectionTest {
    @Test public void startsAtFirstTile() {
        assertEquals(0, new HomeSelection(4).current());
    }

    @Test public void selectMovesCurrent() {
        HomeSelection s = new HomeSelection(4);
        s.select(2);
        assertEquals(2, s.current());
    }

    @Test public void outOfRangeSelectIsIgnored() {
        HomeSelection s = new HomeSelection(4);
        s.select(2);
        s.select(9);
        s.select(-1);
        assertEquals(2, s.current());
    }

    @Test public void coldStartResetsToFirstTile() {
        HomeSelection s = new HomeSelection(4);
        s.select(3);
        s.onColdStart();
        assertEquals(0, s.current());
    }
}
