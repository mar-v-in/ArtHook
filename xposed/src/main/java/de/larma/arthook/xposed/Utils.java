package de.larma.arthook.xposed;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;

public final class Utils {
    private Utils() {
    }

    public static void callMain(String className, String... args) throws Throwable {
        try {
            Class.forName(className).getDeclaredMethod("main", String[].class).invoke(null,
                    new Object[]{args});
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
