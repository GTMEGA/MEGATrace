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
import lombok.experimental.UtilityClass;
import lombok.val;
import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;
import mega.trace.natives.Tracy;

import java.nio.charset.StandardCharsets;
import java.util.Stack;

import static org.lwjgl.opengl.GL46C.*;

@Lwjgl3Aware
@UtilityClass
public final class GPUProfiler {
    private static final PriorityQueue<GPUSyncQuery> queryPool = new ObjectArrayFIFOQueue<>(16384);

    public static void init() {
        for (var i = 0; i < 16384; i++) {
            queryPool.enqueue(new GPUSyncQuery());
        }

        val gpuTime = glGetInteger64(GL_TIMESTAMP);
        Tracy.gpuInit(gpuTime);
    }

    private static int lastSync = 0;

    public static void timeSync() {
        lastSync++;
        if (lastSync > 100) {
            val gpuTime = glGetInteger64(GL_TIMESTAMP);
            Tracy.gpuTimeSync(gpuTime);
            lastSync = 0;
        }
    }

    private static final Stack<GPUSyncQuery> sections = new Stack<>();

    public static void startSection(String name) {
        startSection(name, 0xFF0000);
    }

    public static void startSection(String name, int color) {
        val section = queryPool.dequeue();
        section.push("gl_" + name, color);
        sections.push(section);
    }

    public static void endSection() {
        if (!sections.isEmpty()) {
            sections.pop().pop();
        }
    }

    @Lwjgl3Aware
    private static class GPUSyncQuery implements GLAsyncTask {
        final int glQueryPush = glGenQueries();
        final int glQueryPop = glGenQueries();

        short queryIdPush;
        short queryIdPop;

        void push(String name, int color) {
            queryIdPush = Tracy.gpuBeginZone(name.getBytes(StandardCharsets.UTF_8), color);
            glQueryCounter(glQueryPush, GL_TIMESTAMP);
        }

        void pop() {
            glQueryCounter(glQueryPop, GL_TIMESTAMP);
            queryIdPop = Tracy.gpuEndZone();
            GLAsyncTasks.queueTask(this);
        }

        @Override
        public void end() {
            val gpuTimePush = glGetQueryObjectui64(glQueryPush, GL_QUERY_RESULT);
            val gpuTimePop = glGetQueryObjectui64(glQueryPop, GL_QUERY_RESULT);

            Tracy.gpuTime(queryIdPush, gpuTimePush);
            Tracy.gpuTime(queryIdPop, gpuTimePop);

            queryPool.enqueue(this);
        }
    }
}
