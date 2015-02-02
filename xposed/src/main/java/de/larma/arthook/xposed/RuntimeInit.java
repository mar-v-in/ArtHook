package de.larma.arthook.xposed;

public final class RuntimeInit {
    private RuntimeInit() {
    }

    public static void main(String[] args) {
        Xposed.main(false, args);
        Utils.callMain("com.android.internal.os.RuntimeInit", args);
    }
}
