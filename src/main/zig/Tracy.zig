// This file is part of MEGATrace.
//
// Copyright (C) 2024 The MEGA Team
// All Rights Reserved
//
// The above copyright notice, this permission notice and the word "MEGA"
// shall be included in all copies or substantial portions of the Software.
//
// MEGATrace is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, only version 3 of the License.
//
// MEGATrace is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with MEGATrace.  If not, see <https://www.gnu.org/licenses/>.

const std = @import("std");
const jni = @import("jni");
const config = @import("config");
const tracy = @import("tracy");
const GPU = tracy.GPU;
var tracingAlloc: tracy.TracingAllocator = undefined;
var arenaAlloc: std.heap.ArenaAllocator = undefined;
var alloc: std.mem.Allocator = undefined;
var source_mutex: std.Thread.Mutex = .{};
var source_registry: std.StringHashMap(*const tracy.TracySourceLocationData) = undefined;
var zone_mutex: std.Thread.Mutex = .{};
var zone_registry: std.AutoHashMap(jni.jlong, tracy.ZoneContext) = undefined;
var counter: std.atomic.Value(jni.jlong) = undefined;

pub fn init(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    tracingAlloc = tracy.TracingAllocator.init(std.heap.c_allocator);
    arenaAlloc = std.heap.ArenaAllocator.init(tracingAlloc.allocator());
    alloc = arenaAlloc.allocator();
    source_registry = @TypeOf(source_registry).init(alloc);
    zone_registry = @TypeOf(zone_registry).init(alloc);
    counter = @TypeOf(counter).init(1);
    tracy.setThreadName("Main");
}

pub fn deinit(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    arenaAlloc.deinit();
}

pub fn frameMark(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    tracy.frameMark();
}

pub fn critical_frameMark() callconv(.c) void {
    tracy.frameMark();
}

pub fn jni_initZone(cEnv: *jni.cEnv, _: jni.jclass, jFunction: jni.jbyteArray, jFile: jni.jbyteArray, line: jni.jint, active: jni.jboolean, jName: jni.jbyteArray, color: jni.jint) callconv(.c) jni.jlong {
    const env = jni.JNIEnv.warp(cEnv);
    const name = getByteArray(env, jName);
    defer freeByteArray(env, jName, name);
    if (name == null)
        return 0;
    const function = getByteArray(env, jFunction);
    defer freeByteArray(env, jFunction, function);
    const file = getByteArray(env, jFile);
    defer freeByteArray(env, jFile, file);
    return initZone(function, file, line, active, name.?, color) catch 0;
}

pub fn critical_initZone(jFunctionL: jni.jint, jFunction: [*c]jni.jbyte, jFileL: jni.jint, jFile: [*c]jni.jbyte, line: jni.jint, active: jni.jboolean, jNameL: jni.jint, jName: [*c]jni.jbyte, color: jni.jint) callconv(.c) jni.jlong {
    const name = critical_getByteArray(jNameL, jName) orelse return 0;
    const function = critical_getByteArray(jFunctionL, jFunction);
    const file = critical_getByteArray(jFileL, jFile);
    return initZone(function, file, line, active, name, color) catch 0;
}

fn initZone(function: ?[:0]const u8, file: ?[:0]const u8, line: jni.jint, active: jni.jboolean, name: [:0]const u8, color: jni.jint) !jni.jlong {
    const source = blk: {
        while (!source_mutex.tryLock()) {}
        defer source_mutex.unlock();
        break :blk if (source_registry.get(name)) |k| k else blk2: {
            const key = (try cloneString(name)).?;
            const value = try alloc.create(tracy.TracySourceLocationData);
            value.* = .{
                .name = key,
                .file = @ptrCast(try cloneString(file)),
                .function = @ptrCast(try cloneString(function)),
                .color = @bitCast(color),
                .line = @bitCast(line),
            };
            try source_registry.put(key, value);
            break :blk2 value;
        };
    };
    const context = tracy.initZoneRaw(source, jni.jbooleanToBool(active));
    {
        while (!zone_mutex.tryLock()) {}
        defer zone_mutex.unlock();
        var i: jni.jlong = 0;
        while (i == 0) {
            i = counter.fetchAdd(1, .monotonic);
        }
        try zone_registry.put(i, context);
        return i;
    }
}

