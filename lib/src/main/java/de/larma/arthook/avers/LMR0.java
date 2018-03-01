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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.larma.arthook.ArtMethod;

import static de.larma.arthook.ArtMethod.ART_METHOD_CLASS_NAME;

public class LMR0 extends VersionHelper {
    @Override
    public Object createArtMethod() {
        try {
            Constructor constructor = Class.forName(ART_METHOD_CLASS_NAME).getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Method newMethod(Object method, ArtMethod artMethod) {
        try {
            Method m = Method.class.getConstructor(Class.forName(ART_METHOD_CLASS_NAME)).newInstance(artMethod.artMethod);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Method.", t);
        }
    }

    @Override
    public Constructor<?> newConstructor(Object constructor, ArtMethod artMethod) {
        try {
            Constructor<?> c = Constructor.class.getConstructor(Class.forName(ART_METHOD_CLASS_NAME)).newInstance(artMethod.artMethod);
            c.setAccessible(true);
            return c;
        } catch (Throwable t) {
            throw new RuntimeException("Can't create new Constructor.", t);
        }
    }

    @Override
    public void copy(ArtMethod src, ArtMethod dst) {
        try {
            // Copy fields of java.lang.reflect.ArtMethod (this.artMethod => clone.artMethod)
            for (Field field : Class.forName(ART_METHOD_CLASS_NAME).getDeclaredFields()) {
                field.setAccessible(true);
                dst.set(field, src.get(field));
            }
            dst.associatedMethod = newAssociatedMethod(src.associatedMethod, dst);
        } catch (Exception e) {
            throw new RuntimeException("Clone not supported", e);
        }
    }
}
