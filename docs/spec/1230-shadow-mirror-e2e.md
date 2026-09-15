# 1230 — R31：ResilienceAdvisor 影子镜像旁路 e2e 补测

> 来源：K 会话第 31 轮 = effort #1230（[T1875](../../.wayfinder/tickets/T1875-shadow-mirror-shape.md) / [T1876](../../.wayfinder/tickets/T1876-shadow-mirror-verify.md) / impl 933）。方法论：批次化延续——全仓分支扫描最大单方法缺口的深水区定向（Istio mirror 思想的容量预案信心面）。

## Problem Statement

shadowMirrorIfSampled（spec 1623/T2397，主路成功后采样对照首个备模型）21 missed 为全仓最大单方法缺口：镜像调用发生、镜像失败全吞、候选空守卫、零采样守卫——容量预案信心面的四条分支从未被直接断言。

## 目标

- ShadowMirrorEndToEndTest（4 用例，e2e harness 复用）：主路成功后镜像到首个备模型（旁路调用发生、主回复不受影响）；镜像失败全吞（shadow 模型异常不影响主回复）；fallback 候选空守卫（模块级 NamedFallbackModel 列表空 → 不镜像）；shadowProbePercent=0 → 探针不构建零旁路。

## 实现决策

- 形态 = ShadowMirrorEndToEndTest：FallbackChainEndToEndTest 多模型 runtime harness 复用（ScriptedChatModel 双模型 + ResilienceModule.configure + NamedFallbackModel 列表）。
- 语义实证：镜像候选由模块注入的 NamedFallbackModel 列表驱动（props.fallback.models 空列表经规范化为 null 不拦截镜像——镜像由注入列表决定）；shadowProbePercent=0 → 探针不构建（装配守卫）。

## 测试决策

- 断言只对外部行为：用户回复恒主路产出、旁路调用次数（seenPrompts）、镜像失败不影响主路。
- 验收门：定向绿 + resilience 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- canary/degrade 路径（adviseCallCanary 6/degradeFromCanary 8/recordCandidateUsage 13）批次 6。

## Further Notes

- 影子镜像 = 容量预案信心面：「备模型若被启用结果是否一致」——Istio mirror 思想。
