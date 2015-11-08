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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static de.larma.arthook.DebugHelper.intHex;
import static de.larma.arthook.DebugHelper.logd;

/**
 * A helper class to access {@link java.lang.reflect.ArtMethod} using reflection.
 * <p/>
 * There might exist multiple instances of this helper class at the same time, but
 * {@link #hashCode()} is the same for all of them
 */
@SuppressWarnings("JavadocReference")
public class ArtMethod {
    private static final String ART_METHOD_CLASS_NAME = "java.lang.reflect.ArtMethod";
    private static final String ABSTRACT_METHOD_CLASS_NAME = "java.lang.reflect.AbstractMethod";
    private static final String FIELD_ART_METHOD = "artMethod";

    private static final String FIELD_ACCESS_FLAGS = "accessFlags";
    private static final int FIELD_ACCESS_FLAGS_MIRROR_INDEX = 3;
    private static final String FIELD_DEX_METHOD_INDEX = "dexMethodIndex";
    private static final String FIELD_ENTRY_POINT_FROM_JNI = "entryPointFromJni";
    private static final int FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX = 1;
    private static final String FIELD_ENTRY_POINT_FROM_INTERPRETER = "entryPointFromInterpreter";
    private static final int FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX = 0;
    private static final String FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE = "entryPointFromQuickCompiledCode";
    private static final int FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX = 2;

    private static final int LMR1_MIRROR_FIELDS = 36;
    private static final int LMR1_NATIVE_FIELDS_32 = 12;
    private static final int LMR1_NATIVE_FIELDS_64 = 24;
    private static final int LMR1_NATIVE_FIELDS = Native.is64Bit() ? LMR1_NATIVE_FIELDS_64 : LMR1_NATIVE_FIELDS_32;
    private static final int LMR1_OBJECT_SIZE = LMR1_NATIVE_FIELDS + LMR1_MIRROR_FIELDS;

    private static final int M_MIRROR_FIELDS = 28;
    private static final int M_NATIVE_FIELDS_32 = 12;
    private static final int M_NATIVE_FIELDS_64 = 24;
    private static final int M_NATIVE_FIELDS = Native.is64Bit() ? M_NATIVE_FIELDS_64 : M_NATIVE_FIELDS_32;
    private static final int M_OBJECT_SIZE = M_MIRROR_FIELDS + M_NATIVE_FIELDS;

    private static final boolean VERSION_LMR0 = SDK_INT == LOLLIPOP;
    private static final boolean VERSION_LMR1 = SDK_INT == LOLLIPOP_MR1;
    private static final boolean VERSION_L = VERSION_LMR0 || VERSION_LMR1;
    private static final boolean VERSION_M = SDK_INT >= M;

    private final Object artMethod;
    private Object associatedMethod;

