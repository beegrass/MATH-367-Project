from __future__ import annotations
from dataclasses import dataclass

def words_from_little_endian_bytes(b: bytes) -> list[int]:
    assert len(b) % 4 == 0
    return [int.from_bytes(b[i : i + 4], "little") for i in range(0, len(b), 4)]

def mask32(x: int) -> int:
    return x & 0xFFFFFFFF

def add32(x: int, y: int) -> int:
    return mask32(x + y)

def rightrotate32(x: int, n: int) -> int:
    return mask32(x << (32 - n)) | (x >> n)