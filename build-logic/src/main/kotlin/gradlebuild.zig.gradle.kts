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

import gradlebuild.ZigBuild
import gradlebuild.ZigInstall
import java.util.*

interface TargetPlatform : Named {
    val headers: ConfigurableFileCollection
    val sources: ConfigurableFileCollection
    val libcFile: RegularFileProperty
    val optimizer: Property<String>
}

interface ZigExtension {
    val zigVersion: Property<String>
    val outputDir: DirectoryProperty
    val targets: NamedDomainObjectContainer<TargetPlatform>
}

val extension = project.extensions.create<ZigExtension>("zig")

fun String.kebabToCamelCase() = split("-").joinToString("", transform = {
    it.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }
})

val zigInstall by tasks.registering(ZigInstall::class) {
    group = "build"
    description = "Installs Zig unless the right version is present on the path already"
    zigVersion.convention(extension.zigVersion)
    installDir.convention(layout.buildDirectory.dir("zig-install/unpack"))
    cacheDir.convention(layout.buildDirectory.dir("zig-install/cache"))
}

val zigBuild by tasks.registering {
    group = "build"
    description = "Builds the project with Zig"
}

tasks.withType<ZigBuild>().configureEach {
    executablePath = zigInstall.flatMap { it.executablePath }
    zigVersion = zigInstall.flatMap { it.zigVersion }
    // TODO This should not be required, it's only here because Provider.zip() does not carry dependencies
    dependsOn(zigInstall)
    workingDirectory = layout.projectDirectory
    target.convention("native")
    outputDirectory = extension.outputDir.dir(target)
}

afterEvaluate {
    extension.targets.configureEach {
        val target = this
        val task = tasks.register<ZigBuild>("zigBuild${target.name.kebabToCamelCase()}") {
            group = "zig"
            description = "Builds the project with Zig for $target"
            if (target.name != "native") {
                this.target = target.name
            }
            headers.from(target.headers)
            sources.from(target.sources)
            libcFile = target.libcFile
            optimizer = target.optimizer
        }
        zigBuild.configure { dependsOn(task) }
    }
}
