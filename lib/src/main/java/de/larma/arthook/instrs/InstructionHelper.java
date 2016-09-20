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

package de.larma.arthook.instrs;

import de.larma.arthook.ArtMethod;
import de.larma.arthook.HookPage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class InstructionHelper {

    private final int sizeOfDirectJump;
    private final int sizeOfTargetJump;

    protected InstructionHelper() {
        sizeOfDirectJump = createDirectJump(0).length;
        sizeOfTargetJump = createTargetJump(0, 0, 0).length;
    }

    /**
     * Length of DirectJump as created with {@link #createDirectJump(long)}
     * <p/>
     * This is ensured to be at least 8 bytes and always a multiple of 4
     */
    public int sizeOfDirectJump() {
        return sizeOfDirectJump;
    }

    /**
     * Create assembly corresponding to
     * <code>
     * ldr pc, =c
     * c: .int 0x0 // jump target address
     * </code>
     */
    public abstract byte[] createDirectJump(long targetAddress);

    public abstract long toPC(long code);

    public abstract long toMem(long pc);

    /**
     * Create assembly corresponding to
     * <code>
     * .int 0x0 // original method first sizeof(DirectJump) byte backup
     * [...]
     * DirectJump(original_method_pc+sizeof(DirectJump))
     * </code>
     */
    public byte[] createCallOriginal(long originalAddress, byte[] originalPrologue) {
        byte[] callOriginal = new byte[sizeOfCallOriginal()];
        System.arraycopy(originalPrologue, 0, callOriginal, 0, sizeOfDirectJump());
        byte[] directJump = createDirectJump(toPC(originalAddress + sizeOfDirectJump()));
        System.arraycopy(directJump, 0, callOriginal, sizeOfDirectJump(), sizeOfDirectJump());
        return callOriginal;
    }

    /**
     * Length of a CallOriginal as created by {@link #createCallOriginal(long, byte[])}.
     * <p/>
     * Is always double the size of a DirectJump
     */
    public int sizeOfCallOriginal() {
        return sizeOfDirectJump() * 2;
    }

    public int sizeOfTargetJump() {
        return sizeOfTargetJump;
    }

    public abstract byte[] createTargetJump(long targetAddress, long entryPointFromQuickCompiledCode, long srcAddress);

    public byte[] createTargetJump(HookPage.Hook hook) {
        return createTargetJump(hook.target.getAddress(), hook.target.getEntryPointFromQuickCompiledCode(), hook.src.getAddress());
    }

    @Deprecated
    public int sizeOfArtJump() {
        return 0;
    }

    @Deprecated
    public byte[] createArtJump(long artMethodAddress, long jumpTarget) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public byte[] createArtJump(ArtMethod targetMethod, int offset) {
        return createArtJump(targetMethod.getAddress(),
                targetMethod.getEntryPointFromQuickCompiledCode() + offset);
    }

    @Deprecated
    public byte[] createArtJump(ArtMethod targetMethod) {
        return createArtJump(targetMethod, 0);
    }

    // Helpers
    protected static void writeInt(int i, ByteOrder order, byte[] target, int pos) {
        System.arraycopy(ByteBuffer.allocate(4).order(order).putInt(i).array(), 0, target, pos, 4);
    }

    protected static void writeLong(long i, ByteOrder order, byte[] target, int pos) {
        System.arraycopy(ByteBuffer.allocate(8).order(order).putLong(i).array(), 0, target, pos, 8);
    }

    protected static String toHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        final StringBuilder builder = new StringBuilder();
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            builder.append(hexArray[v >>> 4]);
            builder.append(hexArray[v & 0x0F]);
            builder.append(' ');
        }
        return builder.toString();
    }

    public abstract String getName();
}
