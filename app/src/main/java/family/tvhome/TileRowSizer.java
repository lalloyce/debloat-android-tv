package family.tvhome;

final class TileRowSizer {
    enum Size { FULL, COMPACT }

    private TileRowSizer() {}

    static Size forSuggestions(boolean hasSuggestions) {
        return hasSuggestions ? Size.COMPACT : Size.FULL;
    }

    /** Placeholder heights; tune them on a real TV. */
    static int heightDp(Size size) {
        return size == Size.FULL ? 200 : 128;
    }
}
