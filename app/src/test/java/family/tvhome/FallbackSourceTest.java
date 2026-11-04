package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class FallbackSourceTest {
    private static final class Fake implements SuggestionSource {
        final String tile; final List<Suggestion> result; final IOException failure; int calls;
        Fake(String tile, List<Suggestion> result, IOException failure) { this.tile = tile; this.result = result; this.failure = failure; }
        @Override public String tileId() { return tile; }
        @Override public List<Suggestion> load() throws IOException { calls++; if (failure != null) throw failure; return result; }
    }

    private static List<Suggestion> one(String title) { return Collections.singletonList(new Suggestion("yt", title, "", "u")); }

    @Test public void nonEmptyPrimaryWinsAndFallbackIsNotCalled() throws Exception {
        Fake primary = new Fake("yt", one("p"), null), fallback = new Fake("yt", one("f"), null);
        assertEquals("p", new FallbackSource(primary, fallback).load().get(0).title);
        assertEquals(0, fallback.calls);
    }

    @Test public void emptyPrimaryUsesFallback() throws Exception {
        Fake primary = new Fake("yt", Collections.emptyList(), null), fallback = new Fake("yt", one("f"), null);
        assertEquals("f", new FallbackSource(primary, fallback).load().get(0).title);
    }

    @Test public void failingPrimaryUsesFallback() throws Exception {
        Fake primary = new Fake("yt", null, new IOException("no access")), fallback = new Fake("yt", one("f"), null);
        assertEquals("f", new FallbackSource(primary, fallback).load().get(0).title);
    }

    @Test public void crashingPrimaryUsesFallback() throws Exception {
        SuggestionSource crashing = new SuggestionSource() {
            @Override public String tileId() { return "yt"; }
            @Override public List<Suggestion> load() { throw new IllegalStateException("provider went away"); }
        };
        Fake fallback = new Fake("yt", one("f"), null);
        assertEquals("f", new FallbackSource(crashing, fallback).load().get(0).title);
    }

    @Test public void fallbackFailureIsPropagated() {
        Fake primary = new Fake("yt", Collections.emptyList(), null), fallback = new Fake("yt", null, new IOException("offline"));
        try { new FallbackSource(primary, fallback).load(); fail("expected IOException"); } catch (IOException expected) { }
    }

    @Test public void tileIdComesFromPrimary() {
        assertEquals("yt", new FallbackSource(new Fake("yt", null, null), new Fake("other", null, null)).tileId());
    }
}
