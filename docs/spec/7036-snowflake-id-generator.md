# Spec 7036 — SnowflakeIdGenerator 发号器（effort #7036，U37）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7273–U7274，impl 2288）。
> 借鉴：Twitter Snowflake 思想（分布式 ID 事实标准）。

## Problem Statement

分布式发号的病：时钟回拨静默发重号——**分段 64 位+
回拨守卫面**缺失。

## Solution

`SnowflakeIdGenerator`（core/concurrent）：64 位=1 符号+
41 时间毫秒+10 机器+12 序列；同毫秒序列自增、溢出自旋
下毫秒（注入时钟显式推进——零真实睡眠完全确定）；时钟
回退 fail-fast（拒绝发号）；分解读数；机器号越域/空时钟
fail-fast；步进时钟勘误（固定时钟自旋永真——测试钉住
修正为 +1ms 步进注入）。

## Testing Decisions

- 批量唯一+严格递增（步进时钟）；分解往返；回退 fail-fast；
  机器号隔离；fail-fast。

## Out of Scope

- 不做真实 NTP 校时；不做多机房位分配变体。

## Further Notes

- 与 UuidV7Monotonic 同族不同面：时间有序 UUID vs 分段
  整数发号。
- 里程碑：U37/50（74%）。
