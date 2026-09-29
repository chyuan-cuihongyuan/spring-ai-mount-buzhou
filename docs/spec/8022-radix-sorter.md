# Spec 8022 — RadixSorter 基数排序（effort #8022，V23）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8045–V8046，impl 2324）。
> 借鉴：LSD 基数排序（Hollerith 打孔卡机思想——稳定分桶逐位）。

## Problem Statement

整数排序的病：比较排序 O(n log n) 下界——**LSD 基数按
字节位稳定分桶 O(n·w) 线性扫（w=字长/8 轮）**。

## Solution

`RadixSorter`（core/metrics，静态工具面）：非负 long 域
LSD 每轮 8 位（256 桶计数排序，稳定）；`sort` 返回新数组
（值语义——原数组不动）；负数 fail-fast（偏置变换不做——
诚实拒绝而非静默错排）；null fail-fast；空/单元素原样
返回副本；确定性纯函数。

## Testing Decisions

- 手锚（乱序 10 元逐值+全等值/已序/逆序退化）；300 随机
  vs Arrays.sort 圣像全等+原数组不动性质+稳定性经值域
  可辨识化钉住（值域小编造 32 倍重复值——桶序稳定可见）；
  负数/null fail-fast。

## Out of Scope

- 不做负数偏置变体；不做 MSD/字符串域。

## Further Notes

- 与 QuickSelect（7033）同族不同面：全序面 vs 秩选择面。
- 里程碑：V23/50（46%）。
