package family.tvhome;

/** Which tile is selected. A cold start begins at the first tile; returning from an app keeps the same one. */
final class HomeSelection {
    private final int tileCount;
    private int current;

    HomeSelection(int tileCount) {
        this.tileCount = tileCount;
    }

    int current() { return current; }

    void select(int index) {
        if (index >= 0 && index < tileCount) current = index;
    }

    void onColdStart() { current = 0; }
}
