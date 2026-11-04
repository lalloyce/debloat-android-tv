package family.tvhome;

import java.util.Optional;

final class TileLauncher {
    interface PackageChecker {
        boolean canLaunch(LaunchPlan plan);
    }

    private TileLauncher() {}

    static Optional<LaunchPlan> resolve(Tile tile, PackageChecker checker) {
        for (LaunchPlan plan : tile.plans) {
            if (checker.canLaunch(plan)) return Optional.of(plan);
        }
        return Optional.empty();
    }
}
