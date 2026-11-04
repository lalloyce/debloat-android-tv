package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ChannelRowsMapperTest {
    private static ChannelRow row(String channel, String title) {
        return new ChannelRow(channel, title, "https://img/" + title, "intent://x/" + title + "#Intent;end");
    }

    private static List<Suggestion> map(List<ChannelRow> rows, List<String> preferred, int max) {
        return ChannelRowsMapper.map(rows, "yt", preferred, max);
    }

    @Test public void preferredChannelsAreJoinedInPreferredOrder() {
        List<Suggestion> r = map(Arrays.asList(row("Trending", "t1"), row("Recommended for you", "r1"),
                row("Trending", "t2"), row("YouTube Music", "m1")), Arrays.asList("Recommended for you", "Trending"), 10);
        assertEquals(Arrays.asList("r1", "t1", "t2"), titles(r));
        assertEquals("yt", r.get(0).tileId);
        assertEquals("https://img/r1", r.get(0).thumbnailUrl);
        assertEquals("intent://x/r1#Intent;end", r.get(0).openUri);
    }

    @Test public void fallsBackToNextPreferredWhenFirstIsEmpty() {
        List<Suggestion> r = map(Arrays.asList(row("Trending", "t1"), row("YouTube Music", "m1")),
                Arrays.asList("Recommended for you", "Trending"), 4);
        assertEquals(1, r.size());
        assertEquals("t1", r.get(0).title);
    }

    @Test public void noPreferredMatchTakesEveryUsableRowInOrder() {
        List<Suggestion> r = map(Arrays.asList(row("A", "a1"), row("B", "b1"), row("A", "a2")), Arrays.asList("Nope"), 4);
        assertEquals(Arrays.asList("a1", "b1", "a2"), titles(r));
    }

    @Test public void rowsWithoutTitleOrIntentAreDropped() {
        List<ChannelRow> rows = new ArrayList<>();
        rows.add(new ChannelRow("A", "", "https://i", "intent://x#Intent;end"));
        rows.add(new ChannelRow("A", "no intent", "https://i", ""));
        rows.add(new ChannelRow("A", null, "https://i", "intent://x#Intent;end"));
        rows.add(row("A", "ok"));
        assertEquals(Arrays.asList("ok"), titles(map(rows, Collections.emptyList(), 4)));
    }

    @Test public void nonHttpThumbnailBecomesEmpty() {
        List<ChannelRow> rows = Arrays.asList(new ChannelRow("A", "t", "android.resource://pkg/1", "intent://x#Intent;end"));
        assertEquals("", map(rows, Collections.emptyList(), 4).get(0).thumbnailUrl);
    }

    @Test public void resultIsLimitedToMax() {
        List<ChannelRow> rows = Arrays.asList(row("A", "1"), row("A", "2"), row("A", "3"));
        assertEquals(Arrays.asList("1", "2"), titles(map(rows, Collections.emptyList(), 2)));
    }

    @Test public void emptyInputGivesEmptyList() {
        assertTrue(map(Collections.emptyList(), Arrays.asList("A"), 4).isEmpty());
    }

    private static List<String> titles(List<Suggestion> list) {
        List<String> out = new ArrayList<>();
        for (Suggestion s : list) out.add(s.title);
        return out;
    }

    @Test public void cleartextAndNonWebThumbnailsAreDropped() {
        List<ChannelRow> rows = new ArrayList<>();
        rows.add(new ChannelRow("A", "plain http", "http://img/x", "intent://x#Intent;end"));
        rows.add(new ChannelRow("A", "content uri", "content://media/1", "intent://x#Intent;end"));
        List<Suggestion> out = ChannelRowsMapper.map(rows, "yt", Arrays.asList("A"), 10);
        assertEquals("", out.get(0).thumbnailUrl);
        assertEquals("", out.get(1).thumbnailUrl);
    }
}
