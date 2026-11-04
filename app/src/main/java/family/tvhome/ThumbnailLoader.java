package family.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

/** Small thumbnails, decoded at about 320x180 and kept in a byte-limited cache. */
final class ThumbnailLoader {
    private static final int TARGET_W = 320;
    private static final int TARGET_H = 180;
    private static final int MAX_DOWNLOAD_BYTES = 512 * 1024;

    private final Executor background;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache;

    ThumbnailLoader(Executor background, long budgetBytes) {
        this.background = background;
        this.cache = new LruCache<String, Bitmap>((int) Math.max(1L, budgetBytes)) {
            @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
        };
    }

    void load(String url, ImageView target) {
        target.setTag(url);
        target.setImageDrawable(null);
        if (!ThumbnailUrl.isFetchable(url)) return;
        Bitmap hit = cache.get(url);
        if (hit != null) {
            target.setImageBitmap(hit);
            return;
        }
        background.execute(() -> {
            Bitmap bitmap = fetch(url);
            if (bitmap == null) return;
            cache.put(url, bitmap);
            main.post(() -> {
                if (url.equals(target.getTag())) target.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap fetch(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_DOWNLOAD_BYTES) return null;
                }
            }
            byte[] data = out.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = SampleSize.forTarget(bounds.outWidth, bounds.outHeight, TARGET_W, TARGET_H);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (IOException | RuntimeException e) {
            // A bad link or a decoder complaint costs one picture, never the launcher.
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
