# Spec 131 — 工具级熔断（effort #94）

> wayfinder map：`.wayfinder94/MAP.md`（T477–T478）。主题池轮换（原 fog ⑥⑦ 被
> A 会话收口，改选）。借鉴：resilience4j CircuitBreaker（失败率滑窗 + 半开探测），
> 与 spec 15 模型熔断（模型面）正交——本轮补工具面。

## Problem Statement

单个外部工具（如某下游 HTTP 服务）持续 5xx 时，模型每轮仍反复调它：每次都等
超时/收错误反馈再 REASK——Turn 预算被坏工具吃光，还会把失败信号放大成整轮失败。
模型面有熔断降级链（spec 15），工具面没有等价物。

## Solution

`ToolCircuitBreaker`（core/concurrent）+ `ToolCircuitBreakerHook`（挂进
RuntimeConfig 即启用）：

- **状态机（per toolName）**：CLOSED（计数滑窗）→ 失败率 ≥ 阈值 → OPEN（冷却
  期内 beforeTool 直接拒）→ 冷却耗尽 → HALF_OPEN（放 N 次探测）→ 全成 →
  CLOSED（窗清零）/ 任一败 → OPEN 重新冷却。
- **结局识别**：afterTool 结果走 `ToolFeedbackType.isErrorFeedback` 结构化标记
  （[工具执行失败] / [工具参数校验失败] 均计失败——校验失败也是坏信号）。
- **拒绝语义**：OPEN/HALF_OPEN 超额 → `HookResult.block("工具熔断中…")`——
  模型收到可读理由，不炸 Turn；计数 `buzhou.tool-breaker.blocked`。
- **配置**：windowSize（默认 20）/ failureRateThreshold（默认 50%）/ cooldown
  （默认 60s）/ halfOpenTrials（默认 3）——时钟可注入（测试）。
- 观测：`stateOf(tool)` / `snapshot()`（state/失败率/拒绝计数）；未注册工具的
  状态惰性创建。

## User Stories

1. 作为宿主，下游工具挂了我希望它被自动「摘牌」冷却，Turn 不再被它拖死；
   半开探测恢复后自动「复牌」——无需人工干预。
2. 作为模型，被熔断的工具调用收到明确理由（「工具熔断中，Xs 后重试」），
   可以改道别的工具或告知用户——而不是反复撞墙。
3. 作为运维，我能查每个工具的熔断状态与失败率（snapshot）定位坏依赖。

## Implementation Decisions

- hook 落地（beforeTool order 240——先于 HITL 300；afterTool 记结局）而非
  HarnessToolCallingManager 内嵌：护栏可拒面本就是 hook 领域，且零共享文件冲突。
- 滑窗为计数环形（成功/失败各计数，满窗算比率再滑动）——不存调用明细。
- HALF_OPEN 探测超额的调用按拒绝计（不放行——与 resilience4j 语义一致）。

## Testing Decisions

- 状态机单测：窗内失败率跳闸 / 成功为主不跳 / 冷却后半开探测全成恢复 /
  半开一败即重开 / 时钟注入确定性。
- hook 集成：DefaultToolCallContext + markExecuted（错误反馈文案 vs 正常结果）
  → 记结局；OPEN 时 beforeTool block 文案与计数。
- 先例：AgentBulkheadTest（global-knob 状态机）+ PII hook 测试（ctx 驱动）。

## Out of Scope

- 慢调用比例熔断；熔断事件 webhook 外发；跨实例共享工具熔断状态。

## Further Notes

- 三层韧性拼图：模型熔断（15）+ agent 舱（84）+ 工具熔断（本轮）。
