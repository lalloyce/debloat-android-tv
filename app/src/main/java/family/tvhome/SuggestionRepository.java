package family.tvhome;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Cache first, then the network. Only the most recent request is ever delivered. */
final class SuggestionRepository {
    interface Listener {
        void onSuggestions(long requestId, String tileId, List<Suggestion> items, boolean fromCache);
    }

    private final Map<String, SuggestionSource> sources;
    private final SuggestionCache cache;
    private final Executor background;
    private final Executor main;
    private final LongSupplier clock;
    private final AtomicLong latest = new AtomicLong();
    // The tile whose latest request has not finished yet; guarded by this.
    private String inFlightTile;
    private long inFlightId;

    SuggestionRepository(Map<String, SuggestionSource> sources, SuggestionCache cache, Executor background,
            Executor main, LongSupplier clock) {
        this.sources = sources;
        this.cache = cache;
        this.background = background;
        this.main = main;
        this.clock = clock;
    }

    /**
     * Asks for a tile's suggestions. A repeat request for the tile whose request is still running starts no second
     * fetch and returns the running request's id; its reply goes to the first caller's listener.
     */
    long request(String tileId, Listener listener) {
        long id;
        synchronized (this) {
            if (tileId.equals(inFlightTile) && inFlightId == latest.get()) return inFlightId;
            id = latest.incrementAndGet();
            inFlightTile = tileId;
            inFlightId = id;
        }
        final long requestId = id;
        background.execute(() -> {
            try {
                run(requestId, tileId, listener);
            } catch (RuntimeException e) {
                // Whatever the cache or a provider throws, the launcher must keep running; the row just stays empty.
                deliver(requestId, tileId, Collections.emptyList(), false, listener);
            } finally {
                synchronized (this) {
                    if (inFlightId == requestId) inFlightTile = null;
                }
            }
        });
        return id;
    }

    private void run(long id, String tileId, Listener listener) {
        if (id != latest.get()) return; // the user has already moved on; do no work for this request
        SuggestionCache.Cached cached = cache.load(tileId);
        boolean haveCache = cached != null && !cached.items.isEmpty();
        if (haveCache) deliver(id, tileId, cached.items, true, listener);
        SuggestionSource source = sources.get(tileId);
        if (source == null) {
            if (!haveCache) deliver(id, tileId, Collections.emptyList(), false, listener);
            return;
        }
        if (id != latest.get()) return;
        List<Suggestion> fresh;
        try {
            fresh = source.load();
        } catch (IOException | RuntimeException e) {
            if (!haveCache) deliver(id, tileId, Collections.emptyList(), false, listener);
            return;
        }
        if (fresh.isEmpty()) {
            if (!haveCache) deliver(id, tileId, fresh, false, listener);
            return;
        }
        try {
            cache.save(tileId, fresh, clock.getAsLong());
        } catch (IOException | RuntimeException ignored) {
            // A failed save only costs the instant start next time.
        }
        deliver(id, tileId, fresh, false, listener);
    }

    private void deliver(long id, String tileId, List<Suggestion> items, boolean fromCache, Listener listener) {
        main.execute(() -> {
            if (id == latest.get()) listener.onSuggestions(id, tileId, items, fromCache);
        });
    }
}
