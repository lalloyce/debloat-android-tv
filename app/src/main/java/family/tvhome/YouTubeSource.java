package family.tvhome;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** The most popular videos for a region. The API key is never logged or put into an exception message. */
final class YouTubeSource implements SuggestionSource {
    private final HttpFetcher fetcher;
    private final String apiKey;
    private final String regionCode;
    private final int max;

    YouTubeSource(HttpFetcher fetcher, String apiKey, String regionCode, int max) {
        this.fetcher = fetcher;
        this.apiKey = apiKey;
        this.regionCode = regionCode;
        this.max = max;
    }

    static String buildUrl(String regionCode, String apiKey, int max) {
        return "https://www.googleapis.com/youtube/v3/videos?part=snippet&chart=mostPopular&regionCode="
                + regionCode + "&maxResults=" + max + "&key=" + apiKey;
    }

    @Override public String tileId() { return "youtube"; }

    @Override
    public List<Suggestion> load() throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) return Collections.emptyList();
        String body;
        try {
            body = fetcher.get(buildUrl(regionCode, apiKey, max));
        } catch (IOException e) {
            throw new IOException("YouTube request failed: " + e.getClass().getSimpleName());
        }
        return YouTubeParser.parse(body);
    }
}
