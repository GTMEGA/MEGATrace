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

package mega.trace.mixin.mixins.client;

import mega.trace.IProfiler;
import mega.trace.natives.Tracy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final public Profiler mcProfiler;

    @Inject(method = "<init>",
            at = @At("RETURN"),
            require = 1)
    private void markProfiler(CallbackInfo ci) {
        ((IProfiler)mcProfiler).setName("cl_");
    }

    @Inject(method = "runGameLoop",
            at = @At("HEAD"),
            require = 1)
    private void frame(CallbackInfo ci) {
        Tracy.frameMark();
    }

    @ModifyConstant(method = "runGameLoop",
                    constant = @Constant(stringValue = "root",
                                         ordinal = 1),
                    require = 1)
    private String renameRoot(String constant) {
        return "root2";
    }
}
