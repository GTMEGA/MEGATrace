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

import mega.trace.common.CPUProfiler;
import mega.trace.common.colors.Lch;
import mega.trace.common.colors.Palette;
import mega.trace.mixin.interfaces.IProfilerMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Final
    @Shadow
    public Profiler theProfiler;

    @Inject(method = "<init>",
            at = @At("RETURN"),
            require = 1)
    private void onInit(CallbackInfo ci) {
        ((IProfilerMixin) theProfiler).megatrace$cpuProfiler(new CPUProfiler(
                "sv_",
                new Palette(
                        new Lch(
                                0.8866515f, 0.06801123f, 6.1798873f
                        ),
                        0.001071261f, 0.18384407f
                )
        ));
    }
}
