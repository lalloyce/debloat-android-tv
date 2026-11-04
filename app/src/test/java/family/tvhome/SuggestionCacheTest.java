package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SuggestionCacheTest {
    private File dir;
    private SuggestionCache cache;

    @Before public void setUp() throws Exception {
        dir = Files.createTempDirectory("sugg").toFile();
        dir.deleteOnExit();
        cache = new SuggestionCache(dir);
    }

    private static Suggestion s(String title) {
        return new Suggestion("youtube", title, "https://img/" + title, "https://www.youtube.com/watch?v=" + title);
    }

    @Test public void saveThenLoadReturnsSameItemsAndTime() throws Exception {
        cache.save("youtube", Arrays.asList(s("a"), s("b")), 1234L);
        SuggestionCache.Cached c = cache.load("youtube");
        assertNotNull(c);
        assertEquals(1234L, c.savedAtMillis);
        assertEquals(2, c.items.size());
        assertEquals("a", c.items.get(0).title);
        assertEquals("https://img/b", c.items.get(1).thumbnailUrl);
        assertEquals("https://www.youtube.com/watch?v=a", c.items.get(0).openUri);
        assertEquals("youtube", c.items.get(0).tileId);
    }

    @Test public void unknownTileLoadsNull() {
        assertNull(cache.load("netflix"));
    }

    @Test public void truncatedFileLoadsNullWithoutThrowing() throws Exception {
        try (FileWriter w = new FileWriter(new File(dir, "youtube.json"))) {
            w.write("{\"items\": [");
        }
        assertNull(cache.load("youtube"));
    }

    @Test public void savingTwiceKeepsTheLaterList() throws Exception {
        cache.save("youtube", Collections.singletonList(s("old")), 1L);
        cache.save("youtube", Collections.singletonList(s("new")), 2L);
        List<Suggestion> items = cache.load("youtube").items;
        assertEquals(1, items.size());
        assertEquals("new", items.get(0).title);
    }
}
