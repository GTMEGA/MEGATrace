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
    private static final byte CONTEXT_ID = 1;

    private static final PriorityQueue<GPUSyncQuery> queryPool = new ObjectArrayFIFOQueue<>(1024);

    public static void init() {
        for (var i = 0; i < 1024; i++) {
            queryPool.enqueue(new GPUSyncQuery());
        }

        val gpuTime = glGetInteger64(GL_TIMESTAMP);
        val period = 1F;
        Tracy.gpuNewContext(gpuTime, period, CONTEXT_ID);
    }

    private static int lastSync = 0;

    public static void timeSync() {
        lastSync++;
        if (lastSync > 100) {
            val gpuTime = glGetInteger64(GL_TIMESTAMP);
            Tracy.gpuTimeSync(gpuTime, CONTEXT_ID);
            lastSync = 0;
        }
    }

    private static final Stack<GPUSyncQuery> sections = new Stack<>();

    public static void startSection(String name) {
        final long srcLoc;
        {
            val dummyBytes = "Frame".getBytes(StandardCharsets.UTF_8);
            val nameBytes = ("gl_" + name).getBytes(StandardCharsets.UTF_8);
            srcLoc = Tracy.gpuAllocSrcLoc(dummyBytes, dummyBytes, 0, nameBytes, 0);
        }

        val section = queryPool.dequeue();
        section.push(srcLoc);
        sections.push(section);
    }

    public static void endSection() {
        if (!sections.isEmpty()) {
            sections.pop().pop();
        }
    }

    @Lwjgl3Aware
    private static class GPUSyncQuery implements GLAsyncTask {
        private static short lastQueryId = 1;

        final int glQueryPush = glGenQueries();
        final int glQueryPop = glGenQueries();

        final short queryIdPush = lastQueryId++;
        final short queryIdPop = lastQueryId++;

        void push(long srcLoc) {
            glQueryCounter(glQueryPush, GL_TIMESTAMP);
            Tracy.gpuBeginZone(srcLoc, queryIdPush, CONTEXT_ID);
        }

        void pop() {
            glQueryCounter(glQueryPop, GL_TIMESTAMP);
            Tracy.gpuEndZone(queryIdPop, CONTEXT_ID);
            GLAsyncTasks.queueTask(this);
        }

        @Override
        public void end() {
            val gpuTimePush = glGetQueryObjectui64(glQueryPush, GL_QUERY_RESULT);
            val gpuTimePop = glGetQueryObjectui64(glQueryPop, GL_QUERY_RESULT);

            Tracy.gpuTime(gpuTimePush, queryIdPush, CONTEXT_ID);
            Tracy.gpuTime(gpuTimePop, queryIdPop, CONTEXT_ID);

            queryPool.enqueue(this);
        }
    }
}
