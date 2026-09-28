# Spec 7021 — WriteAheadLog 写前日志（effort #7021，U22）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7243–U7244，impl 2273）。
> 借鉴：PostgreSQL WAL / Redis AOF 思想。

## Problem Statement

崩溃一致性的病：重放静默吐坏数据（无校验）——**写侧
校验和存储+重放重算比对面**缺失。

## Solution

`WriteAheadLog`（core/fs）：写侧每记录存 CRC32，重放
全量重算比对（损坏 fail-fast 携带 LSN）；分段滚动；
LSN 单调；段负载镜像暴露（审计面——损坏注入验证校验
真实生效）；进程内镜像段（磁盘面明示不做）。

## Testing Decisions

- roundtrip；分段滚动 2/2/1；镜像注入损坏→fail-fast
  携带 LSN 1；clean 日志不受扰；fail-fast。

## Out of Scope

- 不做真实文件 IO；不做检查点截断。

## Further Notes

- 与 SegmentLog（5028）同族不同面：滚动淘汰 vs 校验重放。
- 里程碑：U22/50（44%）。
