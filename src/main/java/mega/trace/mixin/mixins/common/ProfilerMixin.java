/*
 * This file is part of MEGATrace.
 *
 * Copyright (C) 2024 The MEGA Team
 * All Rights Reserved
 *
 * The above copyright notice, this permission notice and the word "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 *
 * MEGATrace is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * MEGATrace is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MEGATrace.  If not, see <https://www.gnu.org/licenses/>.
 */

package mega.trace.mixin.mixins.common;

import mega.trace.IProfiler;
import mega.trace.MEGATrace;
import mega.trace.client.GPUProfiler;
import mega.trace.natives.Tracy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.profiler.Profiler;

import java.nio.charset.StandardCharsets;
import java.util.Stack;

@Mixin(Profiler.class)
public abstract class ProfilerMixin implements IProfiler {
    private Stack<Long> sections = new Stack<>();
    private String name = "";
    private boolean includeGpu = false;

    static {
        // TODO: This is here because sometimes the profiler stuff gets called early
        MEGATrace.initNatives();
    }

    @Inject(method = "startSection",
            at = @At("HEAD"),
            require = 1)
    private void startSection(String name, CallbackInfo ci) {
        sections.push(Tracy.beginZone((this.name + name).getBytes(StandardCharsets.UTF_8), 0));
        if (includeGpu) {
            GPUProfiler.startSection(this.name + name, 0x46c26b);
        }
    }

    @Inject(method = "endSection",
            at = @At("HEAD"),
            require = 1)
    private void endSection(CallbackInfo ci) {
        if (!sections.isEmpty()) {
            Tracy.endZone(sections.pop());
            if (includeGpu) {
                GPUProfiler.endSection();
            }
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void includeGpu() {
        this.includeGpu = true;
    }
}
