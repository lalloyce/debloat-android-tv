package family.tvhome;

import java.util.List;
import java.util.Objects;

/** Decisions about replacing the suggestion row while someone may be browsing it. */
final class SuggestionRow {
    private SuggestionRow() {}

    /** True when both lists show the same cards in the same order, so the row need not be rebuilt. */
    static boolean sameItems(List<Suggestion> a, List<Suggestion> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Suggestion x = a.get(i), y = b.get(i);
            if (!Objects.equals(x.title, y.title) || !Objects.equals(x.openUri, y.openUri)
                    || !Objects.equals(x.thumbnailUrl, y.thumbnailUrl)) return false;
        }
        return true;
    }

    /** Which card should hold focus after the row is replaced: the same position, else the last card; -1 for none. */
    static int focusAfterUpdate(int focusedIndex, int newSize) {
        if (focusedIndex < 0 || newSize <= 0) return -1;
        return Math.min(focusedIndex, newSize - 1);
    }
}
