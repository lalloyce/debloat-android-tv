package family.tvhome;

/** Which thumbnail links the loader will download: https only, matching the app's no-cleartext policy. */
final class ThumbnailUrl {
    private ThumbnailUrl() {}

    static boolean isFetchable(String url) {
        return url != null && url.startsWith("https://");
    }
}
