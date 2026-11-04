package family.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** A plain list of every installed app, so nothing needs to live on the home screen. */
public class AppsActivity extends Activity {
    private PackageManager pm;
    private List<AppEntry> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        pm = getPackageManager();
        apps = AppList.build(query(), getPackageName());
        ListView list = findViewById(R.id.appList);
        list.setAdapter(new Adapter());
        list.setOnItemClickListener((parent, view, position, id) -> open(apps.get(position)));
        list.requestFocus();
    }

    private List<AppEntry> query() {
        List<AppEntry> raw = new ArrayList<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                raw.add(new AppEntry(String.valueOf(ri.loadLabel(pm)), ri.activityInfo.packageName, ri.activityInfo.name));
            }
        }
        return raw;
    }

    private void open(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(app.packageName, app.className)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SafeStart.start(this, intent, getString(R.string.not_available));
    }

    private Drawable icon(AppEntry app) {
        try {
            return pm.getActivityIcon(new android.content.ComponentName(app.packageName, app.className));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            ImageView image;
            TextView text;
            if (convertView == null) {
                float d = getResources().getDisplayMetrics().density;
                row = new LinearLayout(AppsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBackgroundResource(R.drawable.glass_row);
                row.setPadding((int) (24 * d), (int) (8 * d), (int) (24 * d), (int) (8 * d));
                image = new ImageView(AppsActivity.this);
                image.setId(android.R.id.icon);
                row.addView(image, new LinearLayout.LayoutParams((int) (40 * d), (int) (40 * d)));
                text = new TextView(AppsActivity.this);
                text.setId(android.R.id.text1);
                text.setTextColor(0xFFFFFFFF);
                text.setTextSize(20);
                text.setPadding((int) (16 * d), 0, 0, 0);
                text.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.addView(text, new LinearLayout.LayoutParams(0, (int) (40 * d), 1f));
            } else {
                row = (LinearLayout) convertView;
                image = row.findViewById(android.R.id.icon);
                text = row.findViewById(android.R.id.text1);
            }
            AppEntry app = apps.get(position);
            image.setImageDrawable(icon(app));
            text.setText(app.label);
            return row;
        }
    }
}
