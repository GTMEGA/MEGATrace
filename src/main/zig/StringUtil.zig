const std = @import("std");
pub inline fn cloneString(input: [:0]const u8, allocator: std.mem.Allocator) ![:0]const u8 {
    const newMem = try allocator.allocSentinel(u8, input.len, 0);
    @memcpy(newMem, input);
    return newMem;
}
