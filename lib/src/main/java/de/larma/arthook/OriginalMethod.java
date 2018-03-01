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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static de.larma.arthook.ArtHook.findTargetMethod;
import static de.larma.arthook.DebugHelper.logd;

public class OriginalMethod {
    private static final String TAG = "ArtHook.OriginalMethod";
    private static final Map<ArtMethod, Method> backupMethods = new HashMap<>();
    private static final Map<String, Method> identifiedBackups = new HashMap<>();
    private final Method method;

    OriginalMethod(Method method) {
        this.method = method;
        method.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    public <T> T invoke(Object receiver, Object... args) {
        try {
            return (T) method.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Calling original method failed", e);
        } catch (InvocationTargetException e) {
            throw OriginalMethod.<RuntimeException>throwUnchecked(e.getTargetException());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeStatic(Object... args) {
        return (T) invoke(null, args);
    }

    public static OriginalMethod byOriginal(Method original) {
        return byOriginal((Object) original);
    }

    public static OriginalMethod byOriginal(Constructor<?> original) {
        return byOriginal((Object) original);
    }

    static OriginalMethod byOriginal(Object originalMethod) {
        return new OriginalMethod(backupMethods.get(ArtMethod.of(originalMethod)));
    }

    public static OriginalMethod byHook(Method hook) {
        try {
            return byOriginal(findTargetMethod(hook));
        } catch (Exception e) {
            throw new RuntimeException("must be called with a hook method", e);
        }
    }

    public static OriginalMethod by(String identifier) {
        return new OriginalMethod(identifiedBackups.get(identifier));
    }

    public static OriginalMethod by($ hookAnchor) {
        return by(hookAnchor.getClass());
    }

    public static OriginalMethod by(Class cls) {
        return byHook(cls.getEnclosingMethod());
    }

    public static OriginalMethod byStack() {
        for (StackTraceElement element : new Exception().getStackTrace()) {
            try {
                Class cls = Class.forName(element.getClassName());
                for (Method method : cls.getDeclaredMethods()) {
                    if (method.getName().equals(element.getMethodName()) &&
                            method.isAnnotationPresent(Hook.class)) {
                        logd(TAG, "Calling method hooked by " + method + " as original");
                        return byHook(method);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("must be called from a hook method");
    }

    /**
     * A dirty trick to throw a checked exception like if it was unchecked.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    public static void store(Method originalMethod, Method backupMethod, String backupIdent) {
        store((Object) originalMethod, backupMethod, backupIdent);
    }

    static void store(Object originalMethod, Method backupMethod, String backupIdent) {
        backupMethods.put(ArtMethod.of(originalMethod), backupMethod);
        if (backupIdent != null) {
            identifiedBackups.put(backupIdent, backupMethod);
        }
    }
}
