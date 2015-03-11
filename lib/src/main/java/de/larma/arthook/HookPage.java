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

/**
 * Represents and manages a memory page for a hooked method.
 * Methods that share the same memory also share the same HookPage.
 * <p/>
 * A HookPage usually has the following layout:
 * <ul>
 * <li>QuickMethodHeader (24 bytes)</li>
 * <li>Backup of hooked method prologue (sizeOf(DirectJump) = >=8 bytes)</li>
 * <li>Jump to the original method address after the prologue (sizeOf(DirectJump) = >=8 bytes)</li>
 * <li>For each method hooked with the memory address associated to this HookPage: a check if the
 * ArtMethod matches and a jump if needed</li>
 * <li>A jump back to the beginning of the HookPage after the QuickMethodHeader</li>
 * </ul>
 */
public class HookPage {
    private static final int QUICK_HEADER_SIZE = 24;

    private final InstructionHelper instructionHelper;
    private final long originalAddress;
    private final byte[] originalPrologue;
    private final byte[] originalQuickMethodHeader;
    private final Set<Hook> hooks = new HashSet<>();
    private int allocatedSize;
    private long allocatedAddress;

    public HookPage(InstructionHelper instructionHelper, long originalAddress) {
        Assertions.argumentNotNull(instructionHelper, "instructionHelper");
        this.instructionHelper = instructionHelper;
        this.originalAddress = originalAddress;
        originalPrologue = Native.memget_verbose(originalAddress,
                instructionHelper.sizeOfDirectJump());
        originalQuickMethodHeader = Native.memget_verbose(originalAddress - QUICK_HEADER_SIZE,
                QUICK_HEADER_SIZE);
    }

    public int getHooksCount() {
        return hooks.size();
    }

    public Set<Hook> getHooks() {
        return hooks;
    }

    public void addHook(Hook hook) {
        Assertions.argumentNotNull(hook, "hook");
        hooks.add(hook);
    }

    private long getBaseAddress() {
        if (getSize() != allocatedSize) {
            allocate();
        }
        return allocatedAddress;
    }

    public long getCallOriginal() {
        return instructionHelper.toPC(getBaseAddress() + QUICK_HEADER_SIZE);
    }

    public long getCallHook() {
        return instructionHelper.toPC(getBaseAddress() + QUICK_HEADER_SIZE +
                instructionHelper.sizeOfCallOriginal());
    }

    private void allocate() {
        if (allocatedAddress != 0)
            deallocate();
        allocatedSize = getSize();
        allocatedAddress = Native.mmap_verbose(allocatedSize);
    }

    private void deallocate() {
        if (allocatedAddress != 0) {
            Native.munmap_verbose(allocatedAddress, allocatedSize);
            allocatedAddress = 0;
            allocatedSize = 0;
            Native.memput_verbose(originalPrologue, originalAddress);
        }
    }

    public long getOriginalAddress() {
        return originalAddress;
    }


    public int getSize() {
        return QUICK_HEADER_SIZE + instructionHelper.sizeOfCallOriginal() + instructionHelper
                .sizeOfTargetJump() * getHooksCount() + instructionHelper.sizeOfDirectJump();
    }

    public long create() {
        byte[] mainPage = new byte[getSize()];
        /*
         * We skip the first 4 bytes here, it contains the pointer to the mapping table
         * that is used for retrieving the dex pc from the native pc.
         * If we make it a null pointer the lookup will be disabled (no line number will appear
         * in stack trace).
         */
        System.arraycopy(getOriginalQuickMethodHeader(), 4, mainPage, 4, QUICK_HEADER_SIZE - 4);
        int offset = QUICK_HEADER_SIZE;
        byte[] callOriginal = instructionHelper.createCallOriginal(getOriginalAddress(),
                getOriginalPrologue());
        System.arraycopy(callOriginal, 0, mainPage, offset, instructionHelper.sizeOfCallOriginal());
        offset += instructionHelper.sizeOfCallOriginal();
        for (Hook hook : getHooks()) {
            byte[] targetJump = instructionHelper.createTargetJump(hook);
            System.arraycopy(targetJump, 0, mainPage, offset, instructionHelper.sizeOfTargetJump());
            offset += instructionHelper.sizeOfTargetJump();
        }
        byte[] jumpCallOriginal = instructionHelper.createDirectJump(instructionHelper.toPC
                (getCallOriginal()));
        System.arraycopy(jumpCallOriginal, 0, mainPage, offset,
                instructionHelper.sizeOfDirectJump());
        Native.memput_verbose(mainPage, allocatedAddress);
        return allocatedAddress;
    }

    public void update() {
        create();
        for (Hook hook : hooks) {
            if (hook.backup != null) {
                long backupAddr = instructionHelper.toMem(
                        hook.backup.getEntryPointFromQuickCompiledCode());
                Native.munprotect_verbose(backupAddr, instructionHelper.sizeOfDirectJump());
                Native.memput_verbose(instructionHelper.createArtJump(hook.src.getAddress(),
                        getCallOriginal()), backupAddr);
            }
        }
    }

    public void activate() {
        Native.munprotect_verbose(originalAddress, instructionHelper.sizeOfDirectJump());
        Native.memput_verbose(instructionHelper.createDirectJump(getCallHook()), originalAddress);
    }

    @Override
    protected void finalize() throws Throwable {
        deallocate();
        super.finalize();
    }

    public byte[] getOriginalPrologue() {
        return originalPrologue;
    }

    public byte[] getOriginalQuickMethodHeader() {
        return originalQuickMethodHeader;
    }

    public static class Hook {
        public final ArtMethod src;
        public final ArtMethod target;
        public final ArtMethod backup;

        public Hook(ArtMethod src, ArtMethod target, ArtMethod backup) {
            Assertions.argumentNotNull(src, "src");
            Assertions.argumentNotNull(target, "target");
            this.src = src;
            this.target = target;
            this.backup = backup;
        }
    }
}
