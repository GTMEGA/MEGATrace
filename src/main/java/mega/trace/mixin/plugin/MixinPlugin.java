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

package mega.trace.mixin.plugin;

import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.IMixinPlugin;
import com.falsepattern.lib.mixin.ITargetedMod;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;
import mega.trace.Share;
import mega.trace.Tags;
import mega.trace.natives.Tracy;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

@Accessors(fluent = false)
public class MixinPlugin implements IMixinPlugin {
    @Getter
    private final Logger logger = IMixinPlugin.createLogger(Tags.MOD_NAME + " Init");

    @Override
    public ITargetedMod[] getTargetedModEnumValues() {
        return TargetedMod.values();
    }

    @Override
    public IMixin[] getMixinEnumValues() {
        return Mixin.values();
    }

    @Override
    public boolean useNewFindJar() {
        return true;
    }

    // region Init Natives
    static {
        initNatives();
    }

    private static void initNatives() {
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

            //TODO: Starting threading during class loading doesn't work so good, put in regular constructor
            tracyInitThread.start();
            tracyInitThread.join(5_000);

            val t = internalErr.get();
            if (t != null)
                throw t;
        } catch (Throwable t) {
            Share.log.warn("Failed to load natives", t);
            return;
        }

        val tracyDeinitThread = new Thread(Tracy::deinit);
        tracyDeinitThread.setName("Tracy Deinit");
        Runtime.getRuntime().addShutdownHook(tracyDeinitThread);

        Share.log.info("Successfully loaded natives");
    }
    // endregion
}
