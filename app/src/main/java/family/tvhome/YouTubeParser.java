package family.tvhome;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

final class YouTubeParser {
    private static final String[] THUMBNAIL_SIZES = {"medium", "high", "default"};

    private YouTubeParser() {}

    static List<Suggestion> parse(String json) throws IOException {
        List<Suggestion> out = new ArrayList<>();
        try {
            org.json.JSONArray items = new JSONObject(json).optJSONArray("items");
            if (items == null) return out;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                JSONObject snippet = item.optJSONObject("snippet");
                String title = snippet == null ? "" : snippet.optString("title", "");
                if (id.isEmpty() || title.isEmpty()) continue;
                out.add(new Suggestion("youtube", title, thumbnail(snippet),
                        "https://www.youtube.com/watch?v=" + id));
            }
        } catch (JSONException e) {
            throw new IOException("Unreadable YouTube response");
        }
        return out;
    }

    private static String thumbnail(JSONObject snippet) {
        JSONObject thumbs = snippet.optJSONObject("thumbnails");
        if (thumbs == null) return "";
        for (String size : THUMBNAIL_SIZES) {
            JSONObject t = thumbs.optJSONObject(size);
            if (t != null && !t.optString("url", "").isEmpty()) return t.optString("url");
        }
        return "";
    }
}
