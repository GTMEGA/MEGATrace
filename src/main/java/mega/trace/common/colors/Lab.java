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

package mega.trace.common.colors;

public record Lab(float L, float a, float b) {
    public static Lab fromRgb(Rgb rgb) {
        return fromRgb(rgb.r(), rgb.g(), rgb.b());
    }

    public static Lab fromRgb(float r, float g, float b) {
        float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
        float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
        float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;

        float l_ = (float) Math.pow(l, 1 / 3f);
        float m_ = (float) Math.pow(m, 1 / 3f);
        float s_ = (float) Math.pow(s, 1 / 3f);

        return new Lab(0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_, 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
                       0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_);
    }

    public static Lab fromLch(Lch lch) {
        return fromLch(lch.L(), lch.C(), lch.h());
    }

    public static Lab fromLch(float L, float C, float h) {
        return new Lab(L, (float) (C * Math.cos(h)), (float) (C * Math.sin(h)));
    }
}
