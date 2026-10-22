#!/usr/bin/env python3
"""Fail if a real Android PNG has no visible blue water in the canvas.

This supplements shader-init logs; it is not a substitute for human review.
Standard library only. Screen pixels may be 360x800 or an API26 framebuffer.
"""
import struct
import sys
import zlib

def water_pixels(path):
    data = open(path, "rb").read()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Not an Android PNG")
    pos = 8
    blocks = []
    width = height = channels = None
    while pos + 12 <= len(data):
        n = int.from_bytes(data[pos:pos + 4], "big")
        kind = data[pos + 4:pos + 8]
        content = data[pos + 8:pos + 8 + n]
        pos += n + 12
        if kind == b"IHDR":
            width, height, bitdepth, color, _, _, interlace = struct.unpack(">IIBBBBB", content)
            if bitdepth != 8 or color not in (2, 6) or interlace:
                raise ValueError("Unsupported screencap format")
            channels = 3 if color == 2 else 4
        elif kind == b"IDAT":
            blocks.append(content)
        elif kind == b"IEND":
            break
    if not blocks or channels is None:
        raise ValueError("Missing image data")
    raw = zlib.decompress(b"".join(blocks))
    stride = width * channels
    if len(raw) != (stride + 1) * height:
        raise ValueError("Truncated PNG")
    previous = bytearray(stride)
    off = count = 0
    for y in range(height):
        mode = raw[off]
        off += 1
        row = bytearray(raw[off:off + stride])
        off += stride
        if mode not in range(5):
            raise ValueError("Unexpected PNG filter")
        for i in range(stride):
            a = row[i - channels] if i >= channels else 0
            b = previous[i]
            c = previous[i - channels] if i >= channels else 0
            if mode == 1:
                predictor = a
            elif mode == 2:
                predictor = b
            elif mode == 3:
                predictor = (a + b) // 2
            elif mode == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                predictor = a if pa <= pb and pa <= pc else b if pb <= pc else c
            else:
                predictor = 0
            row[i] = (row[i] + predictor) & 255
        # Exclude the colored material shelf, dock and emulator sidebars.
        if int(height * .06) <= y < int(height * .78):
            for x in range(int(width * .07), int(width * .92)):
                i = x * channels
                red, green, blue = row[i:i + 3]
                if blue >= 65 and blue >= red + 30 and blue >= green + 15:
                    count += 1
        previous = row
    return count, width, height

for filename in sys.argv[1:]:
    pixels, width, height = water_pixels(filename)
    needed = max(65, round(width * height * .001))
    print(f"{filename}: water-like pixels={pixels}, minimum={needed}, framebuffer={width}x{height}")
    if pixels < needed:
        sys.exit(f"No visible blue water in canvas: {filename}")
