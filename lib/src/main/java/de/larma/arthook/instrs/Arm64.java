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

import de.larma.arthook.HookPage;

@SuppressWarnings("MagicNumber")
public class Arm64 extends InstructionHelper {

    @Override
    public int sizeOfDirectJump() {
        return 16;
    }

    @Override
    public byte[] createDirectJump(long targetAddress) {
        final byte[] instructions = new byte[] {
                0x49, 0x00, 0x00, 0x58,         // ldr x9, _targetAddress
                0x20, 0x01, 0x1F, (byte) 0xD6,  // br x9
                0x00, 0x00, 0x00, 0x00,         // targetAddress
                0x00, 0x00, 0x00, 0x00          // targetAddress
        };
        writeLong(targetAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 8);
        return instructions;
    }

    @Override
    public int sizeOfTargetJump() {
        return 48;
    }

    @Override
    public byte[] createTargetJump(long targetAddress, long entryPointFromQuickCompiledCode, long srcAddress) {
        final byte[] instructions = new byte[] {
                0x49, 0x01, 0x00, 0x58,        // ldr x9, _src_method_pos_x
                0x1F, 0x00, 0x09, (byte) 0xEB, // cmp x0, x9
                0x41, 0x01, 0x00, 0x54,        // bne _branch_1
                0x60, 0x00, 0x00, 0x58,        // ldr x0, _target_method_pos_x
                (byte) 0x89, 0x00, 0x00, 0x58, // ldr x9, _target_method_pc
                0x20, 0x01, 0x1F, (byte) 0xD6, // br x9
                0x00, 0x00, 0x00, 0x00,        // target_method_pos_x
                0x00, 0x00, 0x00, 0x00,        // target_method_pos_x
                0x00, 0x00, 0x00, 0x00,        // target_method_pc
                0x00, 0x00, 0x00, 0x00,        // target_method_pc
                0x00, 0x00, 0x00, 0x00,        // src_method_pos_x
                0x00, 0x00, 0x00, 0x00         // src_method_pos_x
        };
        writeLong(targetAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 24);
        writeLong(entryPointFromQuickCompiledCode, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 16);
        writeLong(srcAddress, ByteOrder.LITTLE_ENDIAN, instructions, instructions.length - 8);
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
        return "64-bit ARM";
    }
}
