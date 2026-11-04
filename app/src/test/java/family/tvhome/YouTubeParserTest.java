package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class YouTubeParserTest {
    private static String fixture() throws IOException {
        try (InputStream in = YouTubeParserTest.class.getResourceAsStream("/youtube_popular_sample.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test public void fixtureYieldsTheTwoCompleteItems() throws Exception {
        List<Suggestion> items = YouTubeParser.parse(fixture());
        assertEquals(2, items.size());
        Suggestion first = items.get(0);
        assertEquals("youtube", first.tileId);
        assertEquals("First video", first.title);
        assertEquals("https://i.ytimg.com/vi/aaaaaaaaaaa/mqdefault.jpg", first.thumbnailUrl);
        assertEquals("https://www.youtube.com/watch?v=aaaaaaaaaaa", first.openUri);
    }

    @Test public void fullShapedApiResponseParsesToFourPlayableItems() throws Exception {
        String body;
        try (InputStream in = YouTubeParserTest.class.getResourceAsStream("/youtube_popular_full_shape.json")) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Suggestion> items = YouTubeParser.parse(body);
        assertEquals(4, items.size());
        for (Suggestion s : items) {
            assertTrue(s.title, !s.title.isEmpty());
            assertTrue(s.thumbnailUrl, s.thumbnailUrl.startsWith("https://i.ytimg.com/"));
            assertTrue(s.openUri, s.openUri.startsWith("https://www.youtube.com/watch?v="));
        }
    }

    @Test public void fallsBackToHigherThumbnailWhenMediumIsMissing() throws Exception {
        assertEquals("https://i.ytimg.com/vi/bbbbbbbbbbb/hqdefault.jpg", YouTubeParser.parse(fixture()).get(1).thumbnailUrl);
    }

    @Test public void itemWithoutIdIsSkipped() throws Exception {
        for (Suggestion s : YouTubeParser.parse(fixture())) assertTrue(!s.title.startsWith("No id"));
    }

    @Test public void emptyObjectYieldsEmptyList() throws Exception {
        assertTrue(YouTubeParser.parse("{}").isEmpty());
    }

    @Test(expected = IOException.class) public void notJsonThrowsIoException() throws Exception {
        YouTubeParser.parse("not json");
    }
}
