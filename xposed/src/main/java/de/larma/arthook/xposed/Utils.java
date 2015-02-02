package de.larma.arthook.xposed;

import android.util.Log;

public final class Utils {
    private Utils() {
    }

    public static void callMain(String className, String... args) {
        try {
            Class.forName(className).getDeclaredMethod("main", String[].class).invoke(null,
                    new Object[]{args});
        } catch (Exception e) {
            Log.w("Xposed", e);
        }
    }
}