pub fn jni_deinitZone(_: *jni.cEnv, _: jni.jclass, i: jni.jlong) callconv(.c) void {
    deinitZone(i);
}

pub fn critical_deinitZone(i: jni.jlong) callconv(.c) void {
    deinitZone(i);
}

inline fn deinitZone(i: jni.jlong) void {
    while (!zone_mutex.tryLock()) {}
    defer zone_mutex.unlock();
    const zone: tracy.ZoneContext = (zone_registry.fetchRemove(i) orelse return).value;
    zone.deinit();
}

pub fn jni_gpu_allocSrcLoc(cEnv: *jni.cEnv, _: jni.jclass, jFile: jni.jbyteArray, jFunction: jni.jbyteArray, line: jni.jint, jName: jni.jbyteArray, color: jni.jint) callconv(.c) jni.jlong {
    const env = jni.JNIEnv.warp(cEnv);
    const function = getByteArray(env, jFunction);
    defer freeByteArray(env, jFunction, function);
    if (function == null)
        return 0;
    const file = getByteArray(env, jFile);
    defer freeByteArray(env, jFile, file);
    if (file == null)
        return 0;
    const name = getByteArray(env, jName);
    defer freeByteArray(env, jName, name);
    return @bitCast(tracy.allocSrcLoc(@bitCast(line), file.?, function.?, name, @bitCast(color)));
}

pub fn jni_gpu_beginZone(_: *jni.cEnv, _: jni.jclass, srcLoc: jni.jlong, queryId: jni.jshort, context: jni.jbyte) callconv(.c) void {
    GPU.beginZone(@bitCast(srcLoc), @bitCast(queryId), @bitCast(context));
}

pub fn critical_gpu_beginZone(srcLoc: jni.jlong, queryId: jni.jshort, context: jni.jbyte) void {
    GPU.beginZone(@bitCast(srcLoc), @bitCast(queryId), @bitCast(context));
}

pub fn jni_gpu_endZone(_: *jni.cEnv, _: jni.jclass, queryId: jni.jshort, context: jni.jbyte) callconv(.c) void {
    GPU.endZone(@bitCast(queryId), @bitCast(context));
}

pub fn critical_gpu_endZone(queryId: jni.jshort, context: jni.jbyte) void {
    GPU.endZone(@bitCast(queryId), @bitCast(context));
}

pub fn jni_gpu_time(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong, queryId: jni.jshort, context: jni.jbyte) callconv(.c) void {
    GPU.time(@bitCast(gpuTime), @bitCast(queryId), @bitCast(context));
}

pub fn critical_gpu_time(gpuTime: jni.jlong, queryId: jni.jshort, context: jni.jbyte) void {
    GPU.time(@bitCast(gpuTime), @bitCast(queryId), @bitCast(context));
}

pub fn jni_gpu_newContext(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong, period: jni.jfloat, context: jni.jbyte) callconv(.c) void {
    GPU.newContext(@bitCast(gpuTime), period, @bitCast(context), &.{}, GPU.ContextType.OpenGl);
}

pub fn critical_gpu_newContext(gpuTime: jni.jlong, period: jni.jfloat, context: jni.jbyte) void {
    GPU.newContext(@bitCast(gpuTime), period, @bitCast(context), &.{}, GPU.ContextType.OpenGl);
}

pub fn jni_gpu_calibration(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong, cpuDelta: jni.jlong, context: jni.jbyte) callconv(.c) void {
    GPU.calibration(@bitCast(gpuTime), @bitCast(cpuDelta), @bitCast(context));
}

