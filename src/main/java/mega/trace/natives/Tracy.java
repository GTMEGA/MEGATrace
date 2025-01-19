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

package mega.trace.natives;

public class Tracy {
    public static void load() throws UnsupportedPlatformException {
        new NativeLoader().loadNative(Tracy.class, "Tracy");
    }

    public static native void init();

    public static native void deinit();

    public static native void frameMark();

    public static native void message(byte[] msg);

    public static native void messageColor(byte[] msg, int color);

    public static native long beginZone(byte[] name, int color);

    public static native void endZone(long zone);

    public static native void gpuInit(long gpuTime);

    public static native void gpuTimeSync(long gpuTime);

    public static native short gpuBeginZone(byte[] name, int color);

    public static native short gpuEndZone();

    public static native void gpuTime(short queryId, long gpuTime);

    public static native void frameImage(byte offset, long image, short width, short height);
}
