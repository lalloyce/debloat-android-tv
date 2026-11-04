package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

public class TileLauncherTest {
    private final LaunchPlan first = LaunchPlan.leanback("a");
    private final LaunchPlan second = LaunchPlan.leanback("b");
    private final Tile tile = new Tile("t", "T", 0, Arrays.asList(first, second));

    @Test public void returnsEmptyWhenNothingLaunchable() {
        Optional<LaunchPlan> r = TileLauncher.resolve(tile, plan -> false);
        assertFalse(r.isPresent());
    }

    @Test public void skipsPlanTheCheckerRejects() {
        Optional<LaunchPlan> r = TileLauncher.resolve(tile, plan -> plan == second);
        assertTrue(r.isPresent());
        assertEquals(second, r.get());
    }

    @Test public void prefersTheFirstAcceptedPlan() {
        Optional<LaunchPlan> r = TileLauncher.resolve(tile, plan -> true);
        assertEquals(first, r.get());
    }
}
