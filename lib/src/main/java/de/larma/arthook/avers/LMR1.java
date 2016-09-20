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

package de.larma.arthook.avers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.larma.arthook.ArtMethod;
import de.larma.arthook.Memory;
import de.larma.arthook.Native;
import de.larma.arthook.Unsafe;

import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_INTERPRETER;
import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_JNI;
import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE;

public class LMR1 extends LMR0 {
    private static final int FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX = 0;
    private static final int FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX = 1;
    private static final int FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX = 2;

    private static final int LMR1_MIRROR_FIELDS = Native.is64Bit() ? 40 : 36;
    private static final int LMR1_NATIVE_FIELDS_32 = 12;
    private static final int LMR1_NATIVE_FIELDS_64 = 24;
    private static final int LMR1_NATIVE_FIELDS = Native.is64Bit() ? LMR1_NATIVE_FIELDS_64 : LMR1_NATIVE_FIELDS_32;
    private static final int LMR1_OBJECT_SIZE = LMR1_NATIVE_FIELDS + LMR1_MIRROR_FIELDS;

    @Override
    public Object getArtMethodFieldNative(ArtMethod artMethod, String name) {
        switch (name) {
            case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                return getNative(artMethod, FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX);
            case FIELD_ENTRY_POINT_FROM_JNI:
                return getNative(artMethod, FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX);
            case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                return getNative(artMethod, FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX);
        }
        return super.getArtMethodFieldNative(artMethod, name);
    }

    private long getNative(ArtMethod artMethod, int num) {
        long objectAddress = Unsafe.getObjectAddress(artMethod.artMethod);
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes = Memory.get(objectAddress + LMR1_MIRROR_FIELDS + intSize * num, intSize);
        if (Native.is64Bit()) {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        } else {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        }
    }

    @Override
    public boolean setArtMethodFieldNative(ArtMethod artMethod, String name, Object value) {
        switch (name) {
            case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                setNative(artMethod, FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX, (Long) value);
                return true;
            case FIELD_ENTRY_POINT_FROM_JNI:
                setNative(artMethod, FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX, (Long) value);
                return true;
            case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                setNative(artMethod, FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX, (Long) value);
                return true;
        }
        return super.setArtMethodFieldNative(artMethod, name, value);
    }

    private void setNative(ArtMethod artMethod, int num, long value) {
        long objectAddress = Unsafe.getObjectAddress(artMethod.artMethod);
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes;
        if (Native.is64Bit()) {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        } else {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
        }
        Memory.put(bytes, objectAddress + LMR1_MIRROR_FIELDS + intSize * num);
    }

    @Override
    public void copy(ArtMethod src, ArtMethod dst) {
        super.copy(src, dst);
        dst.setEntryPointFromInterpreter(src.getEntryPointFromInterpreter());
        dst.setEntryPointFromJni(src.getEntryPointFromJni());
        dst.setEntryPointFromQuickCompiledCode(src.getEntryPointFromQuickCompiledCode());
    }
}
