package family.tvhome;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.tv.TvContract;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What an app publishes to the TV's shared channel data (the rows the stock home shows). Needs the
 * READ_TV_LISTINGS permission; without it, or with no rows, load() fails or returns nothing. Verified on the TV.
 */
final class ChannelRowsSource implements SuggestionSource {
    private final ContentResolver resolver;
    private final String packageName;
    private final String tileId;
    private final List<String> preferredChannels;
    private final int max;

    ChannelRowsSource(ContentResolver resolver, String packageName, String tileId, List<String> preferredChannels, int max) {
        this.resolver = resolver;
        this.packageName = packageName;
        this.tileId = tileId;
        this.preferredChannels = preferredChannels;
        this.max = max;
    }

    @Override public String tileId() { return tileId; }

    @Override
    public List<Suggestion> load() throws IOException {
        try {
            Map<Long, String> names = new HashMap<>();
            // No selection clause: the TV provider refuses one from a caller without the all-data permission,
            // so every row is read and filtered by package here.
            try (Cursor c = resolver.query(TvContract.Channels.CONTENT_URI,
                    new String[] {TvContract.Channels._ID, TvContract.Channels.COLUMN_DISPLAY_NAME,
                            TvContract.Channels.COLUMN_PACKAGE_NAME}, null, null, null)) {
                while (c != null && c.moveToNext()) {
                    if (packageName.equals(c.getString(2))) names.put(c.getLong(0), c.getString(1));
                }
            }
            List<ChannelRow> rows = new ArrayList<>();
            try (Cursor c = resolver.query(TvContract.PreviewPrograms.CONTENT_URI, new String[] {
                    TvContract.PreviewPrograms.COLUMN_CHANNEL_ID, TvContract.PreviewPrograms.COLUMN_TITLE,
                    TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI, TvContract.PreviewPrograms.COLUMN_INTENT_URI,
                    TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME}, null, null, null)) {
                while (c != null && c.moveToNext()) {
                    if (!packageName.equals(c.getString(4))) continue;
                    rows.add(new ChannelRow(names.get(c.getLong(0)), c.getString(1), c.getString(2), c.getString(3)));
                }
            }
            return ChannelRowsMapper.map(rows, tileId, preferredChannels, max);
        } catch (SecurityException e) {
            throw new IOException("No access to TV listings");
        }
    }
}
