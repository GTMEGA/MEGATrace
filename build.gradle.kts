import com.falsepattern.zigbuild.options.ZigBuildOptions
import com.falsepattern.zigbuild.target.ZigOperatingSystemTarget
import com.falsepattern.zigbuild.target.ZigTargetTriple
import com.falsepattern.zigbuild.tasks.ZigBuildTask
import com.falsepattern.zigbuild.toolchain.ZigVersion

plugins {
    id("com.falsepattern.fpgradle-mc") version "0.15.1"
    id("com.falsepattern.zigbuild")
}

group = "mega"

minecraft_fp {
    java {
        compatibility = modern
    }
    mod {
        modid   = "megatrace"
        name    = "MEGATrace"
        rootPkg = "$group.trace"
    }
    mixin {
        pkg           = "mixin.mixins"      //optional
        pluginClass   = "mixin.plugin.MixinPlugin" //optional, requires pkg set
    }
    tokens {
        tokenClass = "Tags"
    }
    publish {
        changelog = "https://github.com/GTMEGA/MEGATrace/releases/tag/${version}"
        maven {
            repoUrl  = "https://mvn.falsepattern.com/gtmega_releases/"
            repoName = "mega"
        }
    }
}

zig {
    toolchain {
        version = ZigVersion.of("0.14.0")
    }
    defaultCacheDirs = false
}

val targets = listOf(
    ZigTargetTriple.X86_64_LINUX_GNU,
    ZigTargetTriple.AARCH64_LINUX_GNU,
    ZigTargetTriple.X86_64_WINDOWS_GNU,
    ZigTargetTriple.AARCH64_WINDOWS_GNU
//    ZigTargetTriple.X86_64_MACOS_NONE TODO MacOS
)

targets.forEach { target ->
    val targetStr = target.resolve()
    val prefix = layout.buildDirectory.dir("zig-build/$targetStr")
    val mod = minecraft_fp.mod
    val buildTask = tasks.register<ZigBuildTask>("buildNatives-$targetStr") {
        options {
            steps.add("install")
            this.target = target
            optimize = ZigBuildOptions.Optimization.ReleaseSmall
            compilerArgs.add(mod.modid.map { "-Dmod_id=$it" })
            compilerArgs.add(mod.name.map { "-Dmod_name=$it" })
            compilerArgs.add(mod.version.map { "-Dmod_version=$it" })
            compilerArgs.add(mod.rootPkg.map { "-Droot_pkg=$it" })
            compilerArgs.add("-Dstrip")
        }
        workingDirectory = layout.projectDirectory
        prefixDirectory = prefix
        clearPrefixDirectory = true
        sourceFiles.from(layout.projectDirectory.dir("src/main/zig"))
        sourceFiles.from(layout.projectDirectory.file("build.zig"))
        sourceFiles.from(layout.projectDirectory.file("build.zig.zon"))
    }
    tasks.named<ProcessResources>("processResources") {
        dependsOn(buildTask)
        into("/natives") {
            if (target.os == ZigOperatingSystemTarget.WINDOWS) {
                from(prefix.map { it.dir("bin") })
            } else {
                from(prefix.map { it.dir("lib") })
            }
            include("*.dll", "*.so")
            rename("(\\w+)\\.(dll|so)", "$1-${targetStr}.$2")
        }
    }
}

repositories {
    exclusive(mavenpattern(), "com.falsepattern", "makamys")
    exclusive(mavenCentral(), "it.unimi.dsi")
    exclusive(mega_uploads(), "optifine")
    exclusive(mega(), "mega")
}

dependencies {
    implementation("mega:megatraceservice:1.2.0")
    implementationSplit("com.falsepattern:falsepatternlib-mc1.7.10:1.5.9")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("makamys:neodymium-mc1.7.10:0.4.3-unofficial:dev")
    compileOnly(deobf("optifine:optifine:1.7.10_hd_u_e7"))
}