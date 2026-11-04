package family.tvhome;

final class AppEntry {
    final String label;
    final String packageName;
    final String className;

    AppEntry(String label, String packageName, String className) {
        this.label = label;
        this.packageName = packageName;
        this.className = className;
    }
}