    /**
     * Create a new ArtMethod.
     * <p/>
     * This really creates a new ArtMethod, not only a helper.
     */
    private ArtMethod() {
        try {
            if (VERSION_L) {
                Constructor constructor = Class.forName(ART_METHOD_CLASS_NAME).getDeclaredConstructor();
                constructor.setAccessible(true);
                artMethod = constructor.newInstance();
            } else if (VERSION_M) {
                artMethod = Memory.map(M_OBJECT_SIZE);
            } else {
                throw new RuntimeException("Platform not supported");
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't create new ArtMethod, is this a system running Art?", e);
        }
    }

    private ArtMethod(Object associatedMethod, Object artMethod) {
        this.artMethod = Assertions.argumentNotNull(artMethod, "artMethod");
        this.associatedMethod = associatedMethod;
    }

    /**
     * Generate a helper for the ArtMethod instance residing in the given Method.
     *
     * @param method Any valid Method instance.
     * @return A new helper for the ArtMethod
     * @throws java.lang.NullPointerException when the {@param method} is a null pointer
     */
    public static ArtMethod of(Method method) {
        return of((Object) method);
    }

    /**
     * Generate a helper for the ArtMethod instance residing in the given Constructor.
     *
     * @param constructor Any valid Constructor instance.
     * @return A new helper for the ArtMethod
     * @throws java.lang.NullPointerException when the {@param method} is a null pointer
     */
    public static ArtMethod of(Constructor<?> constructor) {
        return of((Object) constructor);
    }

    /**
     * Generate a helper for the ArtMethod instance residing in the given Constructor or Method.
     *
     * @param method Any valid Constructor or Method instance.
     * @return A new helper for the ArtMethod
     * @throws java.lang.NullPointerException when the {@param method} is a null pointer
     */
    private static ArtMethod of(Object method) {
        if (method == null)
            return null;
        try {
            Field artMethodField = Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredField(FIELD_ART_METHOD);
            artMethodField.setAccessible(true);
            return new ArtMethod(method, artMethodField.get(method));
        } catch (Throwable e) {
            throw new RuntimeException("Method has no artMethod field, is this a system running Art?", e);
        }
    }

    private Object get(String name) {
        Object val = null;
        if (VERSION_LMR1) {
            switch (name) {
                case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                    val = getLMR1Native(FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX);
                    break;
                case FIELD_ENTRY_POINT_FROM_JNI:
                    val = getLMR1Native(FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX);
                    break;
                case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                    val = getLMR1Native(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX);
                    break;
            }
        } else if (VERSION_M) {
            switch (name) {
                case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                    val = getMNative(FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX, false);
                    break;
                case FIELD_ENTRY_POINT_FROM_JNI:
                    val = getMNative(FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX, false);
                    break;
                case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                    val = getMNative(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX, false);
                    break;
            }
        }
        if (val == null) {
            val = get(getField(name));
        }
        logd("Reading field: " + name + "=" + val + " from " + associatedMethod);
        return val;
    }

    private long getLMR1Native(int num) {
        long objectAddress = Unsafe.getObjectAddress(artMethod);
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes = Memory.get(objectAddress + LMR1_MIRROR_FIELDS + intSize * num, intSize);
        if (Native.is64Bit()) {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        } else {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        }
    }

    private long getMNative(int num, boolean mirror) {
        long objectAddress = (long) artMethod;
        int intSize = Native.is64Bit() && !mirror ? 8 : 4;
        byte[] bytes = Memory.get(objectAddress + (mirror ? 0 : M_MIRROR_FIELDS) + intSize * num, intSize);
        if (intSize == 8) {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        } else {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        }
    }

    private void set(String name, Object value) {
        logd("Writing field: " + name + "=" + value + " from " + associatedMethod);
        if (VERSION_LMR1) {
            switch (name) {
                case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                    setLMR1Native(FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX, (Long) value);
                    return;
                case FIELD_ENTRY_POINT_FROM_JNI:
                    setLMR1Native(FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX, (Long) value);
                    return;
                case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                    setLMR1Native(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX, (Long) value);
                    return;
            }
        } else if (VERSION_M) {
            switch (name) {
                case FIELD_ENTRY_POINT_FROM_INTERPRETER:
                    setMNative(FIELD_ENTRY_POINT_FROM_INTERPRETER_NATIVE_INDEX, (Long) value);
                    return;
                case FIELD_ENTRY_POINT_FROM_JNI:
                    setMNative(FIELD_ENTRY_POINT_FROM_JNI_NATIVE_INDEX, (Long) value);
                    return;
                case FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE:
                    setMNative(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE_NATIVE_INDEX, (Long) value);
                    return;
                case FIELD_ACCESS_FLAGS:
                    setMMirror(FIELD_ACCESS_FLAGS_MIRROR_INDEX, (int) value);
                    break;
            }
        }
        set(getField(name), value);
    }

    private void setLMR1Native(int num, long value) {
        long objectAddress = Unsafe.getObjectAddress(artMethod);
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes;
        if (Native.is64Bit()) {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        } else {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
        }
        Memory.put(bytes, objectAddress + LMR1_MIRROR_FIELDS + intSize * num);
    }

    private void setMNative(int num, long value) {
        long objectAddress = (long) artMethod;
        int intSize = Native.is64Bit() ? 8 : 4;
        byte[] bytes;
        if (Native.is64Bit()) {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
        } else {
            bytes = ByteBuffer.allocate(intSize).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
        }
        Memory.put(bytes, objectAddress + M_MIRROR_FIELDS + intSize * num);
    }

    private void setMMirror(int num, int value) {
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        Memory.put(bytes, ((long) artMethod) + 4 * num);
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    public ArtMethod clone() {
        try {
            ArtMethod clone = new ArtMethod();
            if (VERSION_L) {
                /*if (VERSION_LMR1) {
                    long objectAddress = Unsafe.getObjectAddress(artMethod);
                    long map = Memory.map(LMR1_OBJECT_SIZE);
                    Memory.copy(objectAddress, map, LMR1_OBJECT_SIZE);
                    try {
                        long pointerOffset = Unsafe.objectFieldOffset(ArtMethod.class.getDeclaredField("artMethod"));
                        Unsafe.putLong(clone, pointerOffset, map);
                    } catch (NoSuchFieldException e) {
                        DebugHelper.logw(e);
                    }
                }*/
                for (Field field : Class.forName(ART_METHOD_CLASS_NAME).getDeclaredFields()) {
                    field.setAccessible(true);
                    clone.set(field, get(field));
                }
                if (VERSION_LMR1) {
                    clone.setEntryPointFromInterpreter(getEntryPointFromInterpreter());
                    clone.setEntryPointFromJni(getEntryPointFromJni());
                    clone.setEntryPointFromQuickCompiledCode(getEntryPointFromQuickCompiledCode());
                }
            } else if (VERSION_M) {
                Memory.copy((long) artMethod, (long) clone.artMethod, M_OBJECT_SIZE);
                clone.associatedMethod = associatedMethod;
            }
            clone.associatedMethod = clone.newMethod();
            return clone;
        } catch (Exception e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }

    private Method newMethod() {
        try {
            if (VERSION_L) {
                Method m = Method.class.getConstructor(Class.forName(ART_METHOD_CLASS_NAME)).newInstance(artMethod);
                m.setAccessible(true);
                return m;
            } else if (VERSION_M) {
                // TODO
                Constructor<Method> constructor = Method.class.getDeclaredConstructor();
                // we can't use constructor.setAccessible(true); because Google does not like it
                AccessibleObject.setAccessible(new AccessibleObject[]{constructor}, true);
                Method m = constructor.newInstance();
                m.setAccessible(true);
                for (Field field : Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(m, field.get(associatedMethod));
                }
                Field artMethodField = Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredField(FIELD_ART_METHOD);
                artMethodField.setAccessible(true);
                artMethodField.set(m, artMethod);
                return m;
            } else {
                throw new RuntimeException("Method creation not supported on this platform");
            }
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Method", t);
        }
    }

    public Object getAssociatedMethod() {
        return associatedMethod;
    }

    private Constructor<?> newConstructor() {
        try {
            if (VERSION_L) {
                Constructor<?> c = Constructor.class.getConstructor(Class.forName(ART_METHOD_CLASS_NAME))
                        .newInstance(artMethod);
                c.setAccessible(true);
                return c;
            } else if (VERSION_M) {
                // TODO
                throw new RuntimeException("Method creation not yet supported on M");
            } else {
                throw new RuntimeException("Method creation not supported on this platform");
            }
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Method", t);
        }
    }

    public void setDexMethodIndex(int dexMethodIndex) {
        set(FIELD_DEX_METHOD_INDEX, dexMethodIndex);
    }

    public int getAccessFlags() {
        return (int) get(FIELD_ACCESS_FLAGS);
    }

    public void setAccessFlags(int flags) {
        set(FIELD_ACCESS_FLAGS, flags);
    }

    public long getEntryPointFromJni() {
        return (long) get(FIELD_ENTRY_POINT_FROM_JNI);
    }

    public void setEntryPointFromJni(long entryPointFromJni) {
        set(FIELD_ENTRY_POINT_FROM_JNI, entryPointFromJni);
    }

    public long getEntryPointFromInterpreter() {
        return (long) get(FIELD_ENTRY_POINT_FROM_INTERPRETER);
    }

    public void setEntryPointFromInterpreter(long entryPointFromInterpreter) {
        set(FIELD_ENTRY_POINT_FROM_INTERPRETER, entryPointFromInterpreter);
    }

    public long getEntryPointFromQuickCompiledCode() {
        return (long) get(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE);
    }

    public void setEntryPointFromQuickCompiledCode(long entryPointFromQuickCompiledCode) {
        set(FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE, entryPointFromQuickCompiledCode);
    }

    public void makePrivate() {
        setAccessFlags(getAccessFlags() | Modifier.PRIVATE & ~Modifier.PUBLIC & ~Modifier.PROTECTED);
    }

    public void makeNative() {
        setAccessFlags(getAccessFlags() | Modifier.NATIVE);
    }

    public boolean isFinal() {
        return Modifier.isFinal(getAccessFlags()) || Modifier.isPrivate(getAccessFlags())
                || Modifier.isFinal(((Class) get("declaringClass")).getModifiers());
    }

    public long getAddress() {
        if (VERSION_L) {
            return Unsafe.getObjectAddress(artMethod);
        } else if (VERSION_M) {
            return (long) artMethod;
        } else {
            throw new RuntimeException("Not supported on your platform");
        }
    }

    private void set(Field field, Object value) {
        if (VERSION_L) {
            try {
                field.set(artMethod, value);
            } catch (IllegalAccessException ignored) {
            }
        } else if (VERSION_M) {
            try {
                field.set(associatedMethod, value);
            } catch (IllegalAccessException ignored) {
            }
        } else {
            throw new RuntimeException("Not supported on your platform");
        }

    }

    private Object get(Field field) {
        if (VERSION_L) {
            try {
                return field.get(artMethod);
            } catch (IllegalAccessException e) {
                return null;
            }
        } else if (VERSION_M) {
            try {
                return field.get(associatedMethod);
            } catch (IllegalAccessException e) {
                return null;
            }
        } else {
            throw new RuntimeException("Not supported on your platform");
        }
    }

    private Field getField(String name) {
        if (VERSION_L) {
            try {
                Field field = Class.forName(ART_METHOD_CLASS_NAME).getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable e) {
                throw new RuntimeException("Field " + name + " is not available on this system", e);
            }
        } else if (VERSION_M) {
            try {
                Field field = Class.forName(ABSTRACT_METHOD_CLASS_NAME).getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable e) {
                throw new RuntimeException("Field " + name + " is not available on this system", e);
            }
        } else {
            throw new RuntimeException("Not supported on your platform");
        }
    }

    @Override
    public String toString() {
        return "ArtMethod{" + associatedMethod + ", intern=" + artMethod + ", " +
                "entryPoint=" + intHex(getEntryPointFromQuickCompiledCode()) + "}";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass() ||
                !artMethod.equals(((ArtMethod) other).artMethod));
    }

    @Override
    public int hashCode() {
        return artMethod.hashCode();
    }

}
