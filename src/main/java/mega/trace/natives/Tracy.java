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

package mega.trace.natives;

import org.jetbrains.annotations.Nullable;

public class Tracy {
    public static void load() throws UnsupportedPlatformException {
        new NativeLoader().loadNative(Tracy.class, "Tracy");
    }
    public static native void init();
    public static native void deinit();
    public static native void frameMark();
    public static native long initZone(byte[] function, byte[] file, int line, boolean active, byte[] name, int color);
    public static native void deinitZone(long zone);
    public static native void markServerThread();
    public static native void markClientThread();

    public static native long gpu_allocSrcLoc(byte[] file, byte[] function, int line, byte @Nullable [] name, int color);
    public static native void gpu_beginZone(long srcLoc, short queryId, byte context);
    public static native void gpu_endZone(short queryId, byte context);
    public static native void gpu_time(long gpuTime, short queryId, byte context);
    public static native void gpu_newContext(long gpuTime, float period, byte context);
    public static native void gpu_calibration(long gpuTime, long cpuDelta, byte context);
    public static native void gpu_timeSync(long gpuTime, byte context);
}
