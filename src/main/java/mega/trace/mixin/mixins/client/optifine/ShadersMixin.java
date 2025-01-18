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

package mega.trace.mixin.mixins.client.optifine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shadersmod.client.Shaders;

import net.minecraft.client.Minecraft;

@Mixin(value = Shaders.class,
       remap = false)
public abstract class ShadersMixin {
    @Inject(method = "beginWeather",
            at = @At("HEAD"),
            require = 1)
    private static void preRenderWeather(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.startSection("of_shaderWeather");
    }

    @Inject(method = "endWeather",
            at = @At("RETURN"),
            require = 1)
    private static void postRenderWeather(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    @Inject(method = "renderDeferred",
            at = @At("HEAD"),
            require = 1)
    private static void preRenderDeferred(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.startSection("of_shaderDeferred");
    }

    @Inject(method = "renderDeferred",
            at = @At("RETURN"),
            require = 1)
    private static void postRenderDeferred(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    @Inject(method = "renderCompositeFinal",
            at = @At("HEAD"),
            require = 1)
    private static void preRenderComposite(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.startSection("of_shaderComposite");
    }

    @Inject(method = "renderCompositeFinal",
            at = @At("RETURN"),
            require = 1)
    private static void postRenderComposite(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    @Inject(method = "renderFinal",
            at = @At("HEAD"),
            require = 1)
    private static void preRenderFinal(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.startSection("of_shaderFinal");
    }

    @Inject(method = "renderFinal",
            at = @At("RETURN"),
            require = 1)
    private static void postRender(CallbackInfo ci) {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }
}
