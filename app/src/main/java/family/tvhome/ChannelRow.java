package family.tvhome;

/** One program row published by an app to the TV's shared channel data, as plain data. */
final class ChannelRow {
    final String channelName;
    final String title;
    final String thumbnailUrl;
    final String intentUri;

    ChannelRow(String channelName, String title, String thumbnailUrl, String intentUri) {
        this.channelName = channelName;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.intentUri = intentUri;
    }
}
