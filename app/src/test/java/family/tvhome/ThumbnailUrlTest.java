package family.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThumbnailUrlTest {
    @Test public void httpsIsFetchable() { assertTrue(ThumbnailUrl.isFetchable("https://i.ytimg.com/vi/x/mqdefault.jpg")); }

    @Test public void cleartextHttpIsNotFetchableBecauseTheAppForbidsIt() { assertFalse(ThumbnailUrl.isFetchable("http://example.com/a.jpg")); }

    @Test public void otherSchemesAreNotFetchable() {
        assertFalse(ThumbnailUrl.isFetchable("file:///data/data/x/secret"));
        assertFalse(ThumbnailUrl.isFetchable("content://media/external/1"));
        assertFalse(ThumbnailUrl.isFetchable("jar:https://example.com/a.jar!/b"));
    }

    @Test public void emptyAndNullAreNotFetchable() {
        assertFalse(ThumbnailUrl.isFetchable(""));
        assertFalse(ThumbnailUrl.isFetchable(null));
    }
}
