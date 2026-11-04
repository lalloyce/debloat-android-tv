package family.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Our own Inputs list, as CleanYourTV does: the system exposes no input picker we can open. Verified on the TV. */
final class InputsMenu {
    private InputsMenu() {}

    static List<InputEntry> load(Context context) {
        TvInputManager tim = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        List<InputEntry> all = new ArrayList<>();
        Set<String> hidden = new HashSet<>();
        if (tim == null) return all;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (!info.isPassthroughInput()) continue;
            all.add(new InputEntry(info.getId(), String.valueOf(info.loadLabel(context))));
            if (info.isHidden(context)) hidden.add(info.getId());
        }
        return InputEntry.passthrough(all, hidden);
    }

    /** Returns false when there is nothing to list, so the caller can use the fallback plan. */
    static boolean show(Context context) {
        List<InputEntry> inputs = load(context);
        if (inputs.isEmpty()) return false;
        String[] labels = new String[inputs.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = inputs.get(i).label;
        new AlertDialog.Builder(context, R.style.GlassDialog)
                .setTitle(R.string.inputs)
                .setItems(labels, (dialog, which) -> {
                    Uri uri = TvContract.buildChannelUriForPassthroughInput(inputs.get(which).inputId);
                    SafeStart.start(context, new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            context.getString(R.string.not_available));
                })
                .show();
        return true;
    }
}
