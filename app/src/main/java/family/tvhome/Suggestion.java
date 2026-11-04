package family.tvhome;

final class Suggestion {
    final String tileId;
    final String title;
    final String thumbnailUrl;
    final String openUri;

    Suggestion(String tileId, String title, String thumbnailUrl, String openUri) {
        this.tileId = tileId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.openUri = openUri;
    }
}
