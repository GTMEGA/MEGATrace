// This file is part of MEGATrace.
//
// Copyright (C) 2024-2025 The MEGA Team
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
const StringUtil = @import("StringUtil.zig");
const intern_pool = @import("intern_pool.zig");

const default_gpu_context = 0;
const default_file = "Frame";
const default_function = "Frame";
const default_line = 0;

const cpu_zone_error = -1;

const gpu_zone_error = 0;

var arena_alloc: std.heap.ArenaAllocator = undefined;
var alloc: std.mem.Allocator = undefined;

var name_intern_pool: intern_pool.StringInternPool = undefined;
var message_intern_pool: intern_pool.StringInternPool = undefined;
var source_location_pool: SourceLocationInternPool = undefined;
var gpu_query_id_counter =  std.atomic.Value(u16).init(0);

const SourceLocationKey = struct {
    name: [:0]const u8,
    color: u32
};
const SourceLocationInternPool = intern_pool.InternPool(SourceLocationKey, *const tracy.TracySourceLocationData, struct {
    pub const HashMapContext = struct {
        pub fn hash(_: @This(), s: SourceLocationKey) u64 {
            var hasher = std.hash.Wyhash.init(0);
            hasher.update(s.name);
            hasher.update(std.mem.asBytes(&s.color));
            return hasher.final();
        }

        pub fn eql(_: @This(), a: SourceLocationKey, b: SourceLocationKey) bool {
            return std.mem.eql(u8, a.name, b.name) and a.color == b.color;
        }
    };
    pub inline fn cloneKey(key: SourceLocationKey, _: std.mem.Allocator) !SourceLocationKey {
        //Already interned
        return key;
    }

    pub inline fn cloneValue(_: SourceLocationKey, clonedKey: SourceLocationKey, value: *const tracy.TracySourceLocationData, allocator: std.mem.Allocator) !*const tracy.TracySourceLocationData {
        const new = try allocator.create(tracy.TracySourceLocationData);
        new.name = clonedKey.name;
        new.function = default_function;
        new.file = default_file;
        new.line = default_line;
        new.color = value.color;
        return new;
    }
});

const GPUTracyLocationData = struct {
    name: [:0]const u8,
    color: u32,
};

pub fn jni_init(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    tracy.startupProfiler();
    arena_alloc = std.heap.ArenaAllocator.init(std.heap.c_allocator);
    alloc = arena_alloc.allocator();
    message_intern_pool = intern_pool.StringInternPool.init(alloc);
    name_intern_pool = intern_pool.StringInternPool.init(alloc);
    source_location_pool = SourceLocationInternPool.init(alloc);
}

pub fn jni_deinit(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    tracy.shutdownProfiler();
    source_location_pool.deinit();
    name_intern_pool.deinit();
    message_intern_pool.deinit();
    arena_alloc.deinit();
}

pub fn jni_frameMark(_: *jni.cEnv, _: jni.jclass) callconv(.c) void {
    tracy.frameMark();
}

pub fn critical_frameMark() callconv(.c) void {
    tracy.frameMark();
}

pub fn jni_beginZone(cEnv: *jni.cEnv, _: jni.jclass, jName: jni.jbyteArray, jColor: jni.jint) callconv(.c) jni.jlong {
    const env = jni.JNIEnv.warp(cEnv);
    const name = getByteArray(env, jName) orelse return cpu_zone_error;
    defer freeByteArray(env, jName, name);
    const color: u32 = @bitCast(jColor);
    const zone = beginZone(name, color) catch return cpu_zone_error;
    return @bitCast(zone);
}

pub fn critical_beginZone(jNameL: jni.jint, jName: [*c]jni.jbyte, jColor: jni.jint) callconv(.c) jni.jlong {
    const name = critical_getByteArray(jNameL, jName) orelse return cpu_zone_error;
    const color: u32 = @bitCast(jColor);
    const zone = beginZone(name, color) catch return cpu_zone_error;
    return @bitCast(zone);
}

pub fn jni_message(cEnv: *jni.cEnv, _: jni.jclass, jMsg: jni.jbyteArray) callconv(.c) void {
    const env = jni.JNIEnv.warp(cEnv);
    const msg = getByteArray(env, jMsg) orelse return;
    defer freeByteArray(env, jMsg, msg);
    message(msg) catch {};
}

pub fn critical_message(jMsgL: jni.jint, jMsg: [*c]jni.jbyte) callconv(.c) void {
    const msg = critical_getByteArray(jMsgL, jMsg) orelse return;
    message(msg) catch {};
}

fn message(msg: [:0]const u8) !void {
    const interned_msg = try message_intern_pool.intern(msg, msg);
    tracy.messageRaw(interned_msg);
}


pub fn jni_messageColor(cEnv: *jni.cEnv, _: jni.jclass, jMsg: jni.jbyteArray, jColor: jni.jint) callconv(.c) void {
    const env = jni.JNIEnv.warp(cEnv);
    const msg = getByteArray(env, jMsg) orelse return;
    defer freeByteArray(env, jMsg, msg);
    const color: u32 = @bitCast(jColor);
    messageColor(msg, color) catch {};
}

pub fn critical_messageColor(jMsgL: jni.jint, jMsg: [*c]jni.jbyte, jColor: jni.jint) callconv(.c) void {
    const msg = critical_getByteArray(jMsgL, jMsg) orelse return;
    const color: u32 = @bitCast(jColor);
    messageColor(msg, color) catch {};
}

fn messageColor(msg: [:0]const u8, color: u32) !void {
    const interned_msg = try message_intern_pool.intern(msg, msg);
    tracy.messageColorRaw(interned_msg, color);
}

