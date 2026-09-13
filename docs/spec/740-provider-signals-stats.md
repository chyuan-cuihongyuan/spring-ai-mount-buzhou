# 740 — 供应商限流信号 stats 接线

> 来源：G 会话第 41 轮 = effort #740（719 信号的聚合接线）/ [T1082](../../.wayfinder/tickets/T1082-provider-signals-stats.md) / [T1083](../../.wayfinder/tickets/T1083-provider-signals-stats-verify.md) / impl 640。

## Problem

719 解析出的供应商限流利用率没有聚合归宿——消费端各自为政，健康面板（ResilienceStats details）看不到「供应商余量有多紧」。

## Solution

ResilienceStats 加 `updateProviderUtilization(double)` / `lastProviderUtilization()`（NaN 起始=尚无信号）；details 在有信号后条件出现 `providerUtilization`（不污染默认读数）。消费链：advisor 拦响应头 → parseFlexible → pressureLevel 决策 → stats 回写。

## Out of Scope
时间序列/历史平滑；advisor 自动拦截（719 诚实边界沿用）。
