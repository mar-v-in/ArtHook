package de.larma.arthook;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public enum Arch {
    ARM32, THUMB2, ARM64, x86, x86_64, MIPS, MIPS64;

    public static List<Arch> getArchitectures() {
        final List<Arch> archs = new ArrayList<>();
        final String[] supportedAbis = getSupportedAbis();
        for (String abi : supportedAbis) {
            if ("arm64-v8a".equals(abi) && Native.is64Bit()) {
                archs.add(ARM64);
            } else if ("armeabi-v7a".equals(abi) || "armeabi".equals(abi)) {
                if (isThumb2()) {
                    archs.add(THUMB2);
                } else {
                    archs.add(ARM32);
                }
            } else if ("x86".equals(abi)) {
                archs.add(x86);
            } else if ("x86_64".equals(abi) && Native.is64Bit()) {
                archs.add(x86_64);
            } else if ("mips".equals(abi)) {
                archs.add(MIPS);
            } else if ("mips64".equals(abi) && Native.is64Bit()) {
                archs.add(MIPS64);
            }
        }
        return archs;
    }

    private static boolean isThumb2() {
        try {
            return (ArtMethod.of(ArtMethod.class.getDeclaredMethod("of", Method.class)).getEntryPointFromQuickCompiledCode() & 1) == 1;
        } catch (NoSuchMethodException e) {
            // Should never happen
            throw new LibArtError("Unable to check isThumb2.", e);
        }
    }

    private static String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else if (Build.CPU_ABI != null && Build.CPU_ABI2 != null) {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        } else if (Build.CPU_ABI != null) {
            return new String[]{Build.CPU_ABI};
        } else if (Build.CPU_ABI2 != null) {
            return new String[]{Build.CPU_ABI2};
        }
        return new String[0];
    }
}
