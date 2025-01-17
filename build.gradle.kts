import gradlebuild.ZigBuild

plugins {
    id("fpgradle-minecraft") version("0.10.0")
    id("gradlebuild.zig")
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

repositories {
    exclusive(mavenpattern(), "com.falsepattern", "makamys")
    exclusive(mavenCentral(), "it.unimi.dsi")
}

dependencies {
    implementationSplit("com.falsepattern:falsepatternlib-mc1.7.10:1.5.9")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("makamys:neodymium-mc1.7.10:0.4.3-unofficial:dev")
}

val zigOutDir = layout.buildDirectory.dir("zig")

zig {
    zigVersion = "0.14.0-dev.2649+77273103a"
    outputDir = zigOutDir
    targets {
        create("x86_64-linux-gnu")
        create("aarch64-linux-gnu")
        create("x86_64-windows-gnu")
//        create("aarch64-windows-gnu") TODO tracy is unhappy on this triple
//        create("x86_64-macos-none") TODO MacOS
    }.configureEach {
        optimizer = "ReleaseSmall"
        sources.from(layout.projectDirectory.dir("src/main/zig"))
        sources.from(layout.projectDirectory.dir("build.zig"))
        sources.from(layout.projectDirectory.dir("build.zig.zon"))
    }
}
tasks.withType<ZigBuild>().all {
    val zigBuild = this
    val isWindows = zigBuild.target.map { it.contains("windows") }
    val zigClean = tasks.register<Delete>("zigClean${zigBuild.name.removePrefix("zigBuild")}")
    zigBuild.dependsOn(zigClean)

    val mod = minecraft_fp.mod;
    extraArgs.add(mod.modid.map { "-Dmod_id=$it" })
    extraArgs.add(mod.name.map { "-Dmod_name=$it" })
    extraArgs.add(mod.version.map { "-Dmod_version=$it" })
    extraArgs.add(mod.rootPkg.map { "-Droot_pkg=$it" })
//    extraArgs.add("-Dstrip")

    zigClean.configure {
        group = "zig"
        delete(zigBuild.outputDirectory)
    }
    tasks.named<ProcessResources>("processResources") {
        into("/natives") {
            from(zigBuild.outputDirectory.dir(isWindows.map { if (it) "bin" else "lib" }))
            include("*.dll", "*.so")
            rename("(\\w+)\\.(dll|so)", "$1-${zigBuild.target.get()}.$2")
        }
    }
}