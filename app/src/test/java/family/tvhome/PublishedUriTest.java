package family.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PublishedUriTest {
    // Shapes copied from what the TV's apps publish (the TV provider's published channel rows).
    private static final String YOUTUBE =
            "intent://www.youtube.com/watch?v=u-N3_sNv-cg&launch=launcher#Intent;scheme=https;package=com.google.android.youtube.tv;end";
    private static final String NETFLIX =
            "http://www.netflix.com/title/81598066#Intent;launchFlags=0x20;component=com.netflix.ninja/.MainActivity;end";

    @Test public void intentSchemeIsPublished() { assertTrue(PublishedUri.isPublished(YOUTUBE)); }
    @Test public void fragmentFormIsPublished() { assertTrue(PublishedUri.isPublished(NETFLIX)); }
    @Test public void plainLinkIsNotPublished() { assertFalse(PublishedUri.isPublished("https://www.youtube.com/watch?v=abc")); }

    @Test public void intentSchemeNeedsTheSchemeFlag() { assertTrue(PublishedUri.usesSchemeFlag(YOUTUBE)); }
    @Test public void fragmentFormMustNotUseTheSchemeFlag() { assertFalse(PublishedUri.usesSchemeFlag(NETFLIX)); }
}
