# Spec 5025 — Skip List 跳跃表（effort #5025，S26）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6151–T6152，impl 2176）。
> 借鉴：跳表（Pugh 1990——概率多层链，Redis zset 底层思想）。

## Problem Statement

有序动态集合的病：有序数组插入 O(n)、平衡树实现复杂
（旋转与父指针）——**期望 O(log n) 的简单有序面**缺失。

## Solution

`SkipList`（core/metrics）：

- 多层前向指针塔：每键层高由种子化随机发生器逐半衰（`p=1/2`
  封顶 MAX_LEVEL）——期望 O(log n) 查找/插入；
- `put` upsert、`get`、`keysInOrder`（第 0 层顺序——确定性）；
- 种子注入：同种子同层高同结构（确定性可回放）；
- fail-fast：null 键值。

## User Stories

1. 作为有序集合作者，插入/查找期望对数且实现直白。
2. 作为审计作者，同种子同结构（确定性可回放）。

## Testing Decisions

- 固定种子 500 随机操作 vs TreeMap 圣像（键序与取值全等）；
  同种子重放同键序；upsert 覆盖；null 键值 fail-fast。

## Out of Scope

- 不做并发跳表（无锁层删）；不做范围删除；不做缓存行优化。

## Further Notes

- 与 SkipList 同族不同面：TreeMap 红黑树 vs 概率塔。Wave 5
  第二件。
- 里程碑：S26/50（52%）。
