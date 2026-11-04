package family.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LaunchPlanTest {
    @Test public void viewPlanCarriesPackageAndUri() {
        LaunchPlan p = LaunchPlan.view("p", "https://x");
        assertEquals("android.intent.action.VIEW", p.action);
        assertEquals("p", p.packageName);
        assertEquals("https://x", p.dataUri);
        assertNull(p.className);
    }

    @Test public void componentPlanCarriesClass() {
        LaunchPlan p = LaunchPlan.component("pkg", "pkg.Main");
        assertEquals("pkg", p.packageName);
        assertEquals("pkg.Main", p.className);
        assertNull(p.action);
    }

    @Test public void leanbackPlanHasOnlyPackage() {
        LaunchPlan p = LaunchPlan.leanback("pkg");
        assertEquals("pkg", p.packageName);
        assertNull(p.className);
        assertNull(p.action);
    }

    @Test public void actionPlanHasOnlyAction() {
        LaunchPlan p = LaunchPlan.action("a.b.C");
        assertEquals("a.b.C", p.action);
        assertNull(p.packageName);
    }
}
