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
    private static final int MAX_SYNC_COUNT = 32;

    private static final int FUTURE_SYNC_MIN_DELAY = 4;
    private static final int FUTURE_SYNC_MAX_DELAY = 8;
    private static final int PAST_SYNC_DELAY = 8;

    private static final PriorityQueue<GLSyncTaskQueue> syncPool = new ObjectArrayFIFOQueue<>(MAX_SYNC_COUNT);
    private static final PriorityQueue<GLSyncTaskQueue> futureSync = new ObjectArrayFIFOQueue<>(MAX_SYNC_COUNT);
    private static final PriorityQueue<GLSyncTaskQueue> pastSync = new ObjectArrayFIFOQueue<>(MAX_SYNC_COUNT);

    static {
        // Pre-allocate sync objects
        for (var i = 0; i < MAX_SYNC_COUNT; i++) {
            syncPool.enqueue(new GLSyncTaskQueue());
        }
    }

    private static GLSyncTaskQueue currentSync = null;
    private static int currentFrame = -1;

    public static void nextFrame() {
        currentFrame++;
        processFutureSyncs();
        recyclePastSyncs();
        queueCurrentSync();
    }

    public static void queueTask(GLAsyncTask task) {
        task.start(currentFrame);
        currentSync.queueTask(task);
    }

    /**
     * Processes the future, based on the threshold {@link #FUTURE_SYNC_MIN_DELAY}
     * <p>
     * The threshold is used as a fair guess as to if the tasks would be already done or not.
     * <p>
     * If the total number of syncs exceeds {@link #FUTURE_SYNC_MAX_DELAY}, the next sync will be a hard sync.
     */
    private static void processFutureSyncs() {
        while (futureSync.size() > FUTURE_SYNC_MIN_DELAY) {
            val task = futureSync.first();
            val doHardSync = futureSync.size() >= FUTURE_SYNC_MAX_DELAY;
            if (task.tryRun(doHardSync)) {
                pastSync.enqueue(futureSync.dequeue());
            } else {
                break;
            }
        }
    }

    /**
     * Recycles old syncs back into the pool, based on the threshold {@link #PAST_SYNC_DELAY}
     * <p>
     * This is delayed as if done immediately, it may be a blocking operation.
     */
    private static void recyclePastSyncs() {
        while (pastSync.size() > PAST_SYNC_DELAY) {
            val sync = pastSync.dequeue();
            sync.reset();
            syncPool.enqueue(sync);
        }
    }

    private static void queueCurrentSync() {
        currentSync = syncPool.dequeue();
        currentSync.start();
        futureSync.enqueue(currentSync);
    }

    @Lwjgl3Aware
    private static class GLSyncTaskQueue {
        final PriorityQueue<GLAsyncTask> tasks = new ObjectArrayFIFOQueue<>();

        long sync = 0L;

        void start() {
            sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        }

        void queueTask(GLAsyncTask task) {
            tasks.enqueue(task);
        }

        boolean tryRun(boolean doHardSync) {
            // Hard Sync will wait for maximum time, otherwise wait for 0ns
            val waitNs = doHardSync ? Long.MAX_VALUE : 0L;

            // Check status
            // GL_SYNC_FLUSH_COMMANDS_BIT is used to insert a 'glFlush' into the stream after the sync
            // Which will ensure that the sync will be reached 'eventually'
            val status = glClientWaitSync(sync, GL_SYNC_FLUSH_COMMANDS_BIT, waitNs);
            if (status == GL_TIMEOUT_EXPIRED) {
                return false;
            }
            // Run all tasks
            while (!tasks.isEmpty()) {
                tasks.dequeue().end(currentFrame);
            }
            return true;
        }

        void reset() {
            // Sync cannot be reused
            glDeleteSync(sync);
        }
    }
}