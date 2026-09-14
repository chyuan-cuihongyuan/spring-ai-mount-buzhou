---
id: T1815
title: ToolTimingAggregatorConcurrencyTest 负载下非确定性卡死（yield 风暴 + 无超时护栏）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

R4 验证轮实证：`ToolTimingAggregatorConcurrencyTest` 在同 commit 下两次隔离 worktree 全量跑行为非确定——第一次 ~8 分钟全绿（2634 用例 0 失败），第二次 forked JVM 卡死 109+ CPU 分钟（surefire 01:16 后零测试完成）。根因与最小修复是什么？

## Resolution

**用户常设授权 AFK（可推翻）**

处置（K 会话第 4 轮验证显形，独立票独立 commit）：

1. **实证证据**：jstack（PID 1044，forked JDK 23.0.2）main 线程 RUNNABLE、1651 CPU 秒，栈顶 `ToolTimingAggregator$Timing.record`（:83）——内联可归因至 `RollingMaxCounter.record` 的 CAS do-while；surefire-reports 01:16 后无增量。
2. **定性**：测试自身的脆弱面——`Thread.yield()` 风暴（并行流 lambda 内 yield 恶化 ForkJoin 调度：worker 让出后任务队列照常派发，负载下交错爆炸）叠加**无超时护栏**，病态调度下一次「单步常数开销」被放大成分钟级挂起；CAS 循环本身有界（maxNanos 单调不降必 break），主代码活锁未被证实，但「测试可挂起全量套件」这一脆弱性本身须收口。
3. **最小修复（测试侧，语义不变）**：
   - 移除 `Thread.yield()` 随机分支——交错概率已由并行流 + 系统负载天然保证，不变量断言（count/total/max 守恒）不依赖 yield；
   - `@Timeout(120)` 护栏（JUnit Jupiter 同框架 API，非新引第三方）——病态场景 2 分钟内失败显形，不再挂死全量套件。
4. **护栏纪律**：并发压测类（*ConcurrencyTest）默认应带 @Timeout——先例由本票确立，后续补测轮沿用；不改主代码，主代码 liveness 若未来复现挂起再单列票实证。
