---
id: T1452
title: 工具策略匹配决策读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1451
created: 2026-09-14
---

## Question

J 会话第 1 轮：决策读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolPolicyMatchDecisionTest，AssertJ 同仓风格）：exact/glob/none 三分类各断「返回值不变 + 计数 + 最近决策键」；无效条目（值非 Map）不计 EXACT 落 GLOB；守恒 Σ三分类 == total == match 调用数；环形有界（灌 40 条留 32、新→旧序）；resetStats 归零。BeforeEach+AfterEach 双 reset 隔离同 JVM 其他测试的静态污染。定向 `mvn -pl buzhou-core test -Dtest=ToolPolicyMatchDecisionTest` 绿 + 既有 ToolPolicyMatcherTest 回归绿 + API 快照再生。
