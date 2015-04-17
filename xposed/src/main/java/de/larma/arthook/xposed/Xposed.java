package de.larma.arthook.xposed;

import android.os.Build;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

import de.larma.arthook.ArtMethod;
import de.larma.arthook.Memory;

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
        Class seLinux = Class.forName("android.os.SELinux");
        boolean enforced = false;
        if ((Boolean) seLinux.getDeclaredMethod("isSELinuxEnabled").invoke(null) &&
                (Boolean) seLinux.getDeclaredMethod("isSELinuxEnforced").invoke(null)) {
            Log.d(TAG, "SELinux is enabled and enforcing. Trying to temporary disable enforcement!");
            if ((Boolean) seLinux.getDeclaredMethod("setSELinuxEnforce", boolean.class).invoke(null, false)) {
                enforced = true;
                Log.d(TAG, "Temporary removed SELinux enforcement!");
            } else {
                Log.d(TAG, "Removed SELinux enforcement failed, trying to continue anyway!");
            }
        }
        if (Memory.map(1) == 0) {
            Log.d(TAG, "Mapping memory rwx is blocked by SELinux :(");
        } else {
            Log.d(TAG, "we can mmap rwx!");
        }
        try {
            long addr = ArtMethod.of(Xposed.class.getDeclaredMethod("test")).getEntryPointFromQuickCompiledCode() - 1;
            Memory.unprotect(addr, 2);
            Log.d(TAG, "we can remove mprotect!");
        } catch (Exception e) {
            Log.d(TAG, "Removing mprotect from memory is blocked by SELinux :(");
        }
        if (enforced) {
            if ((Boolean) seLinux.getDeclaredMethod("setSELinuxEnforce", boolean.class).invoke(null, true)) {
                Log.d(TAG, "Reset SELinux enforcement");
            } else {
                Log.d(TAG, "Error resetting SELinux enforcement");
            }
        }
        // TODO
    }

    private static void test() {
        Log.d(TAG, "TEST");
    }
}
