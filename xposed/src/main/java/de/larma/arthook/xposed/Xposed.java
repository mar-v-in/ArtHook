package de.larma.arthook.xposed;

import android.os.Build;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

public final class Xposed {

    private static final String TAG = "ArtHook.Xposed";

    private Xposed() {
    }

    public static void main(boolean zygote, String[] args) {
        try {
            init(zygote);
        } catch (Throwable t) {
            Log.w(TAG, t);
        }
    }

    private static void init(boolean zygote) throws Throwable {
        String startClassName = "unknown"; //Native.getStartClassName();
        String date = DateFormat.getDateTimeInstance().format(new Date());
        Log.d(TAG, "-----------------\n" + date + " UTC\n" + "Loading ArtHook module" +
                " (for " + (zygote ? "Zygote" : startClassName) + ")...\n");
        if (zygote)
            Log.d(TAG, "Running ROM '" + Build.DISPLAY + "' with fingerprint '" + Build
                    .FINGERPRINT + "'");
        if (de.larma.arthook.Native.mmap_verbose(1) == 0) {
            Log.d(TAG, "Blocked by SELINUX :(");
        }
        // TODO
    }
}
