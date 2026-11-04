package family.tvhome;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

/** Android side of LaunchPlan: builds intents and checks that a plan can start. Verified on the TV. */
final class IntentFactory {
    private IntentFactory() {}

    /** Null when the launch plan names a leanback package that has no launch intent. */
    static Intent toIntent(PackageManager pm, LaunchPlan plan) {
        Intent intent;
        if (plan.action == null && plan.className == null && plan.packageName != null) {
            intent = pm.getLeanbackLaunchIntentForPackage(plan.packageName);
            if (intent == null) intent = pm.getLaunchIntentForPackage(plan.packageName);
            if (intent == null) return null;
        } else if (plan.className != null) {
            intent = new Intent().setComponent(new ComponentName(plan.packageName, plan.className));
        } else {
            intent = new Intent(plan.action);
            if (plan.packageName != null) intent.setPackage(plan.packageName);
        }
        if (plan.dataUri != null) intent.setData(Uri.parse(plan.dataUri));
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * An intent from an "intent:" style URI that another app published, or null unless it targets expectedPackage.
     * Rows come from other apps, so nothing else is ever started from them.
     */
    static Intent fromPublishedUri(String uri, String expectedPackage) {
        try {
            Intent intent = Intent.parseUri(uri, PublishedUri.usesSchemeFlag(uri) ? Intent.URI_INTENT_SCHEME : 0);
            String target = intent.getComponent() != null ? intent.getComponent().getPackageName() : intent.getPackage();
            if (!expectedPackage.equals(target)) return null;
            intent.setSelector(null);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }

    static TileLauncher.PackageChecker checker(PackageManager pm) {
        return plan -> {
            if (plan.packageName != null && !isEnabled(pm, plan.packageName)) return false;
            Intent intent = toIntent(pm, plan);
            return intent != null && intent.resolveActivity(pm) != null;
        };
    }

    private static boolean isEnabled(PackageManager pm, String pkg) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
