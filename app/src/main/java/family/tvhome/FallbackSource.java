package family.tvhome;

import java.io.IOException;
import java.util.List;

/** The primary source's items when it has any; otherwise the fallback's. A failing or crashing primary counts as empty. */
final class FallbackSource implements SuggestionSource {
    private final SuggestionSource primary;
    private final SuggestionSource fallback;

    FallbackSource(SuggestionSource primary, SuggestionSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override public String tileId() { return primary.tileId(); }

    @Override
    public List<Suggestion> load() throws IOException {
        try {
            List<Suggestion> items = primary.load();
            if (!items.isEmpty()) return items;
        } catch (IOException | RuntimeException ignored) {
            // Fall through to the fallback.
        }
        return fallback.load();
    }
}
