# 838 — 选举竞争读数

> 来源：H 会话第 38 轮 = effort #838 / [T1175](../../.wayfinder/tickets/T1175-leader-election-stats.md) / [T1176](../../.wayfinder/tickets/T1176-leader-election-stats-verify.md) / impl 590。
> 借鉴：Redisson RedLock 竞争统计（≈36K star）。

## Problem

Redis 选主（331）只有结果查询（inspect）：「选主抖不抖、谁是常任主、失位率多高」无计数面——选主不稳时只能翻连接日志。

## Solution

`LeaderElectionStats`（store-redis，纯记账）：

- **四态计数**：ACQUIRED（获选）/ RENEWED（续期保位）/ OTHER_HOLDER（他主在位让位）/ LOST（失位）。
- **竞争烈度**：contentionRatio = (让位+失位)/总尝试——0 稳态、趋 1 激烈抖动。
- **喂点**：选举器包装/装配侧在 tryAcquireOrRenew 返回后归类喂入——选举行为零变更。

## 兼容性

纯新增；RedisLeaderElector 零变更。

## 诚实边界

四态归类归调用方；不区分 scope（分桶自建）；纯计数无时序。
