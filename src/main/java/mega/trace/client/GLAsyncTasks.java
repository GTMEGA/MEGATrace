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

import static org.lwjgl.opengl.GL46C.GL_SYNC_FLUSH_COMMANDS_BIT;
import static org.lwjgl.opengl.GL46C.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL46C.GL_TIMEOUT_EXPIRED;
import static org.lwjgl.opengl.GL46C.glClientWaitSync;
import static org.lwjgl.opengl.GL46C.glDeleteSync;
import static org.lwjgl.opengl.GL46C.glFenceSync;

@Accessors(fluent = true,
           chain = false)
public final class GLAsyncTasks {
    private static final int MAX_SYNC_COUNT = 32;
    private static final int FUTURE_SYNC_MIN_DELAY = 4;
    private static final int FUTURE_SYNC_MAX_DELAY = 8;
    private static final int PAST_SYNC_DELAY = 8;

    @Getter
    private static final GLAsyncTasks instance = new GLAsyncTasks(MAX_SYNC_COUNT, FUTURE_SYNC_MIN_DELAY, FUTURE_SYNC_MAX_DELAY, PAST_SYNC_DELAY);

    private final int futureSyncMinDelay;
    private final int futureSyncMaxDelay;
    private final int pastSyncDelay;

    private final PriorityQueue<GLSyncTaskQueue> syncPool;
    private final PriorityQueue<GLSyncTaskQueue> futureSync;
    private final PriorityQueue<GLSyncTaskQueue> pastSync;

    private GLSyncTaskQueue currentSync;
    private int currentFrame;

    public GLAsyncTasks(int maxSyncCount, int futureSyncMinDelay, int futureSyncMaxDelay, int pastSyncDelay) {
        this.futureSyncMinDelay = futureSyncMinDelay;
        this.futureSyncMaxDelay = futureSyncMaxDelay;
        this.pastSyncDelay = pastSyncDelay;

        this.syncPool = new ObjectArrayFIFOQueue<>(maxSyncCount);
        // Pre-allocate sync objects
        for (var i = 0; i < maxSyncCount; i++) {
            syncPool.enqueue(new GLSyncTaskQueue());
        }

        this.futureSync = new ObjectArrayFIFOQueue<>(maxSyncCount);
        this.pastSync = new ObjectArrayFIFOQueue<>(maxSyncCount);

        this.currentSync = null;
        this.currentFrame = -1;
    }

    public void nextFrame() {
        queueCurrentSync();
        recyclePastSyncs();
        processFutureSyncs();
    }

    public void queueTask(GLAsyncTask task) {
        task.start(currentFrame);
        currentSync.queueTask(task);
    }

    /**
     * Recycles old syncs back into the pool, based on the threshold {@link #pastSyncDelay}
     * <p>
     * This is delayed as if done immediately, it may be a blocking operation.
     */
    private void recyclePastSyncs() {
        while (pastSync.size() > pastSyncDelay) {
            val sync = pastSync.dequeue();
            sync.reset();
            syncPool.enqueue(sync);
        }
    }

    /**
     * Processes the future, based on the threshold {@link #futureSyncMinDelay}
     * <p>
     * The threshold is used as a fair guess as to if the tasks would be already done or not.
     * <p>
     * If the total number of syncs exceeds {@link #futureSyncMaxDelay}, the next sync will be a hard sync.
     */
    private void processFutureSyncs() {
        while (futureSync.size() > futureSyncMinDelay) {
            val task = futureSync.first();
            val doHardSync = futureSync.size() >= futureSyncMaxDelay;
            if (task.tryRun(doHardSync)) {
                pastSync.enqueue(futureSync.dequeue());
            } else {
                break;
            }
        }
    }

    private void queueCurrentSync() {
        currentSync = syncPool.dequeue();
        currentSync.start();
        futureSync.enqueue(currentSync);
        currentFrame++;
    }

    @Lwjgl3Aware
    private class GLSyncTaskQueue {
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