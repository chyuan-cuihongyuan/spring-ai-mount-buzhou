# Spec 5024 — MemTable 内存表（effort #5024，S25）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6149–T6150，impl 2175）。
> 借鉴：LSM MemTable（可变内存表 + 满表滚动冻结）。

## Problem Statement

写路径内存缓冲的病：无限累积（内存爆炸）或满即阻塞（无
滚动交接语义）——**有序可变表 + 满表滚动面**缺失。

## Solution

`MemTable`（core/metrics）：

- `put(key, value)`：upsert 语义（同键覆盖）；表满返回 false
 （滚动交接责任在调用方——flush/drain 后可再写），写入成功
  true；
- `get(key)` / `size()` / `isFull()` 读数；
- `drain()`：整表按 key 字典序导出并清空（滚动到不可变段
  的交接点——确定性）；
- fail-fast：maxEntries≤0、null key/value。

## User Stories

1. 作为写路径作者，内存表有序满即滚动——交接确定性。
2. 作为审计作者，同写入序列同导出（确定性可回放）。

## Testing Decisions

- upsert 覆盖；满拒写不覆盖已有；drain 字典序导出并清空；
  isFull 读数；负容量/null fail-fast；确定性回放。

## Out of Scope

- 不做 WAL 联动（GroupCommitLog 已覆盖持久面）；不做并发
  表（单线程口径）；不做压缩。

## Further Notes

- 与 SparseIndex（S22 不可变段索引）同族不同面：可变写侧
  vs 不可变读侧。Wave 5 开波。
- 里程碑：S25/50（50%）。
