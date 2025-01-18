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

package mega.trace.common;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongStack;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import mega.trace.MEGATrace;
import mega.trace.natives.Tracy;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
@Accessors(fluent = true,
           chain = false)
public final class CPUProfiler implements TracyProfiler {
    static {
        // TODO: This is here because sometimes the profiler stuff gets called early
        MEGATrace.initNatives();
    }

    @Getter(AccessLevel.NONE)
    private final LongStack zones = new LongArrayList();

    private final String prefix;
    private final int color;

    @Override
    public void beginZone(byte @NotNull [] name, int color) {
        zones.push(Tracy.beginZone(name, color));
    }

    @Override
    public void endZone() {
        if (!zones.isEmpty()) {
            Tracy.endZone(zones.popLong());
        }
    }
}
