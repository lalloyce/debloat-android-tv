package family.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AppList {
    private AppList() {}

    static List<AppEntry> build(List<AppEntry> raw, String ownPackage) {
        Set<String> seen = new HashSet<>();
        List<AppEntry> out = new ArrayList<>();
        for (AppEntry e : raw) {
            if (e.packageName.equals(ownPackage)) continue;
            if (seen.add(e.packageName)) out.add(e);
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }
}
