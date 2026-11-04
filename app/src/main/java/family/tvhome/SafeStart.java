package family.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Starts another app's screen without ever taking the launcher down if that screen has just gone away. */
final class SafeStart {
    private SafeStart() {}

    static boolean start(Context context, Intent intent, String unavailableMessage) {
        if (intent == null) {
            Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
