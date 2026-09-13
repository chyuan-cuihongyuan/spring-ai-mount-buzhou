---
id: T1502
title: 内嵌策略引擎判定分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1501
created: 2026-09-14
---

## Question

J 会话第 26 轮：判定分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EmbeddedPolicyEngineStatsTest，PolicyDecision.Rule/Input 公共 record 直构）：allow 规则命中 allowCount=1；无规则默认拒 denyCount=1；escalate 规则未审批 escalateCount=1；escalate+humanApproved → escalateApprovedCount=1 且决策 action=ALLOW；label 谓词不匹配跳过该规则落入下一条/默认拒；四桶守恒和 == decide 调用数。定向 `mvn -pl buzhou-guard test -Dtest='EmbeddedPolicyEngineStatsTest'` 绿 + 既有引擎回归绿。
