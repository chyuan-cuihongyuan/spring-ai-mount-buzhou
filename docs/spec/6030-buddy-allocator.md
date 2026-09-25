# Spec 6030 — Buddy Allocator 伙伴分配器（effort #6030，T31）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6261–T6262，impl 2231）。
> 借鉴：Linux 内存管理 buddy 思想。

## Problem Statement

变长块分配的病：外部碎片化（空闲总量够但连续块不足）——
**分裂/伙伴合并面**缺失。

## Solution

`BuddyAllocator`（core/memory，单位模型）：

- 申请 order k 无块时把更大块对半裂开（低半自用高半入闲
  链）；释放与伙伴（offset XOR 2^k）逐级合并；
- freeList 用 TreeSet（同阶取最小偏移——确定性）；
- fail-fast：阶越域、双重释放、池满。

## Testing Decisions

- 16 块全量分配+全量释放回归单 order4 块（合并完备性）；
- 分裂取低半确定性；交错分配释放读数钉住；池满/双重释放
  fail-fast。

## Out of Scope

- 不做字节语义（单位模型）；不做多池 NUMA。

## Further Notes

- 与 SlabClassPacker（cache）同族不同面：分裂合并 vs 尺寸
  分类装箱。
- 里程碑：T31/50（62%）。
