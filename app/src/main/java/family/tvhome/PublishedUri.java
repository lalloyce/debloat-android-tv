package family.tvhome;

/** Tells the two link shapes apps publish in their rows apart. Verified on the TV. */
final class PublishedUri {
    private PublishedUri() {}

    /** True for "intent:" links (YouTube) and for "http://...#Intent;...;end" links (Netflix). */
    static boolean isPublished(String uri) {
        return uri.startsWith("intent:") || uri.contains("#Intent;");
    }

    /**
     * Intent.parseUri with URI_INTENT_SCHEME reads only "intent:" links; given any other link it returns a plain
     * view of it and ignores the "#Intent;" part, so those must be parsed with no flag.
     */
    static boolean usesSchemeFlag(String uri) {
        return uri.startsWith("intent:");
    }
}
