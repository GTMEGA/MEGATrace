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

public class Tracy {
    public static void load() throws UnsupportedPlatformException {
        new NativeLoader().loadNative(Tracy.class, "Tracy");
    }
    public static native void init();
    public static native void deinit();
    public static native void frameMark();
    public static native long initZone(byte[] function, byte[] file, int line, boolean active, byte[] name, int color);
    public static native void deinitZone(long zone);

    public static native long gpuAllocSrcLoc(byte[] file, byte[] function, int line, byte [] name, int color);
    public static native void gpuBeginZone(long srcLoc, short queryId, byte context);
    public static native void gpuEndZone(short queryId, byte context);
    public static native void gpuTime(long gpuTime, short queryId, byte context);
    public static native void gpuNewContext(long gpuTime, float period, byte context);
    public static native void gpuCalibration(long gpuTime, long cpuDelta, byte context);
    public static native void gpuTimeSync(long gpuTime, byte context);

    public static native void frameImage(long image, short width, short height, byte offset, boolean flip);
}
