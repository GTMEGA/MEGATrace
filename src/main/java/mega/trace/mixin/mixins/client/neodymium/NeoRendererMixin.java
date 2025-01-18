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

package mega.trace.mixin.mixins.client.neodymium;

import lombok.val;
import makamys.neodymium.Compat;
import makamys.neodymium.renderer.NeoRenderer;
import mega.trace.client.GPUProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;

@Mixin(value = NeoRenderer.class, remap = false)
public abstract class NeoRendererMixin {
    @Inject(method = "render",
            at = @At("HEAD"),
            require = 1)
    private void preRender(int pass, double alpha, CallbackInfoReturnable<Integer> cir) {
        val profiler = Minecraft.getMinecraft().mcProfiler;

        if (pass == 0) {
            if (Compat.isShadersShadowPass()) {
                profiler.startSection("nd_shadow_0");
            } else {
                profiler.startSection("nd_terrain_0");
            }
        }

        if (pass == 1) {
            if (Compat.isShadersShadowPass()) {
                profiler.startSection("nd_shadow_1");
            } else {
                profiler.startSection("nd_terrain_1");
            }
        }
    }

    @Inject(method = "render",
            at = @At("RETURN"),
            require = 1)
    private void postRender(int pass, double alpha, CallbackInfoReturnable<Integer> cir) {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }
}
