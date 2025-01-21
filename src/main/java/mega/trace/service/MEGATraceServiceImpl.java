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

package mega.trace.service;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import mega.trace.natives.Tracy;

import net.minecraft.profiler.Profiler;

import java.nio.charset.StandardCharsets;

@NoArgsConstructor
public final class MEGATraceServiceImpl implements MEGATraceService {
    @Override
    public void markProfiler(@NonNull Object profiler, @NonNull String prefix, int color) {
        if (!(profiler instanceof Profiler mcProfiler)) {
            throw new IllegalArgumentException("Expected instance of :" + Profiler.class.getName() + " got: " + profiler.getClass().getName());
        }
        // TODO: Custom colors?
        mcProfiler.getProfilingData("__MEGATRACE__:" + prefix);
    }

    @Override
    public void message(String msg) {
        message(msg.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void message(byte[] msg) {
        Tracy.message(msg);
    }

    @Override
    public void messageColor(String msg, int color) {
        messageColor(msg.getBytes(StandardCharsets.UTF_8), color);
    }

    @Override
    public void messageColor(byte[] msg, int color) {
        Tracy.messageColor(msg, color);
    }
}