pub fn critical_gpu_calibration(gpuTime: jni.jlong, cpuDelta: jni.jlong, context: jni.jbyte) void {
    GPU.calibration(@bitCast(gpuTime), @bitCast(cpuDelta), @bitCast(context));
}

pub fn jni_gpu_timeSync(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong, context: jni.jbyte) callconv(.c) void {
    GPU.timeSync(@bitCast(gpuTime), @bitCast(context));
}

pub fn critical_gpu_timeSync(gpuTime: jni.jlong, context: jni.jbyte) void {
    GPU.timeSync(@bitCast(gpuTime), @bitCast(context));
}

pub fn jni_frameImage(_: *jni.cEnv, _: jni.jclass, image: jni.jlong, width: jni.jshort, height: jni.jshort, offset: jni.jbyte, flip: jni.jboolean) callconv(.c) void {
    tracy.frameImage(@ptrFromInt(@as(usize, @bitCast(image))), @bitCast(width), @bitCast(height), @bitCast(offset), jni.jbooleanToBool(flip));
}

pub fn critical_frameImage(image: jni.jlong, width: jni.jshort, height: jni.jshort, offset: jni.jbyte, flip: jni.jboolean) void {
    tracy.frameImage(@ptrFromInt(@as(usize, @bitCast(image))), @bitCast(width), @bitCast(height), @bitCast(offset), jni.jbooleanToBool(flip));
}

// Utils

fn cloneString(str: ?[:0]const u8) !?[:0]const u8 {
    if (str == null)
        return null;
    const new = try alloc.allocSentinel(u8, str.?.len, 0);
    @memcpy(new, str.?);
    return new;
}

inline fn getByteArray(env: jni.JNIEnv, array: jni.jbyteArray) ?[:0]u8 {
    if (array == null)
        return null;
    var isCopy: bool = undefined;
    const length: u32 = @bitCast(env.getArrayLength(array));
    const arr = env.getPrimitiveArrayElements(jni.jbyte, array, &isCopy)[0..length];
    return @ptrCast(arr);
}

inline fn freeByteArray(env: jni.JNIEnv, array: jni.jbyteArray, arr: ?[:0]u8) void {
    if (array == null or arr == null)
        return;
    env.releasePrimitiveArrayElements(jni.jbyte, array, @ptrCast(arr.?), .JNIDefault);
}

inline fn critical_getByteArray(len: jni.jint, ptr: [*c]jni.jbyte) ?[:0]const u8 {
    if (ptr) |s| {
        const l: u32 = @bitCast(len);
        return @ptrCast(s[0..l]);
    } else {
        return null;
    }
}

/// Exports all functions with C calling convention from the provided `func_struct` type to be
/// accessible from Java using the JNI.
fn exportJNICritical(comptime class_name: []const u8, comptime func_struct: type) void {
    inline for (@typeInfo(func_struct).@"struct".decls) |decl| {
        const func = comptime @field(func_struct, decl.name);
        const func_type = @TypeOf(func);

        // If it is not a function, skip.
        if (@typeInfo(func_type) != .@"fn") {
            continue;
        }


        // If it is not a function with calling convention .C, skip.
        if (!@typeInfo(func_type).@"fn".calling_convention.eql(.c)) {
            continue;
        }

        var name = decl.name;

        if (std.mem.startsWith(u8, name, "jni_")) {
            name = name["jni_".len..];
        }
        var critical = false;
        if (std.mem.startsWith(u8, name, "critical_")) {
            critical = true;
            name = name["critical_".len..];
        }

        const tmp_name: []const u8 = comptime ((if (critical) "JavaCritical." else "Java.") ++ class_name ++ "." ++ name);
        var export_name: [tmp_name.len]u8 = undefined;

        @setEvalBranchQuota(30000);

        _ = comptime std.mem.replace(u8, tmp_name, ".", "_", export_name[0..]);

        @export(&func, .{
            .name = export_name[0..],
            .linkage = .strong,
        });
    }
}

// Registration

comptime {
    exportJNICritical(config.root_pkg ++ ".natives.Tracy", @This());
}