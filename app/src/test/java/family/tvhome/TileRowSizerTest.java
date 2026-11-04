package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TileRowSizerTest {
    @Test public void suggestionsShrinkTheRow() {
        assertEquals(TileRowSizer.Size.COMPACT, TileRowSizer.forSuggestions(true));
    }

    @Test public void noSuggestionsKeepsFullSize() {
        assertEquals(TileRowSizer.Size.FULL, TileRowSizer.forSuggestions(false));
    }

    @Test public void heightsAreThePlaceholders() {
        assertEquals(200, TileRowSizer.heightDp(TileRowSizer.Size.FULL));
        assertEquals(128, TileRowSizer.heightDp(TileRowSizer.Size.COMPACT));
    }
}
