# Spec 7034 — ZobristHashing 表驱动异或哈希（effort #7034，U35）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7269–U7270，impl 2286）。
> 借鉴：Zobrist 1970 思想（棋类引擎/图同构测试同源）。

## Problem Statement

增量状态哈希的病：全状态重算 O(位置)（每步棋）——
**(位置,种类) 随机长码表+异或增量 O(1) 面**缺失。

## Solution

`ZobristHashing`（core/metrics）：种子化随机长码表；状态
哈希=命中码异或和（空位 −1 贡献 0——异或自逆）；xorIn/
xorOut 增量 O(1) 顺序无关；codeAt 审计面；越域 fail-fast；
碰撞概率诚实边界（64 位——非加密承诺）。

## Testing Decisions

- 整算 vs 逐表项增量全等；xorOut 自逆；空态 0；不同
  状态不同哈希（种子钉住）；越域 fail-fast。

## Out of Scope

- 不做碰撞概率证明；不做表重生成。

## Further Notes

- 与 XxHash64（7030）同族不同面：整块字节流 vs 增量可
  异或表驱动。
- 里程碑：U35/50（70%）。
