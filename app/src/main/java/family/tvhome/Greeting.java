package family.tvhome;

final class Greeting {
    private Greeting() {}

    static String forHour(int hour) {
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour: " + hour);
        if (hour >= 5 && hour < 12) return "Good morning";
        if (hour >= 12 && hour < 17) return "Good afternoon";
        if (hour >= 17 && hour < 21) return "Good evening";
        return "Good night";
    }
}
