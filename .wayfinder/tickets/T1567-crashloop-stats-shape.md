---
id: T1567
title: 崩循环探测器类级水位读面（CrashLoopWatchStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1565
created: 2026-09-15
---

## Question

J 会话第 56 轮：resilience 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换入 resilience 域）：CircuitCrashLoopDetector 已有 per-model LoopState（opensInWindow/looping/loopsDetected），但类级水位缺失——opens 总量、**MAX_MODELS=32 封顶后的静默截断**（truncated 布尔只见 true/false 不见量）、循环检出总数、恢复次数。截断静默蒸发是题材核心：第 33 个模型起 OPEN 事件全部丢弃且无量化信号。

形状裁决：`CircuitCrashLoopDetector` 内静态 `AtomicLong` 四计数——opensRecorded（实际入表）/ opensTruncated（封顶截断丢弃）/ loopsDetected（检出循环总数，含重复闩锁-恢复周期）/ recoveriesRecorded（恢复清除）；嵌套 `record CrashLoopWatchStats` + `stats()` + `resetForTest()`。口径诚实：opensRecorded + opensTruncated = recordOpen 有效调用数（null/空白不计入任何桶——调用即无效）。recordOpen/recordRecovery 行为逐位不变。

Out of scope：per-model 截断明细（truncated 布尔语义维持）；窗口滑动分布（LoopState 已有 opensInWindow）。
