package family.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends Activity {
    private static final int MAX_SUGGESTIONS = 20;

    private final HomeSelection selection = new HomeSelection(Tiles.ALL.size());
    private final List<View> tileViews = new ArrayList<>();
    private final List<ImageView> tileImages = new ArrayList<>();
    private final List<TextView> tileLabels = new ArrayList<>();
    private final List<View> cardViews = new ArrayList<>();
    private final List<Suggestion> shown = new ArrayList<>();
    private LinearLayout tileRow;
    private LinearLayout suggestionRow;
    private View suggestionScroll;
    private SuggestionRepository repository;
    private ThumbnailLoader thumbnails;
    private String requestedTileId;
    private TextView greeting;
    private TextView clock;
    private TextView amPm;
    private TextView date;
    private boolean receiverRegistered;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshHeader(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        greeting = findViewById(R.id.greeting);
        clock = findViewById(R.id.clock);
        amPm = findViewById(R.id.amPm);
        date = findViewById(R.id.date);
        tileRow = findViewById(R.id.tileRow);
        suggestionRow = findViewById(R.id.suggestionRow);
        suggestionScroll = findViewById(R.id.suggestionScroll);
        buildTiles();
        wireHeader();
        buildSuggestions();
        selection.onColdStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHeader();
        refreshTileAvailability();
        registerReceiver(tick, new IntentFilter(Intent.ACTION_TIME_TICK));
        receiverRegistered = true;
        tileViews.get(selection.current()).requestFocus();
        requestSuggestions(Tiles.ALL.get(selection.current()).id);
    }

    @Override
    protected void onPause() {
        if (receiverRegistered) {
            unregisterReceiver(tick);
            receiverRegistered = false;
        }
        super.onPause();
    }

    /** Home is the only screen; Back does nothing so the TV never lands on another launcher. */
    @Override
    public void onBackPressed() {}

    private void buildTiles() {
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < Tiles.ALL.size(); i++) {
            final int index = i;
            final Tile tile = Tiles.ALL.get(i);
            FrameLayout v = new FrameLayout(this);
            ImageView art = new ImageView(this);
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        v.addView(art, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            TextView label = new TextView(this);
            label.setText(tile.label);
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(20);
            label.setGravity(Gravity.CENTER);
            label.setVisibility(View.GONE);
            v.addView(label, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            tileImages.add(art);
            tileLabels.add(label);
            v.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    (int) (TileRowSizer.heightDp(TileRowSizer.Size.FULL) * d), 1f);
            lp.setMargins((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
            tileRow.addView(v, lp);
            tileViews.add(v);
            v.setOnFocusChangeListener((view, hasFocus) -> {
                view.animate().scaleX(hasFocus ? 1.08f : 1f).scaleY(hasFocus ? 1.08f : 1f).setDuration(120).start();
                if (!hasFocus) return;
                selection.select(index);
                if (!tile.id.equals(requestedTileId)) requestSuggestions(tile.id);
            });
            v.setOnClickListener(view -> openTile(tile));
            v.setOnKeyListener((view, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_DPAD_DOWN || event.getAction() != KeyEvent.ACTION_DOWN) return false;
                for (View card : cardViews) {
                    if (card.getVisibility() == View.VISIBLE && suggestionScroll.getVisibility() == View.VISIBLE) {
                        card.requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void buildSuggestions() {
        Executor background = pool();
        // Pictures get their own threads so a row's downloads never hold up the next tile's request.
        thumbnails = new ThumbnailLoader(pool(), ThumbnailBudget.bytes(Runtime.getRuntime().maxMemory()));
        Map<String, SuggestionSource> sources = new HashMap<>();
        SuggestionSource popular = new YouTubeSource(new UrlConnectionFetcher(), BuildConfig.YOUTUBE_API_KEY,
                BuildConfig.REGION_CODE, MAX_SUGGESTIONS);
        // The signed-in user's own recommended rows when the TV lets us read them, else the public popular chart.
        sources.put("youtube", new FallbackSource(new ChannelRowsSource(getContentResolver(),
                "com.google.android.youtube.tv", "youtube", Arrays.asList("Recommended for you", "Trending"),
                MAX_SUGGESTIONS), popular));
        sources.put("netflix", new ChannelRowsSource(getContentResolver(), "com.netflix.ninja", "netflix",
                Arrays.asList("Netflix"), MAX_SUGGESTIONS));
        Handler mainHandler = new Handler(Looper.getMainLooper());
        repository = new SuggestionRepository(sources, new SuggestionCache(new java.io.File(getCacheDir(), "suggestions")),
                background, mainHandler::post, System::currentTimeMillis);
        float d = getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            final int index = i;
            View card = inflater.inflate(R.layout.suggestion_card, suggestionRow, false);
            card.setBackgroundResource(R.drawable.glass_card);
            card.setOnClickListener(v -> openSuggestion(index));
            card.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
                    tileViews.get(selection.current()).requestFocus();
                    return true;
                }
                return false;
            });
            suggestionRow.addView(card);
            cardViews.add(card);
        }
    }

    /** A small pool that gives its threads back when idle, to keep the launcher light. */
    private static Executor pool() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    private void requestSuggestions(String tileId) {
        if (!tileId.equals(requestedTileId)) {
            ((android.widget.HorizontalScrollView) suggestionScroll).scrollTo(0, 0);
            // The old tile's row stays on screen until the new reply; show it as stale and keep focus out of it.
            suggestionScroll.setAlpha(0.3f);
            ((android.view.ViewGroup) suggestionScroll).setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        requestedTileId = tileId;
        repository.request(tileId, (requestId, id, items, fromCache) -> showSuggestions(items));
    }

    private void showSuggestions(List<Suggestion> items) {
        List<Suggestion> next = new ArrayList<>(items.size() > MAX_SUGGESTIONS ? items.subList(0, MAX_SUGGESTIONS) : items);
        suggestionScroll.setAlpha(1f);
        ((android.view.ViewGroup) suggestionScroll).setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // Fresh results often equal the cached row; rebuilding it would blink the pictures and disturb browsing.
        if (!shown.isEmpty() && SuggestionRow.sameItems(shown, next)) return;
        int focusedCard = -1;
        for (int i = 0; i < cardViews.size(); i++) if (cardViews.get(i).hasFocus()) focusedCard = i;
        shown.clear();
        shown.addAll(next);
        boolean any = !shown.isEmpty();
        suggestionScroll.setVisibility(any ? View.VISIBLE : View.GONE);
        for (int i = 0; i < cardViews.size(); i++) {
            View card = cardViews.get(i);
            if (i < shown.size()) {
                card.setVisibility(View.VISIBLE);
                ((TextView) card.findViewById(R.id.cardTitle)).setText(shown.get(i).title);
                thumbnails.load(shown.get(i).thumbnailUrl, (ImageView) card.findViewById(R.id.cardImage));
            } else {
                card.setVisibility(View.GONE);
                ImageView unused = card.findViewById(R.id.cardImage);
                unused.setTag(null);
                unused.setImageDrawable(null);
            }
        }
        float d = getResources().getDisplayMetrics().density;
        int height = (int) (TileRowSizer.heightDp(TileRowSizer.forSuggestions(any)) * d);
        for (View tile : tileViews) tile.getLayoutParams().height = height;
        tileRow.requestLayout();
        // A card that no longer exists would drop focus somewhere arbitrary; keep it near where the viewer was.
        if (focusedCard >= 0) {
            int target = SuggestionRow.focusAfterUpdate(focusedCard, shown.size());
            (target >= 0 ? cardViews.get(target) : tileViews.get(selection.current())).requestFocus();
        }
    }

    private void openSuggestion(int index) {
        if (index >= shown.size()) return;
        Suggestion item = shown.get(index);
        String pkg = null;
        for (Tile t : Tiles.ALL) if (t.id.equals(item.tileId)) pkg = t.plans.get(0).packageName;
        if (pkg == null) return;
        Intent intent = PublishedUri.isPublished(item.openUri) ?IntentFactory.fromPublishedUri(item.openUri, pkg)
                : IntentFactory.toIntent(getPackageManager(), LaunchPlan.view(pkg, item.openUri));
        SafeStart.start(this, intent, getString(R.string.not_available));
    }

    private void wireHeader() {
        findViewById(R.id.btnInputs).setOnClickListener(v -> {
            if (!InputsMenu.show(this)) launchFirstResolvable(HeaderAction.INPUTS.plans());
        });
        findViewById(R.id.btnNetwork).setOnClickListener(v -> launchFirstResolvable(HeaderAction.NETWORK.plans()));
        findViewById(R.id.btnSettings).setOnClickListener(v -> launchFirstResolvable(HeaderAction.SETTINGS.plans()));
        findViewById(R.id.btnApps).setOnClickListener(v -> launchFirstResolvable(HeaderAction.APPS.plans()));
    }

    private void launchFirstResolvable(List<LaunchPlan> plans) {
        Tile pseudo = new Tile("header", "", 0, plans);
        Optional<LaunchPlan> plan = TileLauncher.resolve(pseudo, IntentFactory.checker(getPackageManager()));
        if (!plan.isPresent()) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        SafeStart.start(this, IntentFactory.toIntent(getPackageManager(), plan.get()), getString(R.string.not_available));
    }

    private void openTile(Tile tile) {
        Optional<LaunchPlan> plan = TileLauncher.resolve(tile, IntentFactory.checker(getPackageManager()));
        if (!plan.isPresent()) {
            Toast.makeText(this, getString(R.string.tile_not_available, tile.label), Toast.LENGTH_SHORT).show();
            return;
        }
        SafeStart.start(this, IntentFactory.toIntent(getPackageManager(), plan.get()),
                getString(R.string.tile_not_available, tile.label));
    }

    private void refreshTileAvailability() {
        PackageManager pm = getPackageManager();
        for (int i = 0; i < Tiles.ALL.size(); i++) {
            Tile tile = Tiles.ALL.get(i);
            Optional<LaunchPlan> plan = TileLauncher.resolve(tile, IntentFactory.checker(pm));
            tileViews.get(i).setAlpha(plan.isPresent() ? 1f : 0.4f);
            Drawable art = plan.isPresent() ? appArt(pm, plan.get()) : null;
            tileImages.get(i).setImageDrawable(art);
            tileLabels.get(i).setVisibility(art == null ? View.VISIBLE : View.GONE);
        }
    }

    /** The launched activity's TV banner, else the app's banner, else its icon: no logos are bundled here. */
    private static Drawable appArt(PackageManager pm, LaunchPlan plan) {
        try {
            Intent intent = IntentFactory.toIntent(pm, plan);
            if (intent != null && intent.getComponent() != null) {
                Drawable b = pm.getActivityBanner(intent.getComponent());
                if (b != null) return b;
            }
            Drawable b = pm.getApplicationBanner(plan.packageName);
            if (b != null) return b;
            return pm.getApplicationIcon(plan.packageName);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    /** Always shows the time, whatever the system clock says. */
    private void refreshHeader() {
        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        ClockText text = ClockText.of(now, zone, DateFormat.is24HourFormat(this), Locale.getDefault());
        greeting.setText(Greeting.forHour(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).getHour()));
        clock.setText(text.time);
        amPm.setText(text.amPm);
        amPm.setVisibility(text.amPm.isEmpty() ? View.GONE : View.VISIBLE);
        date.setText(text.date);
    }
}
