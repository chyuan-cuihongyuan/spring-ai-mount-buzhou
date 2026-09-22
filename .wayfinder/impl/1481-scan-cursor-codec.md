# impl 1481 — ScanCursorCodec SCAN 游标编解码（R81 = effort #1880 / spec 1880 / T2961-T2962）

**What**：`ScanCursorCodec`（core/cache 静态纯函数）——nextCursor
（位反转空间 +1：v′=rev(rev(v)+1)，绕满精确归 0）+ isComplete
（0 判完成）+ cycleLength（2^bits）；位宽 [1,63] 游标非负 fail-fast。

**Why**：Redis SCAN 的游标不是偏移量而是位反转计数器——全周游不重
不漏、rehash 桶序对齐迁移序；白盒计算面让「全周游保证」可测可证
（bits=2 序 0→2→1→3→0 实证）。落轮 grep 复核仅 Lettuce 客户端
类型在用，无占坑。

**Verify**：`ScanCursorCodecTest` 4 用例全绿（经典序/8 桶不重全覆盖/
反转空间严格递增/幂与畸形三型 fail-fast）。

**Status**：done（2026-09-23）
