---
id: T1501
title: 内嵌策略引擎判定分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 26 轮：内嵌策略引擎判定分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 26 轮 = effort #1025 / spec 1025 / impl 778）：缺口成立——EmbeddedPolicyEngine（guard-24 OPA 子集：声明式规则→allow/deny/escalate，默认拒）decide 全程零计数：allow/deny/escalate 各多少、escalate 经人工审批转 allow 多少——策略调优（规则覆盖是否合理、审批通道压力）无据。落点 buzhou-guard policy 包：EmbeddedPolicyEngine 实例级四桶计数 + 嵌套 record `PolicyDecisionStats(allowCount, denyCount, escalateCount, escalateApprovedCount)` + `stats()`——守恒四桶和 == decide 调用数（含默认拒与输入缺失拒绝路径）。实例级；嵌套类型不动 API 快照；判定返回值逐位不变。
