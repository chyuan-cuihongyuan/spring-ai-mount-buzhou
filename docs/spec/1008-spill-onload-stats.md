# 1008 — spill 回读命中率读面

> 来源：J 会话第 9 轮 = effort #1008（[T1467](../../.wayfinder/tickets/T1467-spill-onload-stats-shape.md) / [T1468](../../.wayfinder/tickets/T1468-spill-onload-stats-verify.md) / impl 761）。借鉴：PostgreSQL [buffer hit ratio](https://www.postgresql.org/docs/current/pgstatstatements.html)（缓存命中/未命中比是存储健康的第一指标）。

## Problem Statement

spill 回读路径 `OnloadHook.beforeTool`（写侧工具的长内容参数从盘回灌上下文）零计数：回读成功（内容完好在盘）与回读失败（文件被逐/侵蚀/路径失效）不可分——spill 侵蚀率（命中率恶化）不可见，容量与 TTL 配置失去依据。

## 目标

- `OnloadHook` 增量（buzhou-spill）：`attempts` / `loaded` / `failed` 三 AtomicLong——每个非空 path 参数的回读计一次尝试；守恒不变量 **attempts == loaded + failed**；空参数不计尝试（非回读语义）。
- 新公共 record `SpillOnloadStats(attempts, loaded, failed)`（spill 包，api 面）+ `stats()` 快照。
- 命中率 = loaded/attempts 由消费方自算（诚实不设派生字段）。

## 兼容性

纯增量读面：回读/阻断/事件语义零变化；无新配置项。

## Out of Scope

- DiskSpillStore 写侧计数（写放大读数 = H 池 R16 已占领域，回避）。
- 按 path/工具分桶（基数纪律）。
