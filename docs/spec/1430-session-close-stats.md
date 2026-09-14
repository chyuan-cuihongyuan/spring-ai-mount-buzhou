# 1430 — 会话关闭耗时读数

> 来源：L 会话第 31 轮 = effort #1430（票 T2161 / T2162 / impl 1084）。借鉴：k8s graceful shutdown terminationGracePeriod（关闭耗时的分布是排空健康的第一信号——close 卡在 executor 排空/租约释放会让停机窗口超限）。

## Problem Statement

`DefaultAgentSession.close()`（impl-30 清理优先异常聚合：逐 observer 隔离 onClose、资源注册表逆序关闭、事件分发、listeners/span 清理）全程无计时：停机排空慢（observer onClose 慢/资源注册表逆序关闭卡住）不可见——停机窗口超限只能事后倒推。

## 目标

- `SessionCloseStats`（core/session，进程级静态读面，ToolArgsValidator 先例）：
  - `closed`（成功关闭数）/ `closeFailures`（close 内收集到失败的会话数）/ `lastCloseDurationMillis` / `maxCloseDurationMillis`（水位单调）；
  - `stats()` 嵌套 `record Snapshot` + `recordCloseFailure()` 埋点 + `resetForTest()`；
  - DefaultAgentSession.close() 埋点：nanoTime 计时 + failures 非空时记失败（清理优先异常聚合语义逐位不变——只增记账）。
- close 的清理优先/异常聚合/幂等语义逐位不变。

## 兼容性

纯增量读面：close 清理顺序、失败收集（首失败上抛其余 suppressed）、幂等 CAS 语义不变。

## Out of Scope

- observer 级 onClose 分项计时（逐 observer 隔离已有 try-catch 边界，分项面另轮）。
- 排空超时强制中断（TurnStallWatchdog/DrainCoordinator 域）。
- 开启耗时（spawn 计时——StartupPhaseTiming 族 spec 823 已有阶段计时）。
