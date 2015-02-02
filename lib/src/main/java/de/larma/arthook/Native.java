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

import android.util.Log;

public final class Native {
    private static final String TAG = "ArtHook.Native";

    static {
        System.loadLibrary("arthook_native");
    }

    private Native() {
    }

    public static native long mmap(int length);

    public static long mmap_verbose(int length) {
        long mmap = mmap(length);
        Log.d(TAG, "Mapped memory of size " + length + " at " + DebugHelper.intHex(mmap));
        return mmap;
    }

    public static native void munmap(long address, int length);

    public static void munmap_verbose(long address, int length) {
        Log.d(TAG,
                "Removing mapped memory of size " + length + " at " + DebugHelper.intHex(address));
        munmap(address, length);
    }

    public static native void memcpy(long src, long dest, int length);

    public static native void memput(byte[] bytes, long dest);

    public static void memput_verbose(byte[] bytes, long dest) {
        Log.d(TAG, "Writing memory to: " + DebugHelper.intHex(dest));
        Log.d(TAG, DebugHelper.hexdump(bytes, (int) dest));
        memput(bytes, dest);
    }

    public static native byte[] memget(long src, int length);

    public static byte[] memget_verbose(long src, int length) {
        Log.d(TAG, "Reading memory from: " + DebugHelper.intHex(src));
        byte[] bytes = memget(src, length);
        Log.d(TAG, DebugHelper.hexdump(bytes, (int) src));
        return bytes;
    }

    public static native void munprotect(long addr, long len);

    public static void munprotect_verbose(long addr, long len) {
        Log.d(TAG, "Disabling mprotect from " + DebugHelper.intHex(addr));
        munprotect(addr, len);
    }
}
