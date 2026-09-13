---
id: T1485
title: 沙箱执行结果分桶读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 18 轮：沙箱执行结果分桶读面（Firejail/bubblewrap run stats 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 18 轮 = effort #1017 / spec 1017 / impl 770）：缺口成立——LimitedCommandSandbox（impl-40 限额装饰器）run 全程零计数：超时击杀（TIMEOUT）与输出超限截断（OUTPUT）作为 CommandResult 字段透出但**无累计读面**——「沙箱里命令频繁被击杀/截断」是脚本质量与限额配置合理性的第一信号。落点 buzhou-guard：LimitedCommandSandbox 实例级 executions/timeouts/outputTruncations 三 AtomicLong（run 完成即计 executions；TIMEOUT/OUTPUT 归因点各计）+ 嵌套 record `ExecStats(executions, timeouts, outputTruncations)` + `stats()`。实例态（装饰器即装配粒度）；行为逐位不变（仅加计数）。
