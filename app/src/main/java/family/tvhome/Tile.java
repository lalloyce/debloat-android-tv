package family.tvhome;

import java.util.List;

final class Tile {
    final String id;
    final String label;
    final int accent;
    final List<LaunchPlan> plans;

    Tile(String id, String label, int accent, List<LaunchPlan> plans) {
        this.id = id;
        this.label = label;
        this.accent = accent;
        this.plans = plans;
    }
}
