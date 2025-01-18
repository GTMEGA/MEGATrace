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

package mega.trace;

import lombok.val;
import mega.trace.natives.Tracy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import java.util.concurrent.atomic.AtomicReference;

@Mod(modid = Tags.MOD_ID,
     version = Tags.MOD_VERSION,
     name = Tags.MOD_NAME,
     acceptedMinecraftVersions = "[1.7.10]",
     acceptableRemoteVersions = "*",
     dependencies = "required-after:falsepatternlib@[1.5.9,);")
public class MEGATrace {
    private static volatile boolean nativesLoaded;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        initNatives();
    }

    public static synchronized void initNatives() {
        if (nativesLoaded) {
            return;
        }

        Share.log.info("Attempting to load natives");

        try {
            val internalErr = new AtomicReference<Throwable>();
            // This is done on a seperate thread, as Tracy will treat the thread on which `Tracy.load()` is called
            // As the "Main Thread" and does not support renaming it. Launching it on a separate thread lets us
            // work around this limitation, so we can mark the `client` and `server` threads.
            val tracyInitThread = new Thread(() -> {
                try {
                    Tracy.load();
                    Tracy.init();
                } catch (Throwable t) {
                    internalErr.set(t);
                }
            });
            tracyInitThread.setName("Tracy Init");

            tracyInitThread.start();
            tracyInitThread.join(5_000);

            val t = internalErr.get();
            if (t != null) {
                throw t;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load natives", t);
        }

        val tracyDeinitThread = new Thread(Tracy::deinit);
        tracyDeinitThread.setName("Tracy Deinit");
        Runtime.getRuntime().addShutdownHook(tracyDeinitThread);

        Share.log.info("Successfully loaded natives");
        nativesLoaded = true;
    }
}
