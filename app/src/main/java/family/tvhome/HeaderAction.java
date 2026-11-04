package family.tvhome;

import java.util.Collections;
import java.util.List;

enum HeaderAction {
    /** The Inputs icon opens InputsMenu; this is only the fallback when the TV lists no passthrough input. */
    INPUTS(LaunchPlan.component("com.tcl.tv", "com.tcl.tv.TVActivity")),
    NETWORK(LaunchPlan.action("android.settings.WIFI_SETTINGS")),
    SETTINGS(LaunchPlan.action("android.settings.SETTINGS")),
    APPS(LaunchPlan.component("family.tvhome", "family.tvhome.AppsActivity"));

    private final List<LaunchPlan> plans;

    HeaderAction(LaunchPlan plan) {
        this.plans = Collections.singletonList(plan);
    }

    List<LaunchPlan> plans() { return plans; }
}
