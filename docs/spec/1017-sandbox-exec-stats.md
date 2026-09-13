# 1017 — 沙箱执行结果分桶读面

> 来源：J 会话第 18 轮 = effort #1017（[T1485](../../.wayfinder/tickets/T1485-sandbox-exec-stats-shape.md) / [T1486](../../.wayfinder/tickets/T1486-sandbox-exec-stats-verify.md) / impl 770）。借鉴：Firejail / bubblewrap 运行统计（沙箱击杀与限额触发是一等运维信号）。

## Problem Statement

LimitedCommandSandbox（impl-40）把超时击杀（KilledReason.TIMEOUT）与输出超限截断（KilledReason.OUTPUT）作为**单次结果字段**透出，但无累计读面：「沙箱里命令频繁被击杀/截断」——脚本质量问题或限额配置失当——只能逐次翻结果，无水位可告警。

## 目标

- `LimitedCommandSandbox` 增量（buzhou-guard，实例级）：`executions` / `timeouts` / `outputTruncations` 三 AtomicLong。
  - executions：run 完成即计（无论结果）；
  - timeouts：delegate 报 timedOut 时计（TIMEOUT 归因点）；
  - outputTruncations：输出超限截断时计（OUTPUT 归因点）；
  - 同一次执行可同时计 timeouts + outputTruncations（两轴正交——executions ≥ max(timeouts, outputTruncations)，无加法守恒）。
- 嵌套 record `ExecStats(long executions, long timeouts, long outputTruncations)` + `stats()` 快照。

## 兼容性

纯增量读面：CommandResult 字段与归因语义逐位不变（delegate 已给 reason 时不覆盖）；无新配置项。

## Out of Scope

- 底层 DenoSandbox 直连（未装饰）场景的统计（装配层选择；装饰器统计即装饰器口径）。
- exitCode 非零分桶（与熔断/错误签名面重叠，回避）。
