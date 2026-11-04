package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class YouTubeSourceTest {
    @Test public void buildUrlCarriesTheDocumentedParameters() {
        String url = YouTubeSource.buildUrl("KE", "K", 4);
        assertTrue(url, url.startsWith("https://"));
        assertTrue(url, url.contains("chart=mostPopular"));
        assertTrue(url, url.contains("regionCode=KE"));
        assertTrue(url, url.contains("maxResults=4"));
        assertTrue(url, url.contains("part=snippet"));
        assertTrue(url, url.contains("key=K"));
    }

    @Test public void blankKeyMakesNoRequestAndReturnsEmpty() throws Exception {
        final int[] calls = {0};
        YouTubeSource src = new YouTubeSource(url -> { calls[0]++; return "{}"; }, "  ", "KE", 4);
        assertTrue(src.load().isEmpty());
        assertEquals(0, calls[0]);
    }

    @Test public void tileIdIsYoutube() {
        assertEquals("youtube", new YouTubeSource(url -> "{}", "K", "KE", 4).tileId());
    }

    @Test public void loadParsesTheFetchedBody() throws Exception {
        String body = "{\"items\":[{\"id\":\"x\",\"snippet\":{\"title\":\"T\",\"thumbnails\":{\"medium\":{\"url\":\"u\"}}}}]}";
        List<Suggestion> items = new YouTubeSource(url -> body, "K", "KE", 4).load();
        assertEquals(1, items.size());
        assertEquals("T", items.get(0).title);
    }

    @Test public void failureIsPropagatedWithoutTheKey() {
        YouTubeSource src = new YouTubeSource(url -> { throw new IOException("failed " + url); }, "SECRETKEY", "KE", 4);
        try {
            src.load();
            fail("expected IOException");
        } catch (IOException e) {
            assertFalse(String.valueOf(e.getMessage()), String.valueOf(e.getMessage()).contains("SECRETKEY"));
            assertNull(e.getCause());
        }
    }
}
