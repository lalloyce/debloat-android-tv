package family.tvhome;

/** What to start, as plain data so it can be tested without Android. All fields may be null. */
final class LaunchPlan {
    static final String ACTION_VIEW = "android.intent.action.VIEW";

    final String action;
    final String packageName;
    final String className;
    final String dataUri;

    private LaunchPlan(String action, String packageName, String className, String dataUri) {
        this.action = action;
        this.packageName = packageName;
        this.className = className;
        this.dataUri = dataUri;
    }

    static LaunchPlan leanback(String pkg) { return new LaunchPlan(null, pkg, null, null); }

    static LaunchPlan component(String pkg, String cls) { return new LaunchPlan(null, pkg, cls, null); }

    static LaunchPlan action(String action) { return new LaunchPlan(action, null, null, null); }

    static LaunchPlan view(String pkg, String dataUri) { return new LaunchPlan(ACTION_VIEW, pkg, null, dataUri); }
}
