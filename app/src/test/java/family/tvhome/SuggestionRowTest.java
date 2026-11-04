package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SuggestionRowTest {
    private static Suggestion s(String title, String uri) { return new Suggestion("youtube", title, "https://i/" + title, uri); }

    private static List<Suggestion> list(Suggestion... items) { return new ArrayList<>(Arrays.asList(items)); }

    @Test public void identicalListsAreTheSame() {
        assertTrue(SuggestionRow.sameItems(list(s("a", "u1"), s("b", "u2")), list(s("a", "u1"), s("b", "u2"))));
    }

    @Test public void differentTitleLinkThumbnailOrLengthIsNotTheSame() {
        List<Suggestion> base = list(s("a", "u1"), s("b", "u2"));
        assertFalse(SuggestionRow.sameItems(base, list(s("a", "u1"), s("c", "u2"))));
        assertFalse(SuggestionRow.sameItems(base, list(s("a", "u1"), s("b", "other"))));
        assertFalse(SuggestionRow.sameItems(base, list(s("a", "u1"))));
        assertFalse(SuggestionRow.sameItems(list(new Suggestion("youtube", "a", "https://x", "u")), list(new Suggestion("youtube", "a", "https://y", "u"))));
    }

    @Test public void noFocusOnTheRowStaysNoFocus() {
        assertEquals(-1, SuggestionRow.focusAfterUpdate(-1, 5));
    }

    @Test public void focusStaysWhenTheCardStillExists() {
        assertEquals(3, SuggestionRow.focusAfterUpdate(3, 5));
    }

    @Test public void focusMovesToTheLastCardWhenTheRowShrinks() {
        assertEquals(1, SuggestionRow.focusAfterUpdate(7, 2));
    }

    @Test public void focusHasNowhereToGoWhenTheRowIsEmpty() {
        assertEquals(-1, SuggestionRow.focusAfterUpdate(2, 0));
        assertEquals(-1, SuggestionRow.focusAfterUpdate(2, Collections.<Suggestion>emptyList().size()));
    }
}
