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

package mega.trace.client;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import lombok.val;
import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import mega.trace.natives.Tracy;

import net.minecraft.client.Minecraft;


import static org.lwjgl.opengl.GL46C.*;

public final class ScreenshotHandler {
    private static final PriorityQueue<ScreenshotBuffer> bufferPool = new ObjectArrayFIFOQueue<>(16);

    static {
        for (var i = 0; i < 16; i++) {
            bufferPool.enqueue(new ScreenshotBuffer());
        }
    }

    public static void queueScreenshot() {
        val buffer = bufferPool.dequeue();
        buffer.prepare();
        buffer.capture(GLAsyncTasks.currentFrame());
        GLAsyncTasks.queueTask(() -> {
            buffer.submit(GLAsyncTasks.currentFrame());
            bufferPool.enqueue(buffer);
        });
    }

    @Lwjgl3Aware
    private static class ScreenshotBuffer {
        short srcWidth;
        short srcHeight;

        int fbo;
        int tex;
        int pbo;

        short texWidth;
        short texHeight;
        int pboSizeBytes;

        int capturedFrame;

        void prepare() {
            val mcFb = Minecraft.getMinecraft().getFramebuffer();
            if (srcWidth == mcFb.framebufferWidth && srcHeight == mcFb.framebufferHeight) {
                return;
            }

            srcWidth = (short) mcFb.framebufferWidth;
            srcHeight = (short) mcFb.framebufferHeight;

            // TODO: Dynamic?
            texWidth = 320;
            texHeight = 240;
            pboSizeBytes = texWidth * texHeight * 4;

            if (fbo != 0) {
                glDeleteFramebuffers(fbo);
            }
            if (tex != 0) {
                glDeleteTextures(tex);
            }
            if (pbo != 0) {
                glDeleteBuffers(pbo);
            }

            fbo = glCreateFramebuffers();
            tex = glCreateTextures(GL_TEXTURE_2D);
            pbo = glCreateBuffers();

            glTextureStorage2D(tex, 1, GL_RGBA8, texWidth, texHeight);
            glNamedFramebufferTexture(fbo, GL_COLOR_ATTACHMENT0, tex, 0);
            // Client-side storage should mean the actual buffer is CPU-Side, but it's a hint not a promise.
            glNamedBufferStorage(pbo, pboSizeBytes, GL_MAP_READ_BIT | GL_CLIENT_STORAGE_BIT);
        }

        void capture(int currentFrame) {
            val mcFb = Minecraft.getMinecraft().getFramebuffer();

            // @formatter:off
            glBlitNamedFramebuffer(mcFb.framebufferObject, fbo,
                                   0, 0, srcWidth - 1, srcHeight - 1,
                                   0, 0, texWidth - 1, texHeight - 1,
                                   GL_COLOR_BUFFER_BIT, GL_LINEAR);
            // @formatter:on

            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo);
            glPixelStorei(GL_PACK_ALIGNMENT, 4);
            glGetTextureImage(tex, 0, GL_RGBA, GL_UNSIGNED_BYTE, pboSizeBytes, 0L);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

            capturedFrame = currentFrame;
        }

        void submit(int currentFrame) {
            val image = nglMapNamedBuffer(pbo, GL_READ_ONLY);
            val offset = (byte) (currentFrame - capturedFrame);
            Tracy.frameImage(image, texWidth, texHeight, offset, true);
            glUnmapNamedBuffer(pbo);
        }
    }
}
