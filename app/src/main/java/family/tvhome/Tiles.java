package family.tvhome;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class Tiles {
    private Tiles() {}

    static final List<Tile> ALL = Collections.unmodifiableList(Arrays.asList(
            new Tile("youtube", "YouTube", 0xFFC4302B,
                    Collections.singletonList(LaunchPlan.leanback("com.google.android.youtube.tv"))),
            new Tile("netflix", "Netflix", 0xFF222222,
                    Collections.singletonList(LaunchPlan.leanback("com.netflix.ninja"))),
            new Tile("tclmovies", "TCL Movies", 0xFF1D3A5F,
                    Collections.singletonList(LaunchPlan.component(
                            "com.tcl.waterfall.overseas", "com.tcl.waterfall.overseas.main.MainActivity"))),
            new Tile("tv", "TV", 0xFF2C4A3A,
                    Collections.singletonList(LaunchPlan.component("com.tcl.tv", "com.tcl.tv.TVActivity")))));
}
