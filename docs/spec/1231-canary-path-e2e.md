# 1231 — R32：金丝雀路径端到端直测（adviseCallCanary × degradeFromCanary × canary-selected）

> 来源：K 会话第 32 轮 = effort #1231（[T1877](../../.wayfinder/tickets/T1877-canary-shape.md) / [T1878](../../.wayfinder/tickets/T1878-canary-verify.md) / impl 934）。方法论：金丝雀发布（canary release）语义的 e2e 合同——「新模型先接 1% 流量、失败自动回主模型」的发布安全网。

## Problem Statement

金丝雀路径（canaryEnabled + 权重确定性路由 + 金丝雀终态失败链序回退 + canary-selected 事件）从未被直接断言——「新模型先接小流量、失败自动回主模型」的发布安全网回归不可见。

## 目标

- CanaryPathEndToEndTest（3 用例，多模型 runtime harness 复用）：canary 路由成功（权重全给 secondary → 回复来自备模型）；金丝雀终态失败链序回退主模型（switched 事件 from=secondary to=primary）；canary-selected 事件 payload 钉 model+sessionId。

## 实现决策

- Fallback record：canaryEnabled=true + weights 全给 secondary → FallbackChain.selectInitialTarget 确定性选中（会话哈希）。
- 事件断言经 session.addEventListener 录制（既有 listen 先例）。

## 测试决策

- 断言只对外部行为：回复来源、事件类型与 payload、模型调用次数。
- 验收门：定向绿 + resilience 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- candidateLimiter 限流拒绝分支（需限流器装配，留后续）。
- adviseStream 金丝雀路径（当前实现金丝雀仅 call 路径——如实记录）。

## Further Notes

- 金丝雀语义三件套（路由/回退/事件）首次成组断言——发布安全网的 e2e 合同。
