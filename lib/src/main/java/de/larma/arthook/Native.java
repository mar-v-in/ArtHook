/*
 * Copyright 2014-2015 Marvin Wißfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.larma.arthook;

import static de.larma.arthook.DebugHelper.intHex;
import static de.larma.arthook.DebugHelper.logd;

public final class Native {

    static {
        System.loadLibrary("arthook_native");
    }

    private Native() {
    }

    public static native long mmap(int length);

    public static native void munmap(long address, int length);

    public static native void memcpy(long src, long dest, int length);

    public static native void memput(byte[] bytes, long dest);

    public static native byte[] memget(long src, int length);

    public static native void munprotect(long addr, long len);

    public static native void ptrace(int pid);

    private static Boolean sixtyFour;

    public static boolean is64Bit() {
        if (sixtyFour == null)
            try {
                sixtyFour = (Boolean) Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("is64Bit").invoke(Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("getRuntime").invoke(null));
            } catch (Exception e) {
                throw new RuntimeException("Can't determine int size number!", e);
            }
        return sixtyFour;
    }
}
