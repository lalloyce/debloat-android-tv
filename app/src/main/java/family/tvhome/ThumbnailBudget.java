package family.tvhome;

final class ThumbnailBudget {
    private static final long CAP = 4L * 1024L * 1024L;

    private ThumbnailBudget() {}

    /** A twentieth of the app's heap limit, at most 4 MiB, so thumbnails never crowd out the 0.9 GB TV. */
    static long bytes(long maxMemoryBytes) {
        return Math.max(0L, Math.min(maxMemoryBytes / 20L, CAP));
    }
}
