---
id: T1006
title: 秘密熵过滤 Builder 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1005
created: 2026-09-13
---

## Question

Builder 声明熵阈值 → beforeTurn 真缝示例键被滤？缺省照常 redact？装配摘要可见？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 28 轮）：① 声明熵阈值 → beforeTurn 输入原样（低熵示例未被替换）；② 缺省 → 输入含 [SECRET:AWS_ACCESS_KEY]（既有语义）；③ assemblySummary 含 SecretScanHook；④ 全模块回归绿。
