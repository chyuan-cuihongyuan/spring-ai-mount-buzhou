# 1056 — 崩循环探测器类级水位读面

> 来源：J 会话第 56 轮 = effort #1056（[T1567](../../.wayfinder/tickets/T1567-crashloop-stats-shape.md) / [T1568](../../.wayfinder/tickets/T1568-crashloop-stats-verify.md) / impl 808）。借鉴：Kubernetes kube-state-metrics（crashloop_backoff 事件总账——per-object 状态之外还需集群级总量与丢弃量）。J 会话首个 resilience 域轮。

## Problem Statement

`CircuitCrashLoopDetector`（K8s crashloop 思想，模型反复熔断探测）已有 per-model `LoopState`，但**类级水位缺失**：OPEN 记录总量、循环检出总次数、恢复总次数均不可见；更关键的是 `MAX_MODELS=32` 封顶后新模型的 OPEN 事件被静默丢弃——`truncated` 布尔只回答"发生过"不回答"丢了多少"，多租户大规模部署下第 33+ 个模型的熔断信号整体蒸发且无量化对账。

## 目标

- `CircuitCrashLoopDetector` 增量（resilience，静态面）：四 `AtomicLong`。
  - `opensRecorded`：实际写入模型表的 OPEN 数；
  - `opensTruncated`：封顶截断丢弃数（truncated 布尔置位时同步累加——从"是否发生过"升级为"发生过多少"）；
  - `loopsDetected`：循环检出总数（同一模型重复闩锁-恢复周期重复计数）；
  - `recoveriesRecorded`：恢复清除次数。
- 嵌套 `record CrashLoopWatchStats(long opensRecorded, long opensTruncated, long loopsDetected, long recoveriesRecorded)` + `stats()` + `resetForTest()`。
- 口径诚实：null/空白模型调用不落入任何桶（无效调用不入账）。

## 兼容性

纯增量读面：recordOpen/recordRecovery/isLooping/snapshot/truncated 行为与返回逐位不变；无新配置项。

## Out of Scope

- per-model 截断明细（truncated 布尔语义维持——单布尔既有契约不变）。
- 窗口滑动分布统计（LoopState.opensInWindow 已覆盖即时值）。
