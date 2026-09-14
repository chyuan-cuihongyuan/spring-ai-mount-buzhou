# 1066 — Deno 沙箱探测读面

> 来源：J 会话第 66 轮 = effort #1066（[T1587](../../.wayfinder/tickets/T1587-denoprobe-stats-shape.md) / [T1588](../../.wayfinder/tickets/T1588-denoprobe-stats-verify.md) / impl 818）。借鉴：Envoy health check statistics（健康检查自身的成败分布与缓存行为是可用性对账的第一信号）。guard/sandbox 域第二轴（R56 resilience 后 guard 回访）。

## Problem Statement

`DenoSandbox.available()`（impl-40：deno --version 探测 + TTL 缓存）三分支全静默——TTL 内缓存命中、缓存失效重探测、探测失败置不可用：**探测行为分布不可见**。宿主无法回答「沙箱档为什么一直不可用」（探测持续失败）、「probeTtl 是否误配为 0」（每次调用都在重探测、进程开销放大）；环境缺 deno 与探测抖动无法区分。

## 目标

- `DenoSandbox` 增量（guard/sandbox，静态面）：五 `AtomicLong` 双守恒。
  - `availableCalls`：available() 入口计数；
  - `probeCacheHits`：TTL 内缓存命中短路径；
  - `probes`：重探测执行数；
  - `probeSuccesses` / `probeUnavailables`（探测命令不成功或 launcher 异常）。
- 嵌套 `record DenoProbeStats(...)` + `stats()` + `resetForTest()`。
- 双守恒恒等式：**availableCalls = probeCacheHits + probes**；**probes = probeSuccesses + probeUnavailables**。

## 兼容性

纯增量读面：available()/invalidateProbeCache()/run() 行为与返回逐位不变（探测缓存语义原样）；静态面理由同 R46–R65 先例；无新配置项。

## Out of Scope

- 探测耗时直方图（5s 超时上界既有）。
- per-binary 分桶（binary 名配置面）。
