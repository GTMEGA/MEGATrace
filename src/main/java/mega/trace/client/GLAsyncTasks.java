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

import static org.lwjgl.opengl.GL46C.*;

@UtilityClass
public final class GLAsyncTasks {
    private static final PriorityQueue<GLSyncTaskQueue> syncPool = new ObjectArrayFIFOQueue<>(32);
    private static final PriorityQueue<GLSyncTaskQueue> futureSync = new ObjectArrayFIFOQueue<>(32);
    private static final PriorityQueue<GLSyncTaskQueue> pastSync = new ObjectArrayFIFOQueue<>(32);

    static {
        // Pre-allocate sync objects
        for (var i = 0; i < 16; i++)
            syncPool.enqueue(new GLSyncTaskQueue());
    }

    private static GLSyncTaskQueue currentSync = null;
    private static int currentFrame = -1;

    public static void nextFrame() {
        currentFrame++;

        // Query future syncs
        while(!futureSync.isEmpty()) {
            val task = futureSync.first();
            if (task.tryRun(futureSync.size() < 8 ? 0L : Long.MAX_VALUE))
                pastSync.enqueue(futureSync.dequeue());
        }
        // Recycle past syncs
        while(pastSync.size() > 8) {
            val sync = pastSync.dequeue();
            sync.reset();
            syncPool.enqueue(sync);
        }
        // Queue current sync
        currentSync = syncPool.dequeue();
        currentSync.start();
        futureSync.enqueue(currentSync);
    }

    public static void queueTask(Runnable task) {
        currentSync.queueTask(task);
    }

    public static int currentFrame() {
        return currentFrame;
    }

    @Lwjgl3Aware
    private static class GLSyncTaskQueue {
        long sync = 0L;
        final PriorityQueue<Runnable> tasks = new ObjectArrayFIFOQueue<>(32);

        void start() {
            sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        }

        void queueTask(Runnable task) {
            tasks.enqueue(task);
        }

        boolean tryRun(long waitNs) {
            // Check status
            val status = glClientWaitSync(sync, GL_SYNC_FLUSH_COMMANDS_BIT, waitNs);
            if (status == GL_TIMEOUT_EXPIRED)
                return false;
            // Run tasks
            while (!tasks.isEmpty())
                tasks.dequeue().run();
            return true;
        }

        void reset() {
            glDeleteSync(sync);
            sync = 0L;
        }
    }
}