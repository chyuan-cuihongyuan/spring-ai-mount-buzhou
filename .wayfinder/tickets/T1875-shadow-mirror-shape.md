---
id: T1875
title: R31 选题——ResilienceAdvisor 影子镜像旁路 e2e 补测（shadowMirrorIfSampled 21 missed 深水区）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 31 轮：全仓分支扫描最大单方法缺口 shadowMirrorIfSampled（21 missed，891 行深水区）如何定向补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 31 轮 = effort #1230 / spec 1230 / impl 933）：

1. **补测面（4 用例 e2e）**：主路成功后镜像到首个备模型（旁路调用发生）；镜像失败全吞不影响主路；fallback 链空 → 候选空守卫直返不镜像；shadowProbePercent=0 → 探针不构建零旁路。
2. **形态**：ShadowMirrorEndToEndTest——FallbackChainEndToEndTest 多模型 runtime harness 复用（ScriptedChatModel + ResilienceModule.configure + NamedFallbackModel 列表）。
3. **边界**：不改主代码；canary/degrade 路径（adviseCallCanary 6/degradeFromCanary 8/recordCandidateUsage 13）留批次 6。
