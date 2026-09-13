---
id: T1508
title: 模型窗口解析分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1507
created: 2026-09-14
---

## Question

J 会话第 29 轮：解析分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TableContextWindowResolverStatsTest，AssertJ 同仓风格）：override 命中计 overrideHits 且返回覆盖值；内置表前缀命中（大小写不敏感）计 builtInHits；未知模型回退 32768 计 fallbackHits（重复查询重复计——每模型告警一次是既有 WARN 语义不在本面）；null 模型走回退；resolvedWindows 快照含已解析模型且不可变；三路守恒和 == 解析总数。定向 `mvn -pl buzhou-core test -Dtest='TableContextWindowResolverStatsTest'` 绿 + 既有窗口解析回归绿。
