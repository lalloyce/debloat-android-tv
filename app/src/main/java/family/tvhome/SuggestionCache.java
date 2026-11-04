package family.tvhome;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The last good suggestion set per tile, one JSON file each. A bad or half-written file reads as "no cache". */
final class SuggestionCache {
    static final class Cached {
        final List<Suggestion> items;
        final long savedAtMillis;

        Cached(List<Suggestion> items, long savedAtMillis) {
            this.items = items;
            this.savedAtMillis = savedAtMillis;
        }
    }

    private final File dir;

    SuggestionCache(File dir) {
        this.dir = dir;
    }

    void save(String tileId, List<Suggestion> items, long savedAtMillis) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        try {
            JSONArray array = new JSONArray();
            for (Suggestion s : items) {
                array.put(new JSONObject()
                        .put("tileId", s.tileId)
                        .put("title", s.title)
                        .put("thumbnailUrl", s.thumbnailUrl)
                        .put("openUri", s.openUri));
            }
            byte[] bytes = new JSONObject().put("savedAt", savedAtMillis).put("items", array)
                    .toString().getBytes(StandardCharsets.UTF_8);
            File tmp = new File(dir, tileId + ".json.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(bytes);
                out.getFD().sync();
            }
            Files.move(tmp.toPath(), fileFor(tileId).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /** Null when the file is missing, unreadable or corrupt. */
    Cached load(String tileId) {
        File file = fileFor(tileId);
        if (!file.isFile()) return null;
        try {
            JSONObject root = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            JSONArray array = root.getJSONArray("items");
            List<Suggestion> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                items.add(new Suggestion(o.getString("tileId"), o.getString("title"),
                        o.getString("thumbnailUrl"), o.getString("openUri")));
            }
            return new Cached(items, root.getLong("savedAt"));
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    private File fileFor(String tileId) {
        return new File(dir, tileId + ".json");
    }
}
