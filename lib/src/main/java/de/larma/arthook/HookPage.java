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

import java.util.HashSet;
import java.util.Set;

import de.larma.arthook.instrs.InstructionHelper;

import static de.larma.arthook.DebugHelper.logd;

/**
 * Represents and manages a memory page for a hooked method.
 * Methods that share the same memory also share the same HookPage.
 * <p/>
 * A HookPage usually has the following layout:
 * <ul>
 * <li>For each method hooked with the memory address associated to this HookPage: a check if the
 * ArtMethod matches and a jump if needed</li>
 * <li>Backup of hooked method prologue (sizeOf(DirectJump) = >=8 bytes)</li>
 * <li>Jump to the original method address after the prologue (sizeOf(DirectJump) = >=8 bytes)</li>
 * </ul>
 */
public class HookPage {
    private final InstructionHelper instructionHelper;
    private final long originalAddress;
    private final byte[] originalPrologue;
    private final Set<Hook> hooks = new HashSet<>();
    private int allocatedSize;
    private long allocatedAddress;
    private int quickCompiledCodeSize;
    private boolean active;

    public HookPage(InstructionHelper instructionHelper, long originalAddress, int quickCompiledCodeSize) {
        this.instructionHelper = Assertions.argumentNotNull(instructionHelper, "instructionHelper");
        this.originalAddress = originalAddress;
        this.quickCompiledCodeSize = quickCompiledCodeSize;

        originalPrologue = Memory.get(originalAddress, quickCompiledCodeSize > 0 ? Math.min(quickCompiledCodeSize,
                instructionHelper.sizeOfDirectJump()) : instructionHelper.sizeOfDirectJump());
    }

    public int getHooksCount() {
        return hooks.size();
    }

    public Set<Hook> getHooks() {
        return hooks;
    }

    public void addHook(Hook hook) {
        hooks.add(Assertions.argumentNotNull(hook, "hook"));
    }

    private long getBaseAddress() {
        if (getSize() != allocatedSize) {
            allocate();
        }
        return allocatedAddress;
    }

    public long getCallHook() {
        return instructionHelper.toPC(getBaseAddress());
    }

    private void allocate() {
        if (allocatedAddress != 0)
            deallocate();
        allocatedSize = getSize();
        allocatedAddress = Memory.map(allocatedSize);
    }

    private void deallocate() {
        if (allocatedAddress != 0) {
            Memory.unmap(allocatedAddress, allocatedSize);
            allocatedAddress = 0;
            allocatedSize = 0;

            if (active) {
                Memory.put(originalPrologue, originalAddress);
            }
        }
    }

    public int getSize() {
        return instructionHelper.sizeOfTargetJump() * getHooksCount() + instructionHelper.sizeOfCallOriginal();
    }

    public byte[] create() {
        byte[] mainPage = new byte[getSize()];
        int offset = 0;
        for (Hook hook : getHooks()) {
            byte[] targetJump = instructionHelper.createTargetJump(hook);
            System.arraycopy(targetJump, 0, mainPage, offset, instructionHelper.sizeOfTargetJump());
            offset += instructionHelper.sizeOfTargetJump();
        }
        if (quickCompiledCodeSize > instructionHelper.sizeOfDirectJump()) {
            byte[] callOriginal = instructionHelper.createCallOriginal(originalAddress, originalPrologue);
            System.arraycopy(callOriginal, 0, mainPage, offset, callOriginal.length);
        } else {
            System.arraycopy(originalPrologue, 0, mainPage, offset, originalPrologue.length);
        }
        return mainPage;
    }

    public void update() {
        byte[] page = create();
        logd("Writing HookPage for " + hooks.iterator().next().src);
        Memory.put(page, getBaseAddress());
    }

    public boolean activate() {
        logd("Writing hook to " + DebugHelper.addrHex(getCallHook()) + " in " + DebugHelper.addrHex(originalAddress));
        boolean result = Memory.unprotect(originalAddress, instructionHelper.sizeOfDirectJump());
        if (result) {
            Memory.put(instructionHelper.createDirectJump(getCallHook()), originalAddress);
            active = true;
            return true;
        } else {
            DebugHelper.logw("Writing hook failed: Unable to unprotect memory at " + DebugHelper.addrHex(originalAddress) + "!");
            active = false;
            return false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        deallocate();
        super.finalize();
    }

    public static class Hook {
        public final ArtMethod src;
        public final ArtMethod target;

        public Hook(ArtMethod src, ArtMethod target) {
            this.src = Assertions.argumentNotNull(src, "src");
            this.target = Assertions.argumentNotNull(target, "target");
        }
    }
}
