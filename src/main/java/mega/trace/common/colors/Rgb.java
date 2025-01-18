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

package mega.trace.common.colors;

import com.falsepattern.lib.util.MathUtil;

import java.awt.Color;

public record Rgb(float r, float g, float b) {
    public static Rgb fromLab(Lab lab) {
        return fromLab(lab.L(), lab.a(), lab.b());
    }

    public static Rgb fromLab(float L, float a, float b) {
        float l_ = L + 0.3963377774f * a + 0.2158037573f * b;
        float m_ = L - 0.1055613458f * a - 0.0638541728f * b;
        float s_ = L - 0.0894841775f * a - 1.2914855480f * b;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        return new Rgb(+4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s, -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
                       -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s);
    }

    public Rgb clamp(float min, float max) {
        return new Rgb(MathUtil.clamp(r, min, max), MathUtil.clamp(g, min, max), MathUtil.clamp(b, min, max));
    }

    public int toArgbInt() {
        return toArgbInt(1.0f);
    }

    public int toArgbInt(float alpha) {
        return new Color(r, g, b, alpha).getRGB();
    }

    public static Rgb fromPalette(Palette palette, int index) {
        return fromPalette(palette.lch(), palette.sL(), palette.sH(), index);
    }

    public static Rgb fromPalette(Lch lch, float sL, float sH, int index) {
        return fromPalette(lch.L(), lch.C(), lch.h(), sL, sH, index);
    }

    public static Rgb fromPalette(float L, float C, float h, float sL, float sH, int index) {
        L -= sL * index;
        h += sH * index;
        return Rgb.fromLab(Lab.fromLch(L, C, h)).clamp(0, 1);
    }
}
