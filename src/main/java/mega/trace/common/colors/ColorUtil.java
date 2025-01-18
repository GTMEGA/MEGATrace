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

import lombok.val;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * OKLCH <-> OKLAB <-> RGB color conversions and palette processing/generation
 */
public class ColorUtil {
    /**
     * Color scheme generator
     */
    public static void main(String[] args) throws IOException {
        float TAU = (float) (Math.PI * 2);
        val img = new BufferedImage(800, 3200, BufferedImage.TYPE_INT_RGB);
        val gfx = img.createGraphics();
        val rng = new Random();

        for (int N = 0; N < 32; N++) {
            float L = rng.nextFloat(0.7f, 0.9f);
            float C = rng.nextFloat(0.05f, 0.1f);
            float h = rng.nextFloat() * TAU;

            float sL = rng.nextFloat(0.001f, 0.002f);
            float sH = rng.nextFloat(0.02f, 0.05f) * TAU;
            System.out.println("L: " + L + " C: " + C + " H: " + h + " sL: " + sL + " sH: " + sH);
            for (int i = 0; i < 8; i++) {
                int offset = i * 100;
                var rgb = Rgb.fromLab(Lab.fromLch(L, C, h)).clamp(0, 1);
                gfx.setColor(new Color(rgb.r(), rgb.g(), rgb.b()));
                gfx.fillRect(offset, N * 100, 800, 100);
                L -= sL;
                h += sH;
            }
        }
        ImageIO.write(img, "png", new File("trace.png"));
    }
}
