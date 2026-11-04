package family.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HeaderActionTest {
    @Test public void networkOpensWifiSettings() {
        assertEquals("android.settings.WIFI_SETTINGS", HeaderAction.NETWORK.plans().get(0).action);
    }

    @Test public void settingsOpensSystemSettings() {
        assertEquals("android.settings.SETTINGS", HeaderAction.SETTINGS.plans().get(0).action);
    }

    @Test public void appsOpensOurOwnList() {
        LaunchPlan p = HeaderAction.APPS.plans().get(0);
        assertEquals("family.tvhome", p.packageName);
        assertEquals("family.tvhome.AppsActivity", p.className);
    }

    @Test public void inputsFallbackIsTheTvApp() {
        assertEquals(1, HeaderAction.INPUTS.plans().size());
        LaunchPlan p = HeaderAction.INPUTS.plans().get(0);
        assertEquals("com.tcl.tv", p.packageName);
        assertEquals("com.tcl.tv.TVActivity", p.className);
    }
}
