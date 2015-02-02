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

/**
 * Throw this inside backup methods to make the compiler happy
 */
public class ToBeHookedException extends RuntimeException {
    public ToBeHookedException(String unique) {
        super(unique);
    }
    
    public ToBeHookedException(int unique) {
        this(Integer.toHexString(unique));
    }
}
