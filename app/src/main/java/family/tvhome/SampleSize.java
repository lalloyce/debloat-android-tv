package family.tvhome;

final class SampleSize {
    private SampleSize() {}

    /** The largest power of two that keeps both decoded dimensions at or above the target; 1 for bad input. */
    static int forTarget(int srcW, int srcH, int targetW, int targetH) {
        if (srcW <= 0 || srcH <= 0 || targetW <= 0 || targetH <= 0) return 1;
        int sample = 1;
        while (srcW / (sample * 2) >= targetW && srcH / (sample * 2) >= targetH) sample *= 2;
        return sample;
    }
}
