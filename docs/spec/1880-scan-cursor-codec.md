# Spec 1880 — SCAN 游标编解码（effort #1880，R81）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2961–T2962，impl 1481）。借鉴：
> Redis（130K+ 星）SCAN 的反向递增游标——游标在「位反转空间」里做
> +1：表扩缩容时桶序自动对齐 rehash 迁移序，同尺寸全周游不重不漏
> 后精确归零。纯计算面让「全周游保证」可测可证。

## Problem Statement

键空间增量扫描随手用 offset 分页：扫期间有写入就会跳键或重键
（offset 语义在活集合上不守恒）；Redis SCAN 的游标不是偏移量而是
位反转空间的计数器——这个保证的数学面没有独立可测的口径。

## Solution

`ScanCursorCodec`（core/cache，静态纯函数）：

- `nextCursor(cursor, tableBits)`：v′ = rev_bits(rev_bits(cursor)+1)
  ——位反转空间递增一步；游标绕满一圈精确回 0（0 = 迭代完成）；
- `isComplete(cursor)`：cursor == 0 判完成；
- `cycleLength(tableBits)`：2^bits——同表尺寸全周游桶数。

## User Stories

1. 作为扫描实现者，bits=2 全周游序 0→2→1→3→0——rehash 友好序
   且不重不漏可证。
2. 作为正确性评审者，反转空间严格递增——同尺寸下无桶被访问两次
   有数学保证。
3. 作为容量观察者，cycleLength(20)≈1M——大表全周游成本事前有账。

## Implementation Decisions

- 位宽 tableBits ∈ [1,63]（Redis 最小表 4 桶）；Long.reverse 全宽
  反转后移位截位；cursor 非负（fail-fast）。

## Testing Decisions

- bits=2 全周游序恰 {2,1,3} 后归 0；bits=3 周游长 8 且不重；反转
  空间严格递增性质断言；cycleLength 幂断言；畸形（负游标/位宽越界）
  fail-fast。

## Out of Scope

- 不做实际键扫描与 rehash 期间跨尺寸保证模拟（归存储层）；不解析
  MATCH/count 参数。

## Further Notes

- 与 RedisSummaryStore 的 Lettuce ScanCursor（客户端黑盒）互补：
  这是游标语义的白盒计算面。
