package family.tvhome;

import java.util.ArrayList;
import java.util.List;

final class ChannelRowsMapper {
    private ChannelRowsMapper() {}

    /**
     * Usable rows (title and intent present): those of the preferred channels, joined in preferred order, or, when
     * none of them has any, every channel's rows in order. Cut to max. Only thumbnails the loader can fetch (https) are kept.
     */
    static List<Suggestion> map(List<ChannelRow> rows, String tileId, List<String> preferredChannels, int max) {
        List<ChannelRow> usable = new ArrayList<>();
        for (ChannelRow r : rows) {
            if (r.title != null && !r.title.isEmpty() && r.intentUri != null && !r.intentUri.isEmpty()) usable.add(r);
        }
        List<ChannelRow> chosen = new ArrayList<>();
        for (String channel : preferredChannels) {
            for (ChannelRow r : usable) if (channel.equals(r.channelName)) chosen.add(r);
        }
        if (chosen.isEmpty()) chosen = usable;
        List<Suggestion> out = new ArrayList<>();
        for (ChannelRow r : chosen) {
            if (out.size() >= max) break;
            out.add(new Suggestion(tileId, r.title, fetchableOnly(r.thumbnailUrl), r.intentUri));
        }
        return out;
    }

    private static String fetchableOnly(String url) {
        return ThumbnailUrl.isFetchable(url) ? url : "";
    }
}
