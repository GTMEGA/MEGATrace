const std = @import("std");

pub fn build(b: *std.Build) !void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});
    const strip = b.option(bool, "strip", "Strip debug symbols from the resulting binaries to minimize size");
    const opt = Opt.of(b);
    opt.add([]const u8, "mod_id", "Mod ID", "examplemod");
    opt.add([]const u8, "mod_name", "Mod Name", "Example Mod");
    opt.add([]const u8, "mod_version", "Mod Version", "0.0.0");
    opt.add([]const u8, "root_pkg", "Root package of the mod", "com.example");

    const JNI = b.dependency("JNI", .{
        .target = target,
        .optimize = optimize,
    });
    const tracy = b.dependency("zig-tracy", .{
        .target = target,
        .optimize = optimize,
        .tracy_enable = true,
        .tracy_no_context_switch = true,
        .tracy_no_callstack = true,
        .tracy_no_crash_handler = true,
    });

    const lib = b.addSharedLibrary(.{
        .name = "Tracy",
        .root_source_file = b.path("src/main/zig/Tracy.zig"),
        .target = target,
        .optimize = optimize,
        .strip = strip,
    });

    lib.root_module.addOptions("config", opt.opt);
    lib.root_module.addImport("jni", JNI.module("JNI"));
    lib.root_module.addImport("tracy", tracy.module("tracy"));
    const tracyLib = tracy.artifact("tracy");
    tracyLib.root_module.strip = strip;
    lib.linkLibrary(tracyLib);
    lib.linkLibCpp();

    b.installArtifact(lib);
}

const Opt = struct {
    b: *std.Build,
    opt: *std.Build.Step.Options,

    pub fn of(b: *std.Build) @This() {
        return .{ .b = b, .opt = b.addOptions() };
    }

    pub fn add(self: @This(), comptime T: type, name: []const u8, description: []const u8, default: T) void {
        const value = self.b.option(T, name, description) orelse default;
        self.opt.addOption(T, name, value);
    }
};
