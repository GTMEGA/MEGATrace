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

package gradlebuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class ZigBuild @Inject constructor(@Inject val exec: ExecOperations) : DefaultTask() {
    @get:Internal
    abstract val executablePath: Property<String>

    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:Optional
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Optional
    @get:LocalState
    abstract val cacheDirectory: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val libcFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headers: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val target: Property<String>

    @get:Optional
    @get:Input
    abstract val optimizer: Property<String>

    @get:Optional
    @get:Input
    abstract val extraArgs: ListProperty<String>

    @TaskAction
    fun execute() {
        exec.exec {
            executable = executablePath.get()
            args("build", "install")
            if (outputDirectory.isPresent) {
                args("--prefix", outputDirectory.get().asFile.absolutePath)
            }
            if (cacheDirectory.isPresent) {
                args("--cache-dir", cacheDirectory.get().asFile.absolutePath)
            }
            headers.forEach { headerDir ->
                args("--search-prefix", headerDir.absolutePath)
            }
            if (target.isPresent) {
                args("-Dtarget=${target.get()}")
            }
            if (libcFile.isPresent) {
                args("--libc", libcFile.get().asFile.absolutePath)
            }
            if (optimizer.isPresent) {
                args("-Doptimize=${optimizer.get()}")
            }
            if (extraArgs.isPresent) {
                args(extraArgs.get())
            }
            workingDir = workingDirectory.get().asFile
        }
    }
}