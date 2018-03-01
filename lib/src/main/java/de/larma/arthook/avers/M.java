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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.larma.arthook.ArtMethod;
import de.larma.arthook.Memory;
import de.larma.arthook.Native;

import static de.larma.arthook.ArtMethod.ABSTRACT_METHOD_CLASS_NAME;
import static de.larma.arthook.ArtMethod.FIELD_ACCESS_FLAGS;
import static de.larma.arthook.ArtMethod.FIELD_ART_METHOD;
import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_INTERPRETER;
import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_JNI;
import static de.larma.arthook.ArtMethod.FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE;

public class M extends VersionHelper {
    private static final int FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX = 0;
    private static final int FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX = 1;
    private static final int FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX = 2;
    private static final int FIELD_ACCESS_FLAGS_MIRROR_INDEX = 3;

    private static final int M_MIRROR_FIELDS = Native.is64Bit() ? 32 : 28;
    private static final int M_NATIVE_FIELDS_32 = 12;
    private static final int M_NATIVE_FIELDS_64 = 24;
    private static final int M_NATIVE_FIELDS = Native.is64Bit() ? M_NATIVE_FIELDS_64 : M_NATIVE_FIELDS_32;
    private static final int M_OBJECT_SIZE = M_MIRROR_FIELDS + M_NATIVE_FIELDS;

    @Override
    public Object createArtMethod() {
        return Memory.map(M_OBJECT_SIZE);
    }

    @Override
    public Object getArtMethodFieldNative(ArtMethod artMethod, String name) {
        switch (name) {
            case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                return getNative((Long) artMethod.artMethod, FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX, false);
            case FIELD_ENTRY_POINT_FROM_JNI:
                return getNative((Long) artMethod.artMethod, FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX, false);
            case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                return getNative((Long) artMethod.artMethod, FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX, false);
        }
        return super.getArtMethodFieldNative(artMethod, name);
    }

    private long getNative(long objectAddress, int num, boolean mirror) {
        int intSize = Native.is64Bit() && !mirror ? 8 : 4;
        byte[] bytes = Memory.get(objectAddress + (mirror ? 0 : M_MIRROR_FIELDS) + intSize * num, intSize);
        if (intSize == 8) {
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
            case FIELD_ACCESS_FLAGS:
                setMirror(artMethod, FIELD_ACCESS_FLAGS_MIRROR_INDEX, (int) value);
                return true;
        }
        return super.setArtMethodFieldNative(artMethod, name, value);
    }

    private void setNative(ArtMethod artMethod, int num, long value) {
        long objectAddress = (long) artMethod.artMethod;
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes;
        if (Native.is64Bit()) {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        } else {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
        }
        Memory.put(bytes, objectAddress + M_MIRROR_FIELDS + intSize * num);
    }

    private void setMirror(ArtMethod artMethod, int num, int value) {
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        Memory.put(bytes, ((long) artMethod.artMethod) + 4 * num);
    }

    @Override
    public Method newMethod(Object associatedMethod, ArtMethod artMethod) {
        try {
            Constructor<Method> methodConstructor = Method.class.getDeclaredConstructor();
            // we can't use methodConstructor.setAccessible(true); because Google does not like it
            AccessibleObject.setAccessible(new AccessibleObject[]{methodConstructor}, true);

            Method m = methodConstructor.newInstance();
            m.setAccessible(true);
            for (Field field : Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredFields()) {
                field.setAccessible(true);
                field.set(m, field.get(associatedMethod));
            }

            Field artMethodField = Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredField(FIELD_ART_METHOD);
            artMethodField.setAccessible(true);
            artMethodField.set(m, artMethod.artMethod);

            return m;
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Method", t);
        }
    }

    @Override
    public Constructor<?> newConstructor(Object associatedMethod, ArtMethod newArtMethod) {
        try {
            Constructor<Constructor> constructorConstructor = Constructor.class.getDeclaredConstructor();
            // we can't use constructorConstructor.setAccessible(true); because Google does not like it
            AccessibleObject.setAccessible(new AccessibleObject[]{constructorConstructor}, true);

            Constructor<?> c = constructorConstructor.newInstance();
            c.setAccessible(true);
            for (Field field : Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredFields()) {
                field.setAccessible(true);
                field.set(c, field.get(associatedMethod));
            }

            Field artMethodField = Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredField(FIELD_ART_METHOD);
            artMethodField.setAccessible(true);
            artMethodField.set(c, newArtMethod.artMethod);

            return c;
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Method", t);
        }
    }

    @Override
    public void copy(ArtMethod src, ArtMethod dst) {
        Memory.copy((long) src.artMethod, (long) dst.artMethod, M_OBJECT_SIZE);
        dst.associatedMethod = newAssociatedMethod(src.associatedMethod, dst);
    }
}
