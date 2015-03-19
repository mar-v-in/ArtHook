package de.larma.arthook.xposed;

public final class ZygoteInit {
    private ZygoteInit() {
    }

    public static void main(String[] args) throws Throwable {
        Xposed.main(true, args);
        Utils.callMain("com.android.internal.os.ZygoteInit", args);
    }
}
