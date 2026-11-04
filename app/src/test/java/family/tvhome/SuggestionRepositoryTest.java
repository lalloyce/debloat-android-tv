package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;

public class SuggestionRepositoryTest {
    private static final Executor DIRECT = Runnable::run;

    private static final class Call {
        final long requestId; final String tileId; final List<Suggestion> items; final boolean fromCache;
        Call(long r, String t, List<Suggestion> i, boolean f) { requestId = r; tileId = t; items = i; fromCache = f; }
    }

    private static final class Recorder implements SuggestionRepository.Listener {
        final List<Call> calls = new ArrayList<>();
        @Override public void onSuggestions(long requestId, String tileId, List<Suggestion> items, boolean fromCache) {
            calls.add(new Call(requestId, tileId, items, fromCache));
        }
    }

    private static final class FakeSource implements SuggestionSource {
        final String tile; List<Suggestion> result; IOException failure; RuntimeException crash; int loads;
        FakeSource(String tile, List<Suggestion> result) { this.tile = tile; this.result = result; }
        @Override public String tileId() { return tile; }
        @Override public List<Suggestion> load() throws IOException {
            loads++;
            if (crash != null) throw crash;
            if (failure != null) throw failure;
            return result;
        }
    }

    private static Suggestion s(String tile, String title) {
        return new Suggestion(tile, title, "t", "u");
    }

    private SuggestionCache cache;

    @Before public void setUp() throws Exception {
        File dir = Files.createTempDirectory("repo").toFile();
        dir.deleteOnExit();
        cache = new SuggestionCache(dir);
    }

    private SuggestionRepository repo(Executor background, SuggestionSource... sources) {
        Map<String, SuggestionSource> map = new HashMap<>();
        for (SuggestionSource src : sources) map.put(src.tileId(), src);
        return new SuggestionRepository(map, cache, background, DIRECT, () -> 99L);
    }

    @Test public void cachedItemsComeFirstThenNetwork() throws Exception {
        cache.save("youtube", Collections.singletonList(s("youtube", "cached")), 1L);
        FakeSource src = new FakeSource("youtube", Collections.singletonList(s("youtube", "fresh")));
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(2, rec.calls.size());
        assertTrue(rec.calls.get(0).fromCache);
        assertEquals("cached", rec.calls.get(0).items.get(0).title);
        assertEquals("fresh", rec.calls.get(1).items.get(0).title);
        assertEquals("fresh", cache.load("youtube").items.get(0).title);
        assertEquals(99L, cache.load("youtube").savedAtMillis);
    }

    @Test public void failedFetchAfterCacheHitDeliversNothingMore() throws Exception {
        cache.save("youtube", Collections.singletonList(s("youtube", "cached")), 1L);
        FakeSource src = new FakeSource("youtube", Collections.emptyList());
        src.failure = new IOException("offline");
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(1, rec.calls.size());
        assertTrue(rec.calls.get(0).fromCache);
    }

    @Test public void failedFetchWithNoCacheDeliversOneEmptyList() {
        FakeSource src = new FakeSource("youtube", Collections.emptyList());
        src.failure = new IOException("HTTP 403");
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(1, rec.calls.size());
        assertTrue(rec.calls.get(0).items.isEmpty());
    }

    @Test public void tileWithNoSourceDeliversOneEmptyList() {
        Recorder rec = new Recorder();
        repo(DIRECT).request("netflix", rec);
        assertEquals(1, rec.calls.size());
        assertTrue(rec.calls.get(0).items.isEmpty());
        assertEquals("netflix", rec.calls.get(0).tileId);
    }

    @Test public void emptyFetchKeepsCachedItems() throws Exception {
        cache.save("youtube", Collections.singletonList(s("youtube", "cached")), 1L);
        FakeSource src = new FakeSource("youtube", Collections.emptyList());
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(1, rec.calls.size());
        assertEquals("cached", cache.load("youtube").items.get(0).title);
    }

    @Test public void slowReplyForOldTileIsDropped() {
        Queue<Runnable> queued = new ArrayDeque<>();
        Executor manual = queued::add;
        FakeSource a = new FakeSource("youtube", Collections.singletonList(s("youtube", "A")));
        FakeSource b = new FakeSource("tv", Collections.singletonList(s("tv", "B")));
        Recorder rec = new Recorder();
        SuggestionRepository repository = repo(manual, a, b);
        long first = repository.request("youtube", rec);
        long second = repository.request("tv", rec);
        assertTrue(second > first);
        while (!queued.isEmpty()) queued.remove().run();
        assertEquals(1, rec.calls.size());
        assertEquals("tv", rec.calls.get(0).tileId);
        assertEquals(second, rec.calls.get(0).requestId);
    }

    @Test public void runtimeFailureInASourceIsAFailedFetchNotACrash() {
        FakeSource src = new FakeSource("youtube", Collections.emptyList());
        src.crash = new IllegalStateException("provider went away");
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(1, rec.calls.size());
        assertTrue(rec.calls.get(0).items.isEmpty());
    }

    @Test public void runtimeFailureAfterCacheHitKeepsTheCachedRow() throws Exception {
        cache.save("youtube", Collections.singletonList(s("youtube", "cached")), 1L);
        FakeSource src = new FakeSource("youtube", Collections.emptyList());
        src.crash = new IllegalArgumentException("bad row");
        Recorder rec = new Recorder();
        repo(DIRECT, src).request("youtube", rec);
        assertEquals(1, rec.calls.size());
        assertTrue(rec.calls.get(0).fromCache);
    }

    @Test public void supersededRequestNeverLoadsItsSource() {
        Queue<Runnable> queued = new ArrayDeque<>();
        Executor manual = queued::add;
        FakeSource a = new FakeSource("youtube", Collections.singletonList(s("youtube", "A")));
        FakeSource b = new FakeSource("tv", Collections.singletonList(s("tv", "B")));
        SuggestionRepository repository = repo(manual, a, b);
        repository.request("youtube", new Recorder());
        repository.request("tv", new Recorder());
        while (!queued.isEmpty()) queued.remove().run();
        assertEquals(0, a.loads);
        assertEquals(1, b.loads);
    }

    @Test public void repeatRequestForTheSameTileWhileItIsInFlightStartsNoSecondFetch() {
        Queue<Runnable> queued = new ArrayDeque<>();
        Executor manual = queued::add;
        FakeSource a = new FakeSource("youtube", Collections.singletonList(s("youtube", "A")));
        SuggestionRepository repository = repo(manual, a);
        long first = repository.request("youtube", new Recorder());
        long second = repository.request("youtube", new Recorder());
        assertEquals(first, second);
        assertEquals(1, queued.size());
        while (!queued.isEmpty()) queued.remove().run();
        assertEquals(1, a.loads);
    }

    @Test public void requestAfterTheFirstFinishedFetchesAgain() {
        FakeSource a = new FakeSource("youtube", Collections.singletonList(s("youtube", "A")));
        SuggestionRepository repository = repo(DIRECT, a);
        repository.request("youtube", new Recorder());
        repository.request("youtube", new Recorder());
        assertEquals(2, a.loads);
    }

    @Test public void requestForAnotherTileIsNotMergedWithTheInFlightOne() {
        Queue<Runnable> queued = new ArrayDeque<>();
        Executor manual = queued::add;
        SuggestionRepository repository = repo(manual, new FakeSource("youtube", Collections.emptyList()), new FakeSource("tv", Collections.emptyList()));
        long first = repository.request("youtube", new Recorder());
        long second = repository.request("tv", new Recorder());
        assertTrue(second > first);
        assertEquals(2, queued.size());
    }
}
