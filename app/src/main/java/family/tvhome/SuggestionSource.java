package family.tvhome;

import java.io.IOException;
import java.util.List;

interface SuggestionSource {
    String tileId();

    List<Suggestion> load() throws IOException;
}
