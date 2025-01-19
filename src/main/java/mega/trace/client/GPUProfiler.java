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

package mega.trace.client;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.val;
import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import mega.trace.common.TracyProfiler;
import mega.trace.common.colors.Lch;
import mega.trace.common.colors.Palette;
import mega.trace.common.colors.Rgb;
import mega.trace.natives.Tracy;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL46C.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL46C.GL_TIMESTAMP;
import static org.lwjgl.opengl.GL46C.glGenQueries;
import static org.lwjgl.opengl.GL46C.glGetInteger64;
import static org.lwjgl.opengl.GL46C.glGetQueryObjectui64;
import static org.lwjgl.opengl.GL46C.glQueryCounter;

public final class GPUProfiler implements TracyProfiler {
    @Getter
    private static final TracyProfiler instance = new GPUProfiler();

    private static int lastTimeSync = 0;

    private final PriorityQueue<GPUZone> zonePool;
    private final AbstractObjectList<GPUZone> zones = new ObjectArrayList<>();

    private GPUProfiler() {
        this.zonePool = new ObjectArrayFIFOQueue<>(16384);
        for (var i = 0; i < 16384; i++) {
            zonePool.enqueue(new GPUZone());
        }

        val gpuTime = glGetInteger64(GL_TIMESTAMP);
        Tracy.gpuInit(gpuTime);
    }

    public static void timeSync() {
        lastTimeSync++;
        if (lastTimeSync > 100) {
            val gpuTime = glGetInteger64(GL_TIMESTAMP);
            Tracy.gpuTimeSync(gpuTime);
            lastTimeSync = 0;
        }
    }

    //L: 0.7460972 C: 0.0931054 H: 3.8307996 sL: 0.0018235666 sH: 0.2772051
    private final Palette palette = new Palette(
            new Lch(0.7460972f, 0.093105f, 3.8307996f),
            0.0018235666f, 0.2772051f
    );

    @Override
    public String prefix() {
        return "gl_";
    }

    @Override
    public void beginZone(byte @NotNull [] name) {
        val zone = zonePool.dequeue();
        // TODO palette
        zone.gpuBeginZone(name, Rgb.fromPalette(palette, zones.size()).toArgbInt());
        zones.push(zone);
    }

    @Override
    public void endZone() {
        if (!zones.isEmpty()) {
            zones.pop().gpuEndZone();
        }
    }

    private class GPUZone implements GLAsyncTask {
        final int glQueryPush = glGenQueries();
        final int glQueryPop = glGenQueries();

        short queryIdPush;
        short queryIdPop;

        void gpuBeginZone(byte[] name, int color) {
            queryIdPush = Tracy.gpuBeginZone(name, color);
            glQueryCounter(glQueryPush, GL_TIMESTAMP);
        }

        void gpuEndZone() {
            glQueryCounter(glQueryPop, GL_TIMESTAMP);
            queryIdPop = Tracy.gpuEndZone();
            GLAsyncTasks.instance().queueTask(this);
        }

        @Override
        public void end(int currentFrame) {
            val gpuTimePush = glGetQueryObjectui64(glQueryPush, GL_QUERY_RESULT);
            val gpuTimePop = glGetQueryObjectui64(glQueryPop, GL_QUERY_RESULT);

            Tracy.gpuTime(queryIdPush, gpuTimePush);
            Tracy.gpuTime(queryIdPop, gpuTimePop);

            zonePool.enqueue(this);
        }
    }
}
