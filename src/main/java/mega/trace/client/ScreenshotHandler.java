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
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;
import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import mega.trace.natives.Tracy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;


import static org.lwjgl.opengl.GL46C.*;

@Accessors(fluent = true,
           chain = false)
public final class ScreenshotHandler {
    // region Image Source
    public interface FrameImageSource {
        short width();

        short height();

        int fbo();
    }

    private static final FrameImageSource MC_FRAMEBUFFER = new FrameImageSource() {
        @Override
        public short width() {
            return (short) mcFb().framebufferWidth;
        }

        @Override
        public short height() {
            return (short) mcFb().framebufferHeight;
        }

        @Override
        public int fbo() {
            return mcFb().framebufferObject;
        }

        static Framebuffer mcFb() {
            return Minecraft.getMinecraft().getFramebuffer();
        }
    };
    // endregion

    private static final int IMAGE_POOL_SIZE = 16;
    private static final short IMAGE_WIDTH = 320;
    private static final short IMAGE_HEIGHT = 240;

    @Getter
    private static final ScreenshotHandler instance = new ScreenshotHandler(MC_FRAMEBUFFER, IMAGE_POOL_SIZE, IMAGE_WIDTH, IMAGE_HEIGHT);

    private final FrameImageSource src;
    private final PriorityQueue<FrameImage> imagePool;

    private ScreenshotHandler(FrameImageSource src, int poolSize, short width, short height) {
        this.src = src;

        this.imagePool = new ObjectArrayFIFOQueue<>(poolSize);
        for (var i = 0; i < poolSize; i++) {
            imagePool.enqueue(new FrameImage(width, height));
        }
    }

    public void queueScreenshot() {
        if (!imagePool.isEmpty()) {
            GLAsyncTasks.instance().queueTask(imagePool.dequeue());
        }
    }

    @Lwjgl3Aware
    private class FrameImage implements GLAsyncTask {
        final short width;
        final short height;
        final int imageSizeBytes;

        final int fbo;
        final int tex;
        final int pbo;

        int capturedFrame;

        FrameImage(short width, short height) {
            this.width = width;
            this.height = height;

            this.imageSizeBytes = this.width * this.height * 4;

            this.fbo = glCreateFramebuffers();
            this.tex = glCreateTextures(GL_TEXTURE_2D);
            this.pbo = glCreateBuffers();

            glTextureStorage2D(tex, 1, GL_RGBA8, this.width, this.height);
            glNamedFramebufferTexture(fbo, GL_COLOR_ATTACHMENT0, tex, 0);
            // Client-side storage should mean the actual buffer is CPU-Side, but it's a hint not a promise.
            glNamedBufferStorage(pbo, this.imageSizeBytes, GL_MAP_READ_BIT | GL_CLIENT_STORAGE_BIT);

            this.capturedFrame = -1;
        }

        @Override
        public void start(int currentFrame) {
            // @formatter:off
            glBlitNamedFramebuffer(src.fbo(), fbo,
                                   0, 0, src.width() - 1, src.height() - 1,
                                   0, 0, width - 1, height - 1,
                                   GL_COLOR_BUFFER_BIT, GL_LINEAR);
            // @formatter:on

            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo);
            glPixelStorei(GL_PACK_ALIGNMENT, 4);
            glGetTextureImage(tex, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageSizeBytes, 0L);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

            this.capturedFrame = currentFrame;
        }

        @Override
        public void end(int currentFrame) {
            val image = nglMapNamedBuffer(pbo, GL_READ_ONLY);
            val offset = (byte) (currentFrame - capturedFrame);
            Tracy.frameImage(offset, image, width, height);
            glUnmapNamedBuffer(pbo);
            imagePool.enqueue(this);
        }
    }
}
