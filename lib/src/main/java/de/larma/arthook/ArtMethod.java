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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.larma.arthook.avers.VersionHelper;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static de.larma.arthook.DebugHelper.addrHex;
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

    public static final String FIELD_ACCESS_FLAGS = "accessFlags";
    public static final String FIELD_DEX_METHOD_INDEX = "dexMethodIndex";
    public static final String FIELD_ENTRY_POINT_FROM_JNI = "entryPointFromJni";
    public static final String FIELD_ENTRY_POINT_FROM_INTERPRETER = "entryPointFromInterpreter";
    public static final String FIELD_ENTRY_POINT_FROM_QUICK_COMPILED_CODE = "entryPointFromQuickCompiledCode";

    private static final boolean VERSION_L = SDK_INT == LOLLIPOP || SDK_INT == LOLLIPOP_MR1;
    private static final boolean VERSION_M_PLUS = SDK_INT >= M;

    public final Object artMethod;
    public Object associatedMethod;

    /**
     * Create a new ArtMethod.
     * <p/>
     * This really creates a new ArtMethod, not only a helper.
     */
    private ArtMethod() {
        try {
            artMethod = VersionHelper.CURRENT.createArtMethod();
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
    static ArtMethod of(Object method) {
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
        Object val = VersionHelper.CURRENT.getArtMethodFieldNative(this, name);
        if (val == null) {
            val = get(getField(name));
        }
        logd("Reading field: " + name + "=" + val + " from " + associatedMethod);
        return val;
    }

    private void set(String name, Object value) {
        logd("Writing field: " + name + "=" + value + " from " + associatedMethod);
        if (!VersionHelper.CURRENT.setArtMethodFieldNative(this, name, value)) {
            set(getField(name), value);
        }
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    public ArtMethod clone() {
        ArtMethod clone = new ArtMethod();
        VersionHelper.CURRENT.copy(this, clone);
        return clone;
    }

    public void convertToMethod() {
        associatedMethod = VersionHelper.CURRENT.newMethod(associatedMethod, this);
    }

    public Object getAssociatedMethod() {
        return associatedMethod;
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
        setAccessFlags((getAccessFlags() | Modifier.PRIVATE) & ~Modifier.PUBLIC & ~Modifier.PROTECTED);
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
        } else if (VERSION_M_PLUS) {
            return (long) artMethod;
        } else {
            throw new RuntimeException("Not supported on your platform");
        }
    }

    public void set(Field field, Object value) {
        if (VERSION_L) {
            try {
                field.set(artMethod, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Not supported on your platform", e);
            }
        } else if (VERSION_M_PLUS) {
            try {
                field.set(associatedMethod, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Not supported on your platform", e);
            }
        } else {
            throw new RuntimeException("Not supported on your platform");
        }
    }

    public Object get(Field field) {
        if (VERSION_L) {
            try {
                return field.get(artMethod);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Not supported on your platform", e);
            }
        } else if (VERSION_M_PLUS) {
            try {
                return field.get(associatedMethod);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Not supported on your platform", e);
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
        } else if (VERSION_M_PLUS) {
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
                "entryPoint=" + addrHex(getEntryPointFromQuickCompiledCode()) + "}";
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
