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

import android.util.Log;

import java.nio.ByteOrder;

import de.larma.arthook.DebugHelper;

/**
 * Art hooking for x86.
 * <p/>
 *
 * @see <a href="https://defuse.ca/online-x86-assembler.htm#disassembly">https://defuse.ca/online-x86-assembler.htm#disassembly</a>
 */
@SuppressWarnings("MagicNumber")
public class X86 extends InstructionHelper {

    private static final String TAG = "X86";

    /*
     * TODO: Hooking for x86 is rather hard.
     *
     * 1. Instruction size is not constant, so overwriting the beginning of a method may lead to
     *    problems. We can only overwrite full instructions! Hopefully the beginning of all methods
     *    is the same.
     * 2. We may have to fix the first argument when creating the target jump. It is unclear whether
     *    arg0 is on the stack or in a register. If it is on the stack, things get tricky!
     *
     * For now, we are not able to hook. The current code is only able to call the original method.
     */

    @Override
    public byte[] createDirectJump(long targetAddress) {
        // Note: We have to unsure that we only overwrite full instructions. Unclear how to solve that!
        // We currently have padding byte(s) at the end which works for one use-case.

        /*
        push 0x01010101
        ret
         */
        byte[] instructions = new byte[]{
                0x68, 0x01, 0x01, 0x01, 0x01,
                (byte) 0xC3,
                0x00
        };
        writeInt((int) targetAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 6);
        Log.v(TAG, "createDirectJump(" + DebugHelper.addrHex(targetAddress) + "): " + toHex(instructions));
        return instructions;
    }

    @Override
    public byte[] createTargetJump(long targetAddress, long entryPointFromQuickCompiledCode, long srcAddress) {
        // TODO: Create target jump for x86.
        byte[] instructions = new byte[]{
        };
        // writeInt((int) targetAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 12);
        // writeInt((int) entryPointFromQuickCompiledCode, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 8);
        // writeInt((int) srcAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 4);
        Log.v(TAG, "createTargetJump(" + DebugHelper.addrHex(targetAddress) + ", " + DebugHelper.addrHex(entryPointFromQuickCompiledCode) + ", " + DebugHelper.addrHex(srcAddress) + "): " + toHex(instructions));
        return instructions;
    }

    @Override
    public long toPC(long code) {
        return code;
    }

    @Override
    public long toMem(long pc) {
        return pc;
    }

    @Override
    public String getName() {
        return "32-bit x86";
    }
}
