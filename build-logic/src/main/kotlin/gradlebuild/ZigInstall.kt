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

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject

abstract class ZigInstall @Inject constructor(@Inject val exec: ExecOperations) : DefaultTask() {
    @get:Input
    abstract val zigVersion: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @get:LocalState
    abstract val cacheDir: DirectoryProperty

    @get:Internal
    val executablePath: Provider<String>
        get() = installDir.zip(zigVersion) { installDir, zigVersion ->
            val executable = installDir.asFile.zigExecutablePath(zigVersion)
            if (executable.isFile) {
                executable.absolutePath
            } else {
                "zig"
            }
        }

    @TaskAction
    fun execute() {
        val installDir = this.installDir.get().asFile
        try {
            val outputStream = ByteArrayOutputStream()
            val result = exec.exec {
                commandLine = listOf("zig", "version")
                standardOutput = outputStream
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                val installedVersion = outputStream.toString().trim()
                println("Found Zig ${installedVersion}")
                if (installedVersion == zigVersion.get()) {
                    installDir.deleteRecursively()
                    return
                }
            }
        } catch (e: Exception) {
            println("Zig not found on path")
        }

        // Install Zig
        println("Installing Zig ${zigVersion.get()} for ${os()} ${arch()}")
        val cacheRoot = cacheDir.get().asFile
        cacheRoot.mkdirs()
        val zigArchive = cacheRoot.resolve("${zigArchiveName(zigVersion.get())}")
        if (zigVersion.get().contains('-')) {
          downloadFile("https://ziglang.org/builds/${zigArchiveName(zigVersion.get())}", zigArchive)
        } else {
          downloadFile("https://ziglang.org/download/${zigVersion.get()}/${zigArchiveName(zigVersion.get())}", zigArchive)
        }
        unpackArchiveGeneric(zigArchive, installDir)
        val executable = installDir.zigExecutablePath(zigVersion.get())
        executable.setExecutable(true, false)
    }

    fun downloadFile(url: String, destination: File) {
        URL(url).openStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun unpackArchive(archive: ArchiveInputStream<*>, outputDir: File) {
        var entry = archive.nextEntry
        while (entry != null) {
            val outputFile = File(outputDir, entry.name)
            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile.mkdirs() // Create parent directories if they don't exist
                outputFile.outputStream().use { output ->
                    archive.copyTo(output)
                }
            }
            entry = archive.nextEntry
        }
    }

    fun unpackArchiveGeneric(file: File, outputDir: File) {
        if (file.extension == "zip") {
            unpackZip(file, outputDir)
        } else if (file.extension == "xz") {
            unpackTarXz(file, outputDir)
        } else {
            throw IllegalArgumentException("Unsupported file extension ${file.extension}")
        }
    }

    fun unpackZip(file: File, outputDir: File) {
        ZipArchiveInputStream(BufferedInputStream(FileInputStream(file))).use { zIn ->
            unpackArchive(zIn, outputDir)
        }
    }

    fun unpackTarXz(file: File, outputDir: File) {
        XZCompressorInputStream(BufferedInputStream(FileInputStream(file))).use { xzIn ->
            TarArchiveInputStream(xzIn).use { tarIn ->
                unpackArchive(tarIn, outputDir)
            }
        }
    }

    private fun File.zigExecutablePath(version: String): File =
        resolve("${zigName(version)}/zig${if (isWindows) ".exe" else ""}")

    private fun zigArchiveName(zigVersion: String) =
        "${zigName(zigVersion)}.${if (isWindows) "zip" else "tar.xz"}"

    private fun zigName(zigVersion: String) =
        "zig-${os()}-${arch()}-${zigVersion}"

    private fun os(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> "macos"
            os.contains("win") -> "windows"
            os.contains("linux") -> "linux"
            else -> error("Unsupported OS: $os")
        }
    }

    private val isWindows: Boolean get() {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    private fun arch(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
            arch.contains("aarch64") -> "aarch64"
            else -> error("Unsupported architecture: $arch")
        }
    }
}