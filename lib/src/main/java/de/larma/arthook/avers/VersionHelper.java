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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.larma.arthook.ArtMethod;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.N_MR1;
import static android.os.Build.VERSION_CODES.O;

public abstract class VersionHelper {
    private static final boolean VERSION_LMR0 = SDK_INT == LOLLIPOP;
    private static final boolean VERSION_LMR1 = SDK_INT == LOLLIPOP_MR1;
    private static final boolean VERSION_L = VERSION_LMR0 || VERSION_LMR1;
    private static final boolean VERSION_M = SDK_INT == M;
    private static final boolean VERSION_NMR0 = SDK_INT == N;
    private static final boolean VERSION_NMR1 = SDK_INT == N_MR1;
    private static final boolean VERSION_N = VERSION_NMR0 || VERSION_NMR1;
    private static final boolean VERSION_O = SDK_INT == O;
    private static final boolean VERSION_FUTURE = SDK_INT > O;

    private static final boolean FALSE = false;

    public static VersionHelper CURRENT = FALSE ? null
            : VERSION_LMR0 ? new LMR0()
            : VERSION_LMR1 ? new LMR1()
            : VERSION_M ? new M()
            : VERSION_N ? new N()
            : VERSION_O ? new O()
            : VERSION_FUTURE ? new O()
            : null;

    public abstract Object createArtMethod();

    public Object getArtMethodFieldNative(ArtMethod artMethod, String name) {
        return null;
    }

    public boolean setArtMethodFieldNative(ArtMethod artMethod, String name, Object value) {
        return false;
    }

    public Object newAssociatedMethod(Object associatedMethod, ArtMethod artMethod) {
        if (associatedMethod instanceof Method) {
            return newMethod((Method) associatedMethod, artMethod);
        } else if (associatedMethod instanceof Constructor<?>) {
            return newConstructor((Constructor) associatedMethod, artMethod);
        }
        throw new IllegalArgumentException("associatedMethod has to be instance of Method or Constructor, was " + associatedMethod + ".");
    }

    public abstract Constructor<?> newConstructor(Object originalConstructor, ArtMethod newArtMethod);

    public abstract Method newMethod(Object originalMethod, ArtMethod newArtMethod);

    public abstract void copy(ArtMethod src, ArtMethod dst);
}
