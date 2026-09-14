# 1624 · 危险工具 HITL 豁免征询（spec 820 孤类首个消费者）

> 来源：N 会话 R25（effort #1624 / T2399–T2400 / impl 1177）。spec 1611 普查修复
> 第十弹：GuardExemptionRegistry（spec 820，ESLint suppressions 带过期借鉴）建成
> 即孤——javadoc 自述「各护栏 hook 是否征询豁免由其自身接线」而无任何 hook 接线。

## Problem Statement

危险工具的 HITL 确认对高频已核验工具造成确认疲劳：同一安全工具每次调用都要
人工点确认。「这条告警我看过、豁免到 T1」需要从口头变登记（显式、有时限、
可撤销、可审计）。

## Solution

- `DangerousToolGuardHook` 构造器 +exemptions（null = 不征询零行为；GuardModule
  装配恒建 registry 并传入）。
- 征询点：授权标记检查之后、确认请求之前——豁免命中放行 +
  `guard.exemption.applied` 审计事件（toolName/mechanism）；过期（惰性失效）/
  撤销 / 无豁免恢复标准确认流程（block 语义零变化）。
- `GuardModule.exemptions()`：宿主 grant/revoke/snapshot 面（registry 随模块
  实例，条目封顶 64 有界纪律）。

## Testing Decisions

- `DangerousToolExemptionTest` 四断言：有效豁免（60s）放行；无豁免与过期豁免
  仍 Block；撤销恢复 Block；模块暴露 registry 且初始快照空。
- 回归：guard 全量 343 用例（含既有 DangerousToolGuardHook HITL 流程）。

## Out of Scope

- 豁免的 yml/事件驱动授予面（当前编程面 grant——业务前端桥接后续轮）。
- 其余护栏（PII/moderation/secret）的征询接线（每 hook 一个消费面——按需逐轮）。
