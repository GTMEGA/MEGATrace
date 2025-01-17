const std = @import("std");
const StringUtil = @import("StringUtil.zig");

/// Context members:
/// HashMapContext -- a context for std.HashMap
/// fn cloneKey(key: K, allocator: std.mem.Allocator) !K
/// fn cloneValue(sourceKey: K, clonedKey: K, value: V, allocator: std.mem.Allocator) !V
pub fn InternPool(comptime K: type, comptime V: type, comptime Context: type) type {
    const HashMap = std.HashMap(K, V, Context.HashMapContext, std.hash_map.default_max_load_percentage);
    return struct {
        const This = @This();

        mutex: std.Thread.Mutex = .{},
        allocator: std.mem.Allocator,
        pool: HashMap,

        pub fn init(allocator: std.mem.Allocator) This {
            return This {
                .allocator = allocator,
                .pool = HashMap.init(allocator)
            };
        }

        pub fn deinit(this: *This) void {
            this.pool.deinit();
        }

        pub fn intern(this: *This, key: K, value: V) !V {
            while (!this.mutex.tryLock()) {}
            defer this.mutex.unlock();
            const result = try this.pool.getOrPut(key);
            if (!result.found_existing) {
                const newKey = try Context.cloneKey(key, this.allocator);
                const newValue = try Context.cloneValue(key, newKey, value, this.allocator);
                result.key_ptr.* = newKey;
                result.value_ptr.* = newValue;
                return newValue;
            } else {
                return result.value_ptr.*;
            }
        }
    };
}

inline fn stringCopy(dest: []u8, source: []const u8) void {
    @memcpy(dest, source);
}

const StringContext = struct {
    pub const HashMapContext = std.hash_map.StringContext;
    pub inline fn cloneKey(key: [:0]const u8, allocator: std.mem.Allocator) ![:0]const u8 {
        return StringUtil.cloneString(key, allocator);
    }

    pub inline fn cloneValue(sourceKey: [:0]const u8, clonedKey: [:0]const u8, value: [:0]const u8, allocator: std.mem.Allocator) ![:0]const u8 {
        if (value.ptr == sourceKey.ptr) {
            return clonedKey;
        }

        return StringUtil.cloneString(value, allocator);
    }
};

pub const StringInternPool = InternPool([:0]const u8, [:0]const u8, StringContext);