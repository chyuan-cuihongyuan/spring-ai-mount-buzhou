---
id: T1172
title: 尾采样决策台账验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1171]
created: 2026-09-13
---

## Question

聚合/保留率/挤老/溢出如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 36 轮 = effort #835）：TailSamplingDecisionLogTest 4 例——聚合+0.5 保留率+原因降序/环挤老 6/原因封顶溢出带决策维/脏入参+空真。
