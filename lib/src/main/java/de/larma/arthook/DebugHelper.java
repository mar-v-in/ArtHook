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

import java.lang.reflect.Method;

public final class DebugHelper {

    private static final int HEXDUMP_BYTES_PER_LINE = 16;

    private DebugHelper() {
    }

    public static String intHex(long i) {
        return String.format("0x%08X", (int) i);
    }

    public static String byteHex(byte b) {
        return String.format("%02X", b);
    }

    public static String hexdump(byte[] bytes, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 - start % HEXDUMP_BYTES_PER_LINE; i < bytes.length; i++) {
            int num = Math.abs((start + i) % HEXDUMP_BYTES_PER_LINE);
            if (num == 0 && sb.length() > 0)
                sb.append("\r\n");
            if (num == 0)
                sb.append(intHex(start + i)).append(": ");
            if (num == 8)
                sb.append(" ");
            if (i >= 0)
                sb.append(DebugHelper.byteHex(bytes[i])).append(" ");
            else
                sb.append("   ");
        }
        return sb.toString().trim();
    }

    public static String methodDescription(Method method) {
        return method.getDeclaringClass().getName() + "->" + method.getName() + " @" +
                intHex(ArtMethod.of(method).getEntryPointFromQuickCompiledCode()) +
                " +" + intHex(ArtMethod.of(method).getAddress());
    }
}
