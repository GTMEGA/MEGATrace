/*
 * This file is part of MEGATrace.
 *
 * Copyright (C) 2024-2025 The MEGA Team
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

import lombok.val;
import mega.trace.common.CPUProfiler;
import mega.trace.common.TracyProfiler;
import mega.trace.common.colors.Lch;
import mega.trace.common.colors.Palette;
import mega.trace.mixin.interfaces.IProfilerMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.profiler.Profiler;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Profiler.class)
public abstract class ProfilerMixin implements IProfilerMixin {
    @Unique
    @Nullable
    private TracyProfiler megatrace$cpuProfiler = null;
    @Unique
    @Nullable
    private TracyProfiler megatrace$gpuProfiler = null;

    @Unique
    private boolean megatrace$gpuProfilerEnabled = false;

    @Unique
    @Override
    public void megatrace$cpuProfiler(TracyProfiler cpuProfiler) {
        this.megatrace$cpuProfiler = cpuProfiler;
    }

    @Unique
    @Override
    public void megatrace$gpuProfiler(TracyProfiler gpuProfiler) {
        this.megatrace$gpuProfiler = gpuProfiler;
    }

    @Inject(method = "startSection",
            at = @At("HEAD"),
            require = 1)
    private void startSection(String name, CallbackInfo ci) {
        if (megatrace$cpuProfiler != null) {
            megatrace$cpuProfiler.beginZone(name);
        }
        if (megatrace$gpuProfilerEnabled && megatrace$gpuProfiler != null) {
            megatrace$gpuProfiler.beginZone(name);
        }
    }

    @Inject(method = "endSection",
            at = @At("HEAD"),
            require = 1)
    private void endSection(CallbackInfo ci) {
        if (megatrace$gpuProfilerEnabled && megatrace$gpuProfiler != null) {
            megatrace$gpuProfiler.endZone();
        }
        if (megatrace$cpuProfiler != null) {
            megatrace$cpuProfiler.endZone();
        }
    }

    @Inject(method = "getProfilingData",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void apiHook(String profilerName, CallbackInfoReturnable<List<Profiler.Result>> cir) {
        if (profilerName == null)
            return;
        if (!profilerName.startsWith("__MEGATRACE__:"))
            return;
        // TODO: Custom colors?
        val color = new Palette(
                new Lch(
                        0.871132f, 0.09365219f, 4.5872717f
                ),
                0.0018133742f, 0.18914041f
        );

        val prefix = profilerName.substring("__MEGATRACE__:".length());
        this.megatrace$cpuProfiler = new CPUProfiler(prefix, color);
        cir.setReturnValue(null);
    }

    @Override
    public void megatrace$enableGPUProfiler(boolean enable) {
        this.megatrace$gpuProfilerEnabled = enable;
    }
}
