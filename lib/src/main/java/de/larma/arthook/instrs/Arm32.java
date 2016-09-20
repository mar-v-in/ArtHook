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

import java.nio.ByteOrder;

/**
 * Art hooking for arm32.
 * <p/>
 *
 * @see <a href="http://armconverter.com/">http://armconverter.com/</a>
 */
@SuppressWarnings("MagicNumber")
public class Arm32 extends InstructionHelper {

    @Override
    public int sizeOfDirectJump() {
        return 8;
    }

    @Override
    public byte[] createDirectJump(long targetAddress) {
        final byte[] instructions = new byte[]{
                0x04, (byte) 0xf0, 0x1f, (byte) 0xe5,   // ldr pc, [pc, #-4]
                0, 0, 0, 0                              // .int 0x0
        };
        writeInt((int) targetAddress, ByteOrder.LITTLE_ENDIAN, instructions,
                instructions.length - 4);
        return instructions;
    }

    @Override
    public int sizeOfTargetJump() {
        return 32;
    }

    @Override
    public byte[] createTargetJump(long targetAddress, long entryPointFromQuickCompiledCode, long srcAddress) {
        final byte[] instructions = new byte[]{
                0x14, (byte) 0xc0, (byte) 0x9f, (byte) 0xe5,    // ldr ip, [pc, #20] (srcAddress)
                0x0c, 0x00, 0x50, (byte) 0xe1,                  // cmp r0, ip
                0x04, 0x00, 0x00, 0x1a,                         // bne next
                0x00, 0x00, (byte) 0x9f, (byte) 0xe5,           // ldr r0, [pc, #0] (targetAddress)
                0x04, (byte) 0xf0, (byte) 0x9f, (byte) 0xe5,    // ldr pc, [pc, #4] (entryPointFromQuickCompiledCode)
                0, 0, 0, 0,                                     // targetAddress
                0, 0, 0, 0,                                     // entryPointFromQuickCompiledCode
                0, 0, 0, 0,                                     // srcAddress
        };
        writeInt((int) targetAddress, ByteOrder.LITTLE_ENDIAN, instructions,
                instructions.length - 12);
        writeInt((int) entryPointFromQuickCompiledCode,
                ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 8);
        writeInt((int) srcAddress, ByteOrder.LITTLE_ENDIAN, instructions,
                instructions.length - 4);
        return instructions;
    }

    @Override
    public int sizeOfArtJump() {
        return 16;
    }

    @Override
    public byte[] createArtJump(long artMethodAddress, long jumpTarget) {
        final byte[] instructions = new byte[]{
                0x00, 0x00, (byte) 0x1f, (byte) 0xe5,               // ldr r0, [pc, #-0]
                0x00, (byte) 0xf0, (byte) 0x1f, (byte) 0xe5,        // ldr pc, [pc, #-0]
                0, 0, 0, 0,                                         // target_method_pos
                0, 0, 0, 0                                          // target_method_pc
        };
        writeInt((int) artMethodAddress, ByteOrder.LITTLE_ENDIAN, instructions,
                instructions.length - 8);
        writeInt((int) jumpTarget, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 4);
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
        return "32-bit ARM";
    }
}
