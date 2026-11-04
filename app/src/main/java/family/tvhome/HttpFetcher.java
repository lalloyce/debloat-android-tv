package family.tvhome;

import java.io.IOException;

interface HttpFetcher {
    String get(String url) throws IOException;
}
