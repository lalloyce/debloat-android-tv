package family.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class InputEntry {
    final String inputId;
    final String label;

    InputEntry(String inputId, String label) {
        this.inputId = inputId;
        this.label = label;
    }

    static List<InputEntry> passthrough(List<InputEntry> all, Set<String> hiddenIds) {
        List<InputEntry> out = new ArrayList<>();
        for (InputEntry e : all) {
            if (!hiddenIds.contains(e.inputId)) out.add(e);
        }
        return out;
    }
}
