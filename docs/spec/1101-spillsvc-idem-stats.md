# 1101 — SpillService 幂等复用读面

> 来源：J 会话第 101 轮 = effort #1101（[T1661](../../.wayfinder/tickets/T1661-spillsvc-idem-shape.md) / [T1662](../../.wayfinder/tickets/T1662-spillsvc-idem-verify.md) / impl 853）。借鉴：幂等重试的复用率对账（同 callId 重放命中既有落盘=管线自愈效率）。spill 服务层首轴（R77 为 hook 层）。

## Problem Statement

`SpillService.tryOffload`（溢出落盘服务）五分支零计数——阈值内直返、新落盘、**幂等复用**（HotTail 视图重试同 callId 时占位复用）、降级透传：**服务层分支分布不可见**。幂等复用率高=视图重试频繁（上游效率信号）；降级高发=存储故障。

## 目标

- `SpillService` 增量（spill，静态面）：五 `AtomicLong`。
  - `tryOffloadCalls`（入口）/ `freshStores`（新落盘）/ `idempotentReuses`（同内容占位复用）/ `degraded`（store 异常降级透传）/ `belowThreshold`（阈值内直返）。
- 嵌套 `record SpillServiceStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**tryOffloadCalls = freshStores + idempotentReuses + degraded + belowThreshold**（每入口恰落一桶）。

## 兼容性

纯增量读面：tryOffload 返回语义、幂等复用与降级透传逐位不变；静态面理由同 R46–R100 先例；无新配置项。

## Out of Scope

- 按 toolName/sessionId 分桶（敏感面——红线纪律）。
- preview 截断率（展示面）。
