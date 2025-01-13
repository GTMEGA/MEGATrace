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

import lombok.val;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import static org.lwjgl.opengl.GL46C.*;

public final class ScreenshotHandler {
    // TODO: Cache/Recycle used screenshots

    public static void queueScreenshot() {
        // TODO: Get a screenshot buffer from the queue, prepare it, do the capture, then send the 'upload' method as a lambda to the tasks
    }

    private static class ScreenshotBuffer {
        final int fbo;

        int tex;
        int pbo;
        long ptr;

        int width;
        int height;

        ScreenshotBuffer() {
            fbo = glCreateFramebuffers(); // Framebuffer can be re-used
            tex = 0;
            pbo = 0;
            ptr = -1;//TODO: Persistent bind might be a horrible idea... or persistent and client side flags?
            width = -1;
            height = -1;
        }

        void prepare() {
            val mcFb = mcFramebuffer();
            val mcFbWidth = mcFb.framebufferWidth;
            val mcFbHeight = mcFb.framebufferHeight;

            if (width == mcFbWidth && height == mcFbHeight)
                return;

            {
                if (tex != 0)
                    glDeleteTextures(tex);
                tex = glCreateTextures(GL_TEXTURE_2D);
                // TODO: Allocate texture storage (unsigned byte, rgba), set min/mag to nearest
            }

            {
                if (pbo != 0) {
                    // TODO: Unmap the ptr first!
                    glDeleteBuffers(pbo);
                }

                // TODO: Allocate memory, map the pointer
            }
        }

        void capture() {
            // TODO: Here we will queue the frame capture
        }

        void upload() {
            // TODO: Here we will upload to tracy
        }
    }

    private static Framebuffer mcFramebuffer() {
        return Minecraft.getMinecraft().getFramebuffer();
    }
}