fn beginZone(name: [:0]const u8, color: u32) !u64 {
    const interned_name = try name_intern_pool.intern(name, name);
    const key = SourceLocationKey{
        .name = interned_name,
        .color = color
    };
    const source = try source_location_pool.intern(key, &tracy.TracySourceLocationData{
        .name = interned_name,
        .function = default_function,
        .file = default_file,
        .line = default_line,
        .color = color,
    });
    const context = tracy.initZoneRaw(source, true);
    return @bitCast(context);
}

pub fn jni_endZone(_: *jni.cEnv, _: jni.jclass, i: jni.jlong) callconv(.c) void {
    if (i == cpu_zone_error)
        return;
    endZone(@bitCast(i));
}

pub fn critical_endZone(i: jni.jlong) callconv(.c) void {
    if (i == cpu_zone_error)
        return;
    endZone(@bitCast(i));
}

fn endZone(i: u64) void {
    const zone: tracy.ZoneContext = @bitCast(i);
    zone.deinit();
}

pub fn jni_gpuInit(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong) callconv(.c) void {
    GPU.newContext(gpuTime, 1.0, default_gpu_context, &.{}, GPU.ContextType.OpenGl);
}

pub fn jni_gpuTimeSync(_: *jni.cEnv, _: jni.jclass, gpuTime: jni.jlong) callconv(.c) void {
    GPU.timeSync(@bitCast(gpuTime), default_gpu_context);
}

pub fn critical_gpuTimeSync(gpuTime: jni.jlong) callconv(.c) void {
    GPU.timeSync(@bitCast(gpuTime), default_gpu_context);
}

pub fn jni_gpuBeginZone(cEnv: *jni.cEnv, _: jni.jclass, jName: jni.jbyteArray, jColor: jni.jint) callconv(.c) jni.jshort {
    const env = jni.JNIEnv.warp(cEnv);
    const name = getByteArray(env, jName) orelse return gpu_zone_error;
    defer freeByteArray(env, jName, name);
    const color: u32 = @bitCast(jColor);
    const query_id = gpuBeginZone(name, color) catch return gpu_zone_error;
    return @bitCast(query_id);
}

pub fn critical_gpuBeginZone(jNameL: jni.jint, jName: [*c]jni.jbyte, jColor: jni.jint) callconv(.c) jni.jshort {
    const name = critical_getByteArray(jNameL, jName) orelse return gpu_zone_error;
    const color: u32 = @bitCast(jColor);
    const query_id = gpuBeginZone(name, color) catch return gpu_zone_error;
    return @bitCast(query_id);
}

fn gpuBeginZone(name: [:0]const u8, color: u32) !u16 {
    var query_id: u16 = gpu_zone_error;
    while (query_id == gpu_zone_error) {
        query_id = gpu_query_id_counter.fetchAdd(1, .monotonic);
    }
    const interned_name = try name_intern_pool.intern(name, name);
    const gpu_loc = tracy.allocSrcLoc(default_line, default_file, default_function, interned_name, color);
    GPU.beginZone(gpu_loc, query_id, default_gpu_context);
    return query_id;
}

pub fn jni_gpuEndZone(_: *jni.cEnv, _: jni.jclass) callconv(.c) jni.jshort {
    return @bitCast(gpuEndZone());
}

pub fn critical_gpuEndZone() callconv(.c) jni.jshort {
    return @bitCast(gpuEndZone());
}

fn gpuEndZone() u16 {
    var query_id: u16 = gpu_zone_error;
    while (query_id == gpu_zone_error) {
        query_id = gpu_query_id_counter.fetchAdd(1, .monotonic);
    }
    GPU.endZone(query_id, default_gpu_context);
    return query_id;
}

pub fn jni_gpuTime(_: *jni.cEnv, _: jni.jclass, query_id: jni.jshort, gpu_time: jni.jlong) callconv(.c) void {
    GPU.time(@bitCast(gpu_time), @bitCast(query_id), default_gpu_context);
}

pub fn critical_gpuTime(query_id: jni.jshort, gpu_time: jni.jlong) callconv(.c) void {
    GPU.time(@bitCast(gpu_time), @bitCast(query_id), default_gpu_context);
}

pub fn jni_frameImage(_: *jni.cEnv, _: jni.jclass, offset: jni.jbyte, image: jni.jlong, width: jni.jshort, height: jni.jshort) callconv(.c) void {
    tracy.frameImage(@ptrFromInt(@as(usize, @bitCast(image))), @bitCast(width), @bitCast(height), @bitCast(offset), true);
}

pub fn critical_frameImage(offset: jni.jbyte, image: jni.jlong, width: jni.jshort, height: jni.jshort) callconv(.c) void {
    tracy.frameImage(@ptrFromInt(@as(usize, @bitCast(image))), @bitCast(width), @bitCast(height), @bitCast(offset), true);
}

// Utils

inline fn getByteArray(env: jni.JNIEnv, array: jni.jbyteArray) ?[:0]u8 {
    if (array == null)
        return null;
    var isCopy: bool = undefined;
    const length: u32 = @bitCast(env.getArrayLength(array));
    const arr = env.getPrimitiveArrayElements(jni.jbyte, array, &isCopy)[0..length];
    return @ptrCast(arr);
}

inline fn freeByteArray(env: jni.JNIEnv, array: jni.jbyteArray, arr: [:0]u8) void {
    env.releasePrimitiveArrayElements(jni.jbyte, array, @ptrCast(arr.ptr), .JNIDefault);
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