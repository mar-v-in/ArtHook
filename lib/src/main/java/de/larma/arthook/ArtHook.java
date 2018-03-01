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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.larma.arthook.instrs.Arm32;
import de.larma.arthook.instrs.Arm64;
import de.larma.arthook.instrs.InstructionHelper;
import de.larma.arthook.instrs.Thumb2;

import static de.larma.arthook.DebugHelper.logd;
import static de.larma.arthook.DebugHelper.logw;

public final class ArtHook {
    private static final Map<Long, HookPage> pages = new HashMap<>();
    private static InstructionHelper INSTRUCTION_SET_HELPER;

    private ArtHook() {
    }

    static {
        final List<Arch> archs = Arch.getArchitectures();
        for (Arch arch : archs) {
            switch (arch) {
                case ARM32:
                    INSTRUCTION_SET_HELPER = new Arm32();
                    break;
                case THUMB2:
                    INSTRUCTION_SET_HELPER = new Thumb2();
                    break;
                case ARM64:
                    INSTRUCTION_SET_HELPER = new Arm64();
                    break;
                case x86:
                    // TODO: Support x86
                    // INSTRUCTION_SET_HELPER = new X86();
                    break;
                case x86_64:
                    // TODO: Support x86_64
                    // INSTRUCTION_SET_HELPER = new X64();
                    break;
            }

            if (INSTRUCTION_SET_HELPER != null) {
                break;
            }
        }

        if (INSTRUCTION_SET_HELPER != null) {
            logd("ArtHook", "Using: " + INSTRUCTION_SET_HELPER.getName());
        } else {
            throw new LibArtError("Instruction set not supported: " + archs);
        }
    }

    private static HookPage handleHookPage(ArtMethod original, ArtMethod replacement) {
        long originalEntryPoint = INSTRUCTION_SET_HELPER.toMem(
                original.getEntryPointFromQuickCompiledCode());
        if (!pages.containsKey(originalEntryPoint)) {
            pages.put(originalEntryPoint, new HookPage(INSTRUCTION_SET_HELPER, originalEntryPoint,
                    getQuickCompiledCodeSize(original)));
        }

        HookPage page = pages.get(originalEntryPoint);
        page.addHook(new HookPage.Hook(original, replacement));
        page.update();
        return page;
    }

    public static void hook(Class clazz) {
        for (Method method : Assertions.argumentNotNull(clazz, "clazz").getDeclaredMethods()) {
            if (method.isAnnotationPresent(Hook.class)) {
                try {
                    hook(method);
                } catch (RuntimeException e) {
                    logw(e);
                }
            }
        }
    }

    public static OriginalMethod hook(Method method) {
        if (!method.isAnnotationPresent(Hook.class))
            throw new IllegalArgumentException("method must have @Hook annotation");

        Object original;
        try {
            original = findTargetMethod(method);
        } catch (Throwable e) {
            throw new RuntimeException("Can't find original method (" + method.getName() + ")", e);
        }
        String ident = null;
        if (method.isAnnotationPresent(BackupIdentifier.class)) {
            ident = method.getAnnotation(BackupIdentifier.class).value();
        }
        return hook(original, method, ident);
    }

    public static OriginalMethod hook(Method originalMethod, Method replacementMethod, String backupIdentifier) {
        return hook((Object) originalMethod, replacementMethod, backupIdentifier);
    }

    public static OriginalMethod hook(Object originalMethod, Method replacementMethod, String backupIdentifier) {
        ArtMethod backArt;
        if (originalMethod instanceof Method) {
            backArt = hook((Method) originalMethod, replacementMethod);
        } else if (originalMethod instanceof Constructor) {
            backArt = hook((Constructor<?>) originalMethod, replacementMethod);
            backArt.convertToMethod();
        } else {
            throw new RuntimeException("original method must be of type Method or Constructor");
        }

        Method backupMethod = (Method) backArt.getAssociatedMethod();
        backupMethod.setAccessible(true);
        OriginalMethod.store(originalMethod, backupMethod, backupIdentifier);

        return new OriginalMethod(backupMethod);
    }

    public static ArtMethod hook(Method originalMethod, Method replacementMethod) {
        Assertions.argumentNotNull(originalMethod, "originalMethod");
        Assertions.argumentNotNull(replacementMethod, "replacementMethod");
        if (originalMethod == replacementMethod || originalMethod.equals(replacementMethod))
            throw new IllegalArgumentException("originalMethod and replacementMethod can't be the same");
        if (!replacementMethod.getReturnType().isAssignableFrom(originalMethod.getReturnType()))
            throw new IllegalArgumentException("return types of originalMethod and replacementMethod do not match");

        return hook(ArtMethod.of(originalMethod), ArtMethod.of(replacementMethod));
    }

    public static ArtMethod hook(Constructor<?> originalMethod, Method replacementMethod) {
        Assertions.argumentNotNull(originalMethod, "originalMethod");
        Assertions.argumentNotNull(replacementMethod, "replacementMethod");
        if (replacementMethod.getReturnType() != Void.TYPE)
            throw new IllegalArgumentException("return types of replacementMethod has to be 'void'");

        return hook(ArtMethod.of(originalMethod), ArtMethod.of(replacementMethod));
    }

    private static ArtMethod hook(ArtMethod original, ArtMethod replacement) {
        HookPage page = handleHookPage(original, replacement);
        ArtMethod backArt = original.clone();
        backArt.makePrivate();
        if (getQuickCompiledCodeSize(original) < INSTRUCTION_SET_HELPER.sizeOfDirectJump()) {
            original.setEntryPointFromQuickCompiledCode(page.getCallHook());
        } else {
            boolean result = page.activate();
            if (!result) {
                return null;
            }
        }
        return backArt;
    }

    private static int getQuickCompiledCodeSize(ArtMethod method) {
        long entryPoint = INSTRUCTION_SET_HELPER.toMem(method.getEntryPointFromQuickCompiledCode());
        long sizeInfo1 = entryPoint - 4;
        byte[] bytes = Memory.get(sizeInfo1, 4);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    static Object findTargetMethod(Method method) throws NoSuchMethodException, ClassNotFoundException {
        Hook hook = method.getAnnotation(Hook.class);
        String[] split = hook.value().split("->");
        return findTargetMethod(method, Class.forName(split[0]), split.length == 1 ? method.getName() : split[1]);
    }

    private static Object findTargetMethod(Method method, Class<?> targetClass, String methodName)
            throws NoSuchMethodException {
        Class<?>[] params = null;
        if (method.getParameterTypes().length > 0) {
            params = new Class<?>[method.getParameterTypes().length - 1];
            System.arraycopy(method.getParameterTypes(), 1, params, 0, method.getParameterTypes().length - 1);
        }
        if (methodName.equals("()") || methodName.equals("<init>")) {
            // Constructor
            return targetClass.getConstructor(params);
        }
        try {
            Method m = targetClass.getDeclaredMethod(methodName, method.getParameterTypes());
            if (Modifier.isStatic(m.getModifiers())) return m;
        } catch (NoSuchMethodException ignored) {
        }
        try {
            Method m = targetClass.getDeclaredMethod(methodName, params);
            if (!Modifier.isStatic(m.getModifiers())) return m;
        } catch (NoSuchMethodException ignored) {
        }
        throw new NoSuchMethodException();
    }
}
